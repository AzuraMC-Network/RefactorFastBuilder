package cc.azuramc.refactorfastbuilder.service;

import cc.azuramc.refactorfastbuilder.config.PluginConfig;
import cc.azuramc.refactorfastbuilder.constants.Messages;
import cc.azuramc.refactorfastbuilder.database.DatabaseManager;
import cc.azuramc.refactorfastbuilder.database.SQLitePlayerDataImpl;
import cf.pies.fastbuilder.api.FastbuilderProvider;
import cf.pies.fastbuilder.api.PlayerData;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.logging.Logger;

/**
 * 插件服务管理器
 * 负责管理所有服务的初始化、配置和生命周期
 * 
 * @author an5w1r@163.com
 */
public class PluginServiceManager {
    
    private final Plugin plugin;
    private final Logger logger;
    private final PluginConfig config;
    
    private DatabaseManager databaseManager;
    private SQLitePlayerDataImpl playerDataImpl;
    private MigrationService migrationService;
    
    public PluginServiceManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = new PluginConfig(plugin);
    }
    
    /**
     * 初始化所有服务
     * 
     * @throws Exception 如果初始化失败
     */
    public void initializeServices() throws Exception {
        logger.info(Messages.PLUGIN_STARTING);
        
        // 初始化数据库管理器
        initializeDatabaseManager();
        
        // 初始化PlayerData实现
        initializePlayerDataImpl();
        
        // 初始化迁移服务
        initializeMigrationService();
        
        // 注册PlayerData实现到FastBuilder API
        registerPlayerDataManager();
        
        logger.info(Messages.PLUGIN_STARTED);
    }
    
    /**
     * 关闭所有服务
     */
    public void shutdownServices() {
        logger.info(Messages.PLUGIN_STOPPING);
        
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        logger.info(Messages.PLUGIN_STOPPED);
    }
    
    /**
     * 初始化数据库管理器
     */
    private void initializeDatabaseManager() throws Exception {
        databaseManager = new DatabaseManager(plugin);
        databaseManager.initialize();
    }
    
    /**
     * 初始化PlayerData实现
     */
    private void initializePlayerDataImpl() {
        playerDataImpl = new SQLitePlayerDataImpl(databaseManager);
    }
    
    /**
     * 初始化迁移服务
     */
    private void initializeMigrationService() {
        migrationService = new YamlToSqliteConverter(databaseManager, plugin);
    }
    
    /**
     * 注册PlayerData管理器到FastBuilder API
     */
    private void registerPlayerDataManager() {
        FastbuilderProvider.getApi().setPlayerDataManager(playerDataImpl);
    }
    
    /**
     * 检查是否需要迁移数据文件
     */
    public void checkForMigrationFiles() {
        boolean foundFiles = false;
        
        // 检查FastBuilder插件文件夹
        File fastBuilderFile = migrationService.findFastBuilderDataFile();
        if (fastBuilderFile != null) {
            logger.info(Messages.MIGRATION_FILES_DETECTED);
            foundFiles = true;
        }
        
        if (foundFiles) {
            logger.info(Messages.SUPPORTED_FORMATS_INFO);
            logger.info(Messages.USE_INFO_COMMAND);
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public PlayerData getPlayerData() {
        return playerDataImpl;
    }
    
    public MigrationService getMigrationService() {
        return migrationService;
    }
    
    public PluginConfig getConfig() {
        return config;
    }
} 