package cc.azuramc.refactorfastbuilder.database;

import cc.azuramc.refactorfastbuilder.RefactorFastBuilder;
import cf.pies.fastbuilder.api.PlayerData;
import cf.pies.fastbuilder.api.ResetAnimation;
import cf.pies.fastbuilder.illIIiIIliilIIIIiIIiiIIIliiIIiliiilIIIlIIliliiIlil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 高性能SQLite实现的PlayerData
 * 使用连接池、缓存和批处理操作来优化性能
 * 
 * @author an5w1r@163.com
 */
public class SQLitePlayerDataImpl implements PlayerData {
    
    private final DatabaseManager databaseManager;
    private final Logger logger;

    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_TIME = TimeUnit.MINUTES.toMillis(5); // 5分钟缓存过期

    private static final String UPSERT_PLAYER_DATA = 
        "INSERT OR REPLACE INTO player_data (uuid, key, value) VALUES (?, ?, ?)";
    private static final String SELECT_PLAYER_DATA = 
        "SELECT value FROM player_data WHERE uuid = ? AND key = ?";
    private static final String UPSERT_PLAYER_STATS = 
        "INSERT OR REPLACE INTO player_stats (uuid, category, personal_best, attempts_completed, attempts_failed, average_time, average_time_amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_PLAYER_STATS = 
        "SELECT * FROM player_stats WHERE uuid = ? AND category = ?";
    private static final String UPSERT_PLAYER_BLOCK = 
        "INSERT OR REPLACE INTO player_blocks (uuid, material, data_value, owned) VALUES (?, ?, ?, ?)";
    private static final String SELECT_PLAYER_BLOCK = 
        "SELECT owned FROM player_blocks WHERE uuid = ? AND material = ? AND data_value = ?";
    private static final String UPSERT_GLOBAL_LOG = 
        "INSERT OR REPLACE INTO global_logs (uuid, enabled) VALUES (?, ?)";
    private static final String SELECT_GLOBAL_LOGS_ENABLED = 
        "SELECT uuid FROM global_logs WHERE enabled = 1";
    private static final String SELECT_GLOBAL_LOG = 
        "SELECT enabled FROM global_logs WHERE uuid = ?";
    private static final String UPSERT_RESET_ANIMATION = 
        "INSERT OR REPLACE INTO reset_animations (uuid, animation, owned, selected) VALUES (?, ?, ?, ?)";
    private static final String SELECT_RESET_ANIMATION_OWNED = 
        "SELECT owned FROM reset_animations WHERE uuid = ? AND animation = ?";
    private static final String SELECT_RESET_ANIMATION_SELECTED = 
        "SELECT animation FROM reset_animations WHERE uuid = ? AND selected = 1";
    
    public SQLitePlayerDataImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.logger = Bukkit.getLogger();
        
        // 启动定期清理缓存的任务
        startCacheCleanupTask();
    }
    
    /**
     * 启动缓存清理任务
     */
    private void startCacheCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                RefactorFastBuilder.getInstance(),
                this::cleanExpiredCache,
                20L * 60, // 1分钟后开始
                20L * 60  // 每分钟执行一次
        );
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredKeys = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : cacheTimestamps.entrySet()) {
            if (currentTime - entry.getValue() > CACHE_EXPIRE_TIME) {
                expiredKeys.add(entry.getKey());
            }
        }
        
        for (String key : expiredKeys) {
            cache.remove(key);
            cacheTimestamps.remove(key);
        }
    }
    
    /**
     * 从缓存获取数据
     */
    @SuppressWarnings("unchecked")
    private <T> T getFromCache(String key, Class<T> type) {
        Long timestamp = cacheTimestamps.get(key);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRE_TIME) {
            Object value = cache.get(key);
            if (type.isInstance(value)) {
                return (T) value;
            }
        }
        return null;
    }
    
    /**
     * 添加数据到缓存
     */
    private void putToCache(String key, Object value) {
        cache.put(key, value);
        cacheTimestamps.put(key, System.currentTimeMillis());
    }
    
    /**
     * 生成缓存键
     */
    private String getCacheKey(String prefix, UUID uuid, String... params) {
        StringBuilder sb = new StringBuilder(prefix).append(":").append(uuid);
        for (String param : params) {
            sb.append(":").append(param);
        }
        return sb.toString();
    }

    @Override
    public void setPersonalBest(Player player, String category, double time) {
        setPersonalBest(player.getUniqueId(), category, time);
    }

    @Override
    public void setPersonalBest(UUID uuid, String category, double time) {
        String cacheKey = getCacheKey("pb", uuid, category);
        putToCache(cacheKey, time);
        
        databaseManager.executeAsync(connection -> {
            updatePlayerStats(connection, uuid, category, stats -> stats.personalBest = time);
        }).exceptionally(throwable -> {
            logger.log(Level.WARNING, "更新个人最佳成绩失败: " + uuid + ", " + category, throwable);
            return null;
        });
    }

    @Override
    public double getPersonalBest(Player player, String category) {
        return getPersonalBest(player.getUniqueId(), category);
    }

    @Override
    public double getPersonalBest(UUID uuid, String category) {
        String cacheKey = getCacheKey("pb", uuid, category);
        Double cached = getFromCache(cacheKey, Double.class);
        if (cached != null) {
            return cached;
        }
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAYER_STATS)) {
            
            statement.setString(1, uuid.toString());
            statement.setString(2, category);
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    double pb = rs.getDouble("personal_best");
                    putToCache(cacheKey, pb);
                    return pb;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "获取个人最佳成绩失败: " + uuid + ", " + category, e);
        }
        
        return 0.0;
    }

    @Override
    public double getCoins(Player player) {
        return getCoins(player.getUniqueId());
    }

    @Override
    public double getCoins(UUID uuid) {
        String cacheKey = getCacheKey("coins", uuid);
        Double cached = getFromCache(cacheKey, Double.class);
        if (cached != null) {
            return cached;
        }
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAYER_DATA)) {
            
            statement.setString(1, uuid.toString());
            statement.setString(2, "coins");
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    double coins = Double.parseDouble(rs.getString("value"));
                    putToCache(cacheKey, coins);
                    return coins;
                }
            }
        } catch (SQLException | NumberFormatException e) {
            logger.log(Level.WARNING, "获取金币失败: " + uuid, e);
        }
        
        return 0.0;
    }

    @Override
    public void addCoins(Player player, double amount) {
        addCoins(player.getUniqueId(), amount);
    }

    @Override
    public void addCoins(UUID uuid, double amount) {
        double currentCoins = getCoins(uuid);
        double newAmount = currentCoins + amount;
        
        String cacheKey = getCacheKey("coins", uuid);
        putToCache(cacheKey, newAmount);
        
        databaseManager.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_PLAYER_DATA)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, "coins");
                statement.setString(3, String.valueOf(newAmount));
                statement.executeUpdate();
            }
        }).exceptionally(throwable -> {
            logger.log(Level.WARNING, "添加金币失败: " + uuid + ", " + amount, throwable);
            return null;
        });
    }

    @Override
    public void selectBlock(Player player, Material material, byte data) {
        String value = material.name() + ":" + data;
        String cacheKey = getCacheKey("selected_block", player.getUniqueId());
        putToCache(cacheKey, value);
        
        databaseManager.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_PLAYER_DATA)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, "selected_block");
                statement.setString(3, value);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void setPickaxe(Player player, Material material) {
        String cacheKey = getCacheKey("selected_pick", player.getUniqueId());
        putToCache(cacheKey, material.name());
        
        databaseManager.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_PLAYER_DATA)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, "selected_pick");
                statement.setString(3, material.name());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public illIIiIIliilIIIIiIIiiIIIliiIIiliiilIIIlIIliliiIlil getPick(Player player) {
        String cacheKey = getCacheKey("selected_pick", player.getUniqueId());
        String cached = getFromCache(cacheKey, String.class);
        if (cached != null) {
            Material material = Material.valueOf(cached);
            return new illIIiIIliilIIIIiIIiiIIIliiIIiliiilIIIlIIliliiIlil(material, 0);
        }
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAYER_DATA)) {
            
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, "selected_pick");
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String materialName = rs.getString("value");
                    Material material = Material.valueOf(materialName);
                    putToCache(cacheKey, materialName);
                    return new illIIiIIliilIIIIiIIiiIIIliiIIiliiilIIIlIIliliiIlil(material, 0);
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            logger.log(Level.WARNING, "获取选中镐子失败: " + player.getUniqueId(), e);
        }
        
        return new illIIiIIliilIIIIiIIiiIIIliiIIiliiilIIIlIIliliiIlil(Material.DIAMOND_PICKAXE, 0);
    }

    @Override
    public illIIiIIliilIIIIiIIiiIIIliiIIiliiilIIIlIIliliiIlil getBlock(Player player) {
        String cacheKey = getCacheKey("selected_block", player.getUniqueId());
        String cached = getFromCache(cacheKey, String.class);
        if (cached != null) {
            String[] parts = cached.split(":");
            Material material = Material.valueOf(parts[0]);
            int data = Integer.parseInt(parts[1]);
            return new illIIiIIliilIIIIiIIiiIIIliiIIiliiilIIIlIIliliiIlil(material, data);
        }
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAYER_DATA)) {
            
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, "selected_block");
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString("value");
                    String[] parts = value.split(":");
                    Material material = Material.valueOf(parts[0]);
                    int data = Integer.parseInt(parts[1]);
                    putToCache(cacheKey, value);
                    return new illIIiIIliilIIIIiIIiiIIIliiIIiliiilIIIlIIliliiIlil(material, data);
                }
            }
                 } catch (SQLException | IllegalArgumentException e) {
             logger.log(Level.WARNING, "获取选中方块失败: " + player.getUniqueId(), e);
         }
        
        return new illIIiIIliilIIIIiIIiiIIIliiIIiliiilIIIlIIliliiIlil(Material.SANDSTONE, 0);
    }

    @Override
    public void buyBlock(Player player, Material material, int data) {
        buyBlock(player.getUniqueId(), material, data);
    }

    @Override
    public void buyBlock(UUID uuid, Material material, int data) {
        String cacheKey = getCacheKey("block", uuid, material.name(), String.valueOf(data));
        putToCache(cacheKey, true);
        
        databaseManager.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_PLAYER_BLOCK)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, material.name());
                statement.setInt(3, data);
                statement.setBoolean(4, true);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public boolean hasBlock(Player player, Material material, int data, int permissionId) {
        // 检查权限
        if (player.hasPermission("fastbuilder.allblocks")) {
            return true;
        }
        
        // 检查特定权限（如果有配置）
        if (permissionId != -1) {
            // 这里可以添加配置检查逻辑
            // 暂时跳过，因为需要ConfigManager
        }
        
        String cacheKey = getCacheKey("block", player.getUniqueId(), material.name(), String.valueOf(data));
        Boolean cached = getFromCache(cacheKey, Boolean.class);
        if (cached != null) {
            return cached;
        }
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAYER_BLOCK)) {
            
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, material.name());
            statement.setInt(3, data);
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    boolean hasBlock = rs.getBoolean("owned");
                    putToCache(cacheKey, hasBlock);
                    return hasBlock;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "检查方块拥有状态失败: " + player.getUniqueId() + ", " + material, e);
        }
        
        return false;
    }

    @Override
    public Set<Player> getEnabledLogPlayers() {
        Set<Player> enabledPlayers = new HashSet<>();
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_GLOBAL_LOGS_ENABLED);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.hasPermission("fastbuilder.logs")) {
                    enabledPlayers.add(player);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "获取启用日志的玩家失败", e);
        }
        
        return enabledPlayers;
    }

    @Override
    public boolean hasLogsEnabled(Player player) {
        if (!player.hasPermission("fastbuilder.logs")) {
            return false;
        }
        
        String cacheKey = getCacheKey("logs", player.getUniqueId());
        Boolean cached = getFromCache(cacheKey, Boolean.class);
        if (cached != null) {
            return cached;
        }
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_GLOBAL_LOG)) {
            
            statement.setString(1, player.getUniqueId().toString());
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    boolean enabled = rs.getBoolean("enabled");
                    putToCache(cacheKey, enabled);
                    return enabled;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "检查日志启用状态失败: " + player.getUniqueId(), e);
        }
        
        return false;
    }

    @Override
    public void setLogs(Player player, boolean enabled) {
        String cacheKey = getCacheKey("logs", player.getUniqueId());
        putToCache(cacheKey, enabled);
        
        databaseManager.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_GLOBAL_LOG)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setBoolean(2, enabled);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void save() {
        // 批量保存所有待处理的更新
        // 在实际应用中，这里可以实现批量保存逻辑
    }

    @Override
    public void addSuccessfulAttempt(UUID uuid, String category, int amount) {
        databaseManager.executeAsync(connection -> {
            updatePlayerStats(connection, uuid, category, stats -> 
                stats.attemptsCompleted += amount);
        });
    }

    @Override
    public void addFailedAttempt(UUID uuid, String category, int amount) {
        databaseManager.executeAsync(connection -> {
            updatePlayerStats(connection, uuid, category, stats -> 
                stats.attemptsFailed += amount);
        });
    }

    @Override
    public int getSuccessfulAttempts(UUID uuid, String category) {
        return getPlayerStats(uuid, category).attemptsCompleted;
    }

    @Override
    public int getFailedAttempts(UUID uuid, String category) {
        return getPlayerStats(uuid, category).attemptsFailed;
    }

    @Override
    public int getOverallAttempts(UUID uuid, String category) {
        PlayerStats stats = getPlayerStats(uuid, category);
        return stats.attemptsCompleted + stats.attemptsFailed;
    }

    @Override
    public void setResetAnimation(UUID uuid, ResetAnimation animation) {
        databaseManager.executeAsync(connection -> {
            // 首先取消之前选中的动画
            try (PreparedStatement unselectStatement = connection.prepareStatement(
                "UPDATE reset_animations SET selected = 0 WHERE uuid = ? AND selected = 1")) {
                unselectStatement.setString(1, uuid.toString());
                unselectStatement.executeUpdate();
            }
            
            // 设置新的选中动画
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_RESET_ANIMATION)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, animation.name());
                statement.setBoolean(3, true); // 拥有
                statement.setBoolean(4, true); // 选中
                statement.executeUpdate();
            }
        });
    }

    @Override
    public ResetAnimation getResetAnimation(UUID uuid) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_RESET_ANIMATION_SELECTED)) {
            
            statement.setString(1, uuid.toString());
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String animationName = rs.getString("animation");
                    return ResetAnimation.valueOf(animationName);
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            logger.log(Level.WARNING, "获取重置动画失败: " + uuid, e);
        }
        
        return ResetAnimation.DEFAULT;
    }

    @Override
    public void setResetAnimationOwned(UUID uuid, ResetAnimation animation, boolean owned) {
        databaseManager.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_RESET_ANIMATION)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, animation.name());
                statement.setBoolean(3, owned);
                statement.setBoolean(4, false); // 不自动选中
                statement.executeUpdate();
            }
        });
    }

    @Override
    public boolean ownsResetAnimation(Player player, ResetAnimation animation) {
        if (player.hasPermission("fastbuilder.allanimations")) {
            return true;
        }
        
        String cacheKey = getCacheKey("animation_owned", player.getUniqueId(), animation.name());
        Boolean cached = getFromCache(cacheKey, Boolean.class);
        if (cached != null) {
            return cached;
        }
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_RESET_ANIMATION_OWNED)) {
            
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, animation.name());
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    boolean owned = rs.getBoolean("owned");
                    putToCache(cacheKey, owned);
                    return owned;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "检查动画拥有状态失败: " + player.getUniqueId() + ", " + animation, e);
        }
        
        return false;
    }

    @Override
    public void setAverageTime(UUID uuid, String category, double time) {
        databaseManager.executeAsync(connection -> {
            updatePlayerStats(connection, uuid, category, stats -> stats.averageTime = time);
        });
    }

    @Override
    public double getAverageTime(UUID uuid, String category) {
        return getPlayerStats(uuid, category).averageTime;
    }

    @Override
    public void addAverageTimeAmount(UUID uuid, String category) {
        databaseManager.executeAsync(connection -> {
            updatePlayerStats(connection, uuid, category, stats -> stats.averageTimeAmount++);
        });
    }

    @Override
    public int getAverageTimeAmount(UUID uuid, String category) {
        return getPlayerStats(uuid, category).averageTimeAmount;
    }
    
    /**
     * 获取玩家统计数据
     */
    private PlayerStats getPlayerStats(UUID uuid, String category) {
        String cacheKey = getCacheKey("stats", uuid, category);
        PlayerStats cached = getFromCache(cacheKey, PlayerStats.class);
        if (cached != null) {
            return cached;
        }
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAYER_STATS)) {
            
            statement.setString(1, uuid.toString());
            statement.setString(2, category);
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    PlayerStats stats = new PlayerStats();
                    stats.personalBest = rs.getDouble("personal_best");
                    stats.attemptsCompleted = rs.getInt("attempts_completed");
                    stats.attemptsFailed = rs.getInt("attempts_failed");
                    stats.averageTime = rs.getDouble("average_time");
                    stats.averageTimeAmount = rs.getInt("average_time_amount");
                    
                    putToCache(cacheKey, stats);
                    return stats;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "获取玩家统计失败: " + uuid + ", " + category, e);
        }
        
        return new PlayerStats();
    }
    
    /**
     * 更新玩家统计数据
     */
    private void updatePlayerStats(Connection connection, UUID uuid, String category, 
                                 StatsUpdater updater) throws SQLException {
        
        PlayerStats stats = getPlayerStats(uuid, category);
        updater.update(stats);
        
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_PLAYER_STATS)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, category);
            statement.setDouble(3, stats.personalBest);
            statement.setInt(4, stats.attemptsCompleted);
            statement.setInt(5, stats.attemptsFailed);
            statement.setDouble(6, stats.averageTime);
            statement.setInt(7, stats.averageTimeAmount);
            statement.executeUpdate();
        }
        
        // 更新缓存
        String cacheKey = getCacheKey("stats", uuid, category);
        putToCache(cacheKey, stats);
    }
    
    /**
     * 统计数据更新器接口
     */
    @FunctionalInterface
    private interface StatsUpdater {
        void update(PlayerStats stats);
    }
    
    /**
     * 玩家统计数据类
     */
    private static class PlayerStats {
        public double personalBest = 0.0;
        public int attemptsCompleted = 0;
        public int attemptsFailed = 0;
        public double averageTime = 0.0;
        public int averageTimeAmount = 0;
    }
}
