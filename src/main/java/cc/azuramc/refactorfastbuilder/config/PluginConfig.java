package cc.azuramc.refactorfastbuilder.config;

import org.bukkit.plugin.Plugin;

/**
 * 插件配置管理类
 * 
 * @author an5w1r@163.com
 */
public class PluginConfig {
    
    private final Plugin plugin;

    public static final String DATABASE_NAME = "playerdata.db";
    public static final int CONNECTION_POOL_MAX_SIZE = 10;
    public static final int CONNECTION_POOL_MIN_IDLE = 2;
    public static final long CONNECTION_IDLE_TIMEOUT = 300000L; // 5分钟
    public static final long CONNECTION_MAX_LIFETIME = 600000L; // 10分钟
    public static final int CONNECTION_TIMEOUT = 10000; // 10秒
    public static final int VALIDATION_TIMEOUT = 3000; // 3秒
    public static final long LEAK_DETECTION_THRESHOLD = 60000L; // 1分钟

    // 批处理配置
    public static final int MIGRATION_BATCH_SIZE = 100;
    
    // 线程池配置
    public static final int DB_THREAD_POOL_SIZE = 4;
    
    // SQLite参数
    public static final String[] SQLITE_OPTIMIZATION_PRAGMAS = {
        "PRAGMA journal_mode = WAL",
        "PRAGMA synchronous = NORMAL", 
        "PRAGMA cache_size = 10000",
        "PRAGMA temp_store = MEMORY"
    };
    
    public PluginConfig(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 获取数据库文件完整路径
     */
    public String getDatabasePath() {
        return plugin.getDataFolder().getAbsolutePath() + "/" + DATABASE_NAME;
    }
    
    /**
     * 获取数据库JDBC URL
     */
    public String getDatabaseJdbcUrl() {
        return "jdbc:sqlite:" + getDatabasePath();
    }
    
    /**
     * 获取连接池名称
     */
    public String getConnectionPoolName() {
        return "FastBuilder-SQLite-Pool";
    }
    
    /**
     * 获取数据库线程名前缀
     */
    public String getDatabaseThreadNamePrefix() {
        return "FastBuilder-DB-Thread";
    }

} 