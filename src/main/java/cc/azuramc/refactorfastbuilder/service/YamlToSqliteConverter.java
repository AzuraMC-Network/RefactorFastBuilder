package cc.azuramc.refactorfastbuilder.service;

import cc.azuramc.refactorfastbuilder.config.PluginConfig;
import cc.azuramc.refactorfastbuilder.database.DatabaseManager;
import cc.azuramc.refactorfastbuilder.database.SQLitePlayerDataImpl;
import cf.pies.fastbuilder.api.ResetAnimation;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * YAML到SQLite数据迁移工具
 * 能够将FastBuilder插件的YAML格式playerdata文件转换为SQLite数据库
 * 
 * @author an5w1r@163.com
 */
public class YamlToSqliteConverter implements MigrationService {
    
    private final DatabaseManager databaseManager;
    private final Plugin plugin;
    private final Logger logger;
    
    public YamlToSqliteConverter(DatabaseManager databaseManager, Plugin plugin) {
        this.databaseManager = databaseManager;
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 迁移单个玩家的数据
     */
    private void migratePlayerData(Connection connection, UUID uuid, ConfigurationSection playerSection) 
            throws SQLException {
        
        // 迁移个人最佳成绩和统计数据
        migratePersonalBests(connection, uuid, playerSection);
        
        // 迁移金币
        migrateCoins(connection, uuid, playerSection);
        
        // 迁移选中的工具和方块
        migrateSelectedItems(connection, uuid, playerSection);
        
        // 迁移拥有的方块
        migrateOwnedBlocks(connection, uuid, playerSection);
        
        // 迁移重置动画
        migrateResetAnimations(connection, uuid, playerSection);
        
        // 迁移平均时间数据
        migrateAverageTimes(connection, uuid, playerSection);
        
        // 迁移尝试次数
        migrateAttempts(connection, uuid, playerSection);
    }
    
    /**
     * 迁移个人最佳成绩
     */
    private void migratePersonalBests(Connection connection, UUID uuid, ConfigurationSection playerSection) 
            throws SQLException {
        
        ConfigurationSection pbSection = playerSection.getConfigurationSection("pb");
        if (pbSection == null) return;
        
        String sql = "INSERT OR REPLACE INTO player_stats (uuid, category, personal_best, attempts_completed, attempts_failed, average_time, average_time_amount) VALUES (?, ?, ?, 0, 0, 0, 0)";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String category : pbSection.getKeys(false)) {
                double personalBest = pbSection.getDouble(category, 0.0);
                
                statement.setString(1, uuid.toString());
                statement.setString(2, category);
                statement.setDouble(3, personalBest);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
    
    /**
     * 迁移金币数据
     */
    private void migrateCoins(Connection connection, UUID uuid, ConfigurationSection playerSection) 
            throws SQLException {
        
        if (!playerSection.contains("coins")) return;
        
        double coins = playerSection.getDouble("coins", 0.0);
        
        String sql = "INSERT OR REPLACE INTO player_data (uuid, key, value) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, "coins");
            statement.setString(3, String.valueOf(coins));
            statement.executeUpdate();
        }
    }
    
    /**
     * 迁移选中的工具和方块
     */
    private void migrateSelectedItems(Connection connection, UUID uuid, ConfigurationSection playerSection) 
            throws SQLException {
        
        String sql = "INSERT OR REPLACE INTO player_data (uuid, key, value) VALUES (?, ?, ?)";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // 选中的镐子
            if (playerSection.contains("selected_pick")) {
                String selectedPick = playerSection.getString("selected_pick");
                statement.setString(1, uuid.toString());
                statement.setString(2, "selected_pick");
                statement.setString(3, selectedPick);
                statement.addBatch();
            }
            
            // 选中的方块
            if (playerSection.contains("selected_block")) {
                String selectedBlock = playerSection.getString("selected_block");
                statement.setString(1, uuid.toString());
                statement.setString(2, "selected_block");
                statement.setString(3, selectedBlock);
                statement.addBatch();
            }
            
            statement.executeBatch();
        }
    }
    
    /**
     * 迁移拥有的方块
     */
    private void migrateOwnedBlocks(Connection connection, UUID uuid, ConfigurationSection playerSection) 
            throws SQLException {
        
        ConfigurationSection blocksSection = playerSection.getConfigurationSection("blocks");
        if (blocksSection == null) return;
        
        String sql = "INSERT OR REPLACE INTO player_blocks (uuid, material, data_value, owned) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String blockKey : blocksSection.getKeys(false)) {
                if (blocksSection.getBoolean(blockKey, false)) {
                    String[] parts = blockKey.split(":");
                    if (parts.length == 2) {
                        try {
                            String material = parts[0];
                            int dataValue = Integer.parseInt(parts[1]);
                            
                            statement.setString(1, uuid.toString());
                            statement.setString(2, material);
                            statement.setInt(3, dataValue);
                            statement.setBoolean(4, true);
                            statement.addBatch();
                        } catch (IllegalArgumentException e) {
                            logger.warning("无效的方块格式: " + blockKey + " (玩家: " + uuid + ")");
                        }
                    }
                }
            }
            statement.executeBatch();
        }
    }
    
    /**
     * 迁移重置动画数据
     */
    private void migrateResetAnimations(Connection connection, UUID uuid, ConfigurationSection playerSection) 
            throws SQLException {
        
        String sql = "INSERT OR REPLACE INTO reset_animations (uuid, animation, owned, selected) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // 当前选中的重置动画
            if (playerSection.contains("reset_animation")) {
                String selectedAnimation = playerSection.getString("reset_animation");
                try {
                    ResetAnimation.valueOf(selectedAnimation); // 验证动画名称
                    
                    statement.setString(1, uuid.toString());
                    statement.setString(2, selectedAnimation);
                    statement.setBoolean(3, true); // 拥有
                    statement.setBoolean(4, true); // 选中
                    statement.addBatch();
                } catch (IllegalArgumentException e) {
                    logger.warning("无效的重置动画: " + selectedAnimation + " (玩家: " + uuid + ")");
                }
            }
            
            // 拥有的重置动画
            ConfigurationSection animationsSection = playerSection.getConfigurationSection("reset_animation_owned");
            if (animationsSection != null) {
                for (String animationName : animationsSection.getKeys(false)) {
                    if (animationsSection.getBoolean(animationName, false)) {
                        try {
                            ResetAnimation.valueOf(animationName); // 验证动画名称
                            
                            // 检查是否已经添加了选中的动画
                            boolean isSelected = animationName.equals(playerSection.getString("reset_animation"));
                            if (!isSelected) {
                                statement.setString(1, uuid.toString());
                                statement.setString(2, animationName);
                                statement.setBoolean(3, true); // 拥有
                                statement.setBoolean(4, false); // 不选中
                                statement.addBatch();
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warning("无效的重置动画: " + animationName + " (玩家: " + uuid + ")");
                        }
                    }
                }
            }
            
            statement.executeBatch();
        }
    }
    
    /**
     * 迁移平均时间数据
     */
    private void migrateAverageTimes(Connection connection, UUID uuid, ConfigurationSection playerSection) 
            throws SQLException {
        
        ConfigurationSection avgTimeSection = playerSection.getConfigurationSection("avg_time");
        ConfigurationSection avgTimeAmountSection = playerSection.getConfigurationSection("avg_time_amount");
        
        if (avgTimeSection == null && avgTimeAmountSection == null) return;
        
        String updateSql = "UPDATE player_stats SET average_time = ?, average_time_amount = ? WHERE uuid = ? AND category = ?";
        String insertSql = "INSERT OR IGNORE INTO player_stats (uuid, category, personal_best, attempts_completed, attempts_failed, average_time, average_time_amount) VALUES (?, ?, 0, 0, 0, ?, ?)";
        
        try (PreparedStatement updateStatement = connection.prepareStatement(updateSql);
             PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            
            // 获取所有avg time data类别
            Set<String> categories = new HashSet<>();
            if (avgTimeSection != null) {
                categories = avgTimeSection.getKeys(false);
            }
            if (avgTimeAmountSection != null) {
                categories.addAll(avgTimeAmountSection.getKeys(false));
            }
            
            for (String category : categories) {
                double avgTime = avgTimeSection != null ? avgTimeSection.getDouble(category, 0.0) : 0.0;
                int avgTimeAmount = avgTimeAmountSection != null ? avgTimeAmountSection.getInt(category, 0) : 0;
                
                // 先尝试更新
                updateStatement.setDouble(1, avgTime);
                updateStatement.setInt(2, avgTimeAmount);
                updateStatement.setString(3, uuid.toString());
                updateStatement.setString(4, category);
                
                int updated = updateStatement.executeUpdate();

                // 如果没有更新到任何行 则插入新行 这么做是为了部分情况下可能用到增量更新
                if (updated == 0) {
                    insertStatement.setString(1, uuid.toString());
                    insertStatement.setString(2, category);
                    insertStatement.setDouble(3, avgTime);
                    insertStatement.setInt(4, avgTimeAmount);
                    insertStatement.executeUpdate();
                }
            }
        }
    }
    
    /**
     * 迁移尝试次数数据
     */
    private void migrateAttempts(Connection connection, UUID uuid, ConfigurationSection playerSection) 
            throws SQLException {
        
        ConfigurationSection completedSection = playerSection.getConfigurationSection("attempts_completed");
        ConfigurationSection failedSection = playerSection.getConfigurationSection("attempts_failed");
        
        if (completedSection == null && failedSection == null) return;
        
        String updateSql = "UPDATE player_stats SET attempts_completed = ?, attempts_failed = ? WHERE uuid = ? AND category = ?";
        String insertSql = "INSERT OR IGNORE INTO player_stats (uuid, category, personal_best, attempts_completed, attempts_failed, average_time, average_time_amount) VALUES (?, ?, 0, ?, ?, 0, 0)";
        
        try (PreparedStatement updateStatement = connection.prepareStatement(updateSql);
             PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            
            // 获取所有尝试次数类别
            Set<String> categories = new HashSet<>();
            if (completedSection != null) {
                categories = completedSection.getKeys(false);
            }
            if (failedSection != null) {
                categories.addAll(failedSection.getKeys(false));
            }
            
            for (String category : categories) {
                int completed = completedSection != null ? completedSection.getInt(category, 0) : 0;
                int failed = failedSection != null ? failedSection.getInt(category, 0) : 0;
                
                // 先尝试更新
                updateStatement.setInt(1, completed);
                updateStatement.setInt(2, failed);
                updateStatement.setString(3, uuid.toString());
                updateStatement.setString(4, category);
                
                int updated = updateStatement.executeUpdate();
                
                // 如果没有更新到任何行 则插入新行 这么做是为了部分情况下可能用到增量更新
                if (updated == 0) {
                    insertStatement.setString(1, uuid.toString());
                    insertStatement.setString(2, category);
                    insertStatement.setInt(3, completed);
                    insertStatement.setInt(4, failed);
                    insertStatement.executeUpdate();
                }
            }
        }
    }
    
    /**
     * 迁移全局日志设置
     */
    private void migrateGlobalLogs(Connection connection, YamlConfiguration config) throws SQLException {
        ConfigurationSection globalSection = config.getConfigurationSection("GLOBAL");
        if (globalSection == null) return;
        
        ConfigurationSection logsSection = globalSection.getConfigurationSection("LOGS");
        if (logsSection == null) return;
        
        ConfigurationSection adminSection = logsSection.getConfigurationSection("admin");
        if (adminSection == null) return;
        
        String sql = "INSERT OR REPLACE INTO global_logs (uuid, enabled) VALUES (?, ?)";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String uuidStr : adminSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    boolean enabled = adminSection.getBoolean(uuidStr, false);
                    
                    statement.setString(1, uuid.toString());
                    statement.setBoolean(2, enabled);
                    statement.addBatch();
                } catch (IllegalArgumentException e) {
                    logger.warning("无效的UUID在全局日志设置中: " + uuidStr);
                }
            }
            statement.executeBatch();
        }
        
        logger.info("全局日志设置迁移完成");
    }
    
    /**
     * 创建YAML文件的备份
     */
    private void createBackup(File originalFile) {
        try {
            String backupName = originalFile.getName() + ".backup." + System.currentTimeMillis();
            File backupFile = new File(originalFile.getParent(), backupName);
            
            if (originalFile.renameTo(backupFile)) {
                logger.info("已创建YAML文件备份: " + backupFile.getName());
            } else {
                logger.warning("无法创建YAML文件备份");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "创建备份时发生错误", e);
        }
    }
    
    /**
     * 验证迁移结果
     */
    public boolean validateMigration(File yamlFile) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(yamlFile);
            Set<String> playerUuids = config.getKeys(false);
            playerUuids.remove("GLOBAL");
            
            logger.info("开始验证迁移结果，玩家数量: " + playerUuids.size());
            
            int validationErrors = 0;
            SQLitePlayerDataImpl sqliteImpl = new SQLitePlayerDataImpl(databaseManager);
            
            for (String uuidStr : playerUuids) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection playerSection = config.getConfigurationSection(uuidStr);
                    
                    if (playerSection != null) {
                        // 验证金币
                        double yamlCoins = playerSection.getDouble("coins", 0.0);
                        double sqliteCoins = sqliteImpl.getCoins(uuid);
                        if (Math.abs(yamlCoins - sqliteCoins) > 0.01) {
                            logger.warning("金币数据不匹配 - 玩家: " + uuid + ", YAML: " + yamlCoins + ", SQLite: " + sqliteCoins);
                            validationErrors++;
                        }
                        
                        // 验证个人最佳成绩
                        ConfigurationSection pbSection = playerSection.getConfigurationSection("pb");
                        if (pbSection != null) {
                            for (String category : pbSection.getKeys(false)) {
                                double yamlPb = pbSection.getDouble(category, 0.0);
                                double sqlitePb = sqliteImpl.getPersonalBest(uuid, category);
                                if (Math.abs(yamlPb - sqlitePb) > 0.01) {
                                    logger.warning("个人最佳成绩不匹配 - 玩家: " + uuid + ", 类别: " + category + 
                                                 ", YAML: " + yamlPb + ", SQLite: " + sqlitePb);
                                    validationErrors++;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "验证玩家数据时发生错误: " + uuidStr, e);
                    validationErrors++;
                }
            }
            
            if (validationErrors == 0) {
                logger.info("验证完成，所有数据迁移正确！");
                return true;
            } else {
                logger.warning("验证完成，发现 " + validationErrors + " 个错误");
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "验证迁移时发生错误", e);
            return false;
        }
    }
    
    /**
     * 查找FastBuilder插件的数据文件
     */
    public File findFastBuilderDataFile() {
        Plugin fastBuilderPlugin = Bukkit.getPluginManager().getPlugin("FastBuilder");
        if (fastBuilderPlugin == null) {
            logger.warning("未找到FastBuilder插件，无法自动查找数据文件");
            return null;
        }
        
        File fastBuilderDataFolder = fastBuilderPlugin.getDataFolder();
        if (!fastBuilderDataFolder.exists()) {
            logger.warning("FastBuilder插件数据文件夹不存在: " + fastBuilderDataFolder.getAbsolutePath());
            return null;
        }
        
        // 查找数据文件（只支持.yml）
        File dataFile = new File(fastBuilderDataFolder, "playerdata.yml");
        if (dataFile.exists()) {
            logger.info("找到FastBuilder数据文件: " + dataFile.getAbsolutePath());
            return dataFile;
        }
        
        logger.info("在FastBuilder插件文件夹中未找到playerdata.yml文件");
        return null;
    }
    
    /**
     * 查找本插件数据文件夹中的YAML文件
     */
    public File findLocalDataFile() {
        if (!plugin.getDataFolder().exists()) {
            return null;
        }

        File dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (dataFile.exists()) {
            logger.info("找到本地数据文件: " + dataFile.getAbsolutePath());
            return dataFile;
        }
        
        return null;
    }
    
    /**
     * 快速迁移接口 - 自动查找并迁移YAML数据文件
     * 优先从FastBuilder插件文件夹查找，然后查找本插件文件夹
     */
    public boolean autoFindYmlFile() {
        File yamlFile = findFastBuilderDataFile();

        if (yamlFile == null) {
            logger.info("未找到需要迁移的YAML数据文件");
            logger.info("支持的文件位置:");
            logger.info("1. FastBuilder插件文件夹: plugins/FastBuilder/playerdata.yml");
            logger.info("2. 本插件文件夹: " + plugin.getDataFolder().getPath() + "/playerdata.yml");
            return true;
        }
        
        logger.info("发现数据文件: " + yamlFile.getName() + "，开始自动迁移...");
        boolean success = migrateFromPath(yamlFile.getAbsolutePath());
        
        if (success) {
            logger.info("数据迁移成功！");
            logger.info("原文件已自动备份，建议在确认迁移正确后删除原YAML文件");
            
            // 验证迁移结果
            if (validateMigration(yamlFile)) {
                logger.info("数据验证通过，迁移完全成功！");
            } else {
                logger.warning("数据验证发现问题，请检查迁移结果");
            }
        } else {
            logger.warning("数据迁移失败，请检查日志并手动处理");
        }
        
        return success;
    }
    
    /**
     * 手动迁移指定的YAML文件
     * 
     * @param yamlFilePath YAML文件的完整路径
     * @return 是否迁移成功
     */
    public boolean migrateFromPath(String yamlFilePath) {
        File yamlFile = new File(yamlFilePath);
        
        if (!yamlFile.exists()) {
            logger.warning("指定的YAML文件不存在: " + yamlFilePath);
            return false;
        }

        String fileName = yamlFile.getName().toLowerCase();
        if (!fileName.endsWith(".yml")) {
            logger.warning("文件不是有效的YAML格式: " + yamlFilePath);
            logger.info("支持的文件扩展名: .yml");
            return false;
        }
        
        logger.info("开始迁移指定的YAML文件: " + yamlFilePath);

        logger.info("开始从YAML文件迁移数据: " + yamlFile.getName());
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(yamlFile);
            
            // 获取所有玩家UUID
            Set<String> playerUuids = config.getKeys(false);
            if (playerUuids.isEmpty()) {
                logger.info("YAML文件中没有找到玩家数据");
                return true;
            }
            
            // 移除全局配置节点
            playerUuids.remove("GLOBAL");
            
            logger.info("找到 " + playerUuids.size() + " 个玩家的数据，开始迁移...");
            
            int migratedCount = 0;
            int errorCount = 0;
            
            try (Connection connection = databaseManager.getConnection()) {
                connection.setAutoCommit(false); // 使用事务
                
                for (String uuidStr : playerUuids) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        ConfigurationSection playerSection = config.getConfigurationSection(uuidStr);
                        
                        if (playerSection != null) {
                            migratePlayerData(connection, uuid, playerSection);
                            migratedCount++;
                            
                            // 定期提交事务
                            if (migratedCount % PluginConfig.MIGRATION_BATCH_SIZE == 0) {
                                connection.commit();
                                logger.info("已迁移 " + migratedCount + " 个玩家的数据...");
                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "迁移玩家数据失败: " + uuidStr, e);
                        errorCount++;
                    }
                }
                
                // 迁移全局日志设置
                migrateGlobalLogs(connection, config);
                
                connection.commit(); // 最终提交
                
                logger.info("数据迁移完成！成功: " + migratedCount + ", 失败: " + errorCount);
                
                if (migratedCount > 0) {
                    // 创建备份
                    createBackup(yamlFile);
                }
                
                return errorCount == 0;
                
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "数据库迁移失败", e);
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "读取YAML文件失败", e);
            return false;
        }
    }
} 