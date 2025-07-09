package cc.azuramc.refactorfastbuilder.database;

import cc.azuramc.refactorfastbuilder.config.PluginConfig;
import cc.azuramc.refactorfastbuilder.constants.Messages;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 高性能数据库连接管理器
 * 使用HikariCP连接池提供高效的数据库连接管理
 * 
 * @author an5w1r@163.com
 */
public class DatabaseManager {
    private final Plugin plugin;
    private final Logger logger;
    private final PluginConfig config;
    private HikariDataSource dataSource;
    private final ExecutorService executorService;
    
    // SQL语句常量
    private static final String CREATE_PLAYER_DATA_TABLE = 
        "CREATE TABLE IF NOT EXISTS player_data (" +
        "    uuid TEXT NOT NULL," +
        "    key TEXT NOT NULL," +
        "    value TEXT," +
        "    PRIMARY KEY (uuid, key)" +
        ")";
    
    private static final String CREATE_PLAYER_BLOCKS_TABLE = 
        "CREATE TABLE IF NOT EXISTS player_blocks (" +
        "    uuid TEXT NOT NULL," +
        "    material TEXT NOT NULL," +
        "    data_value INTEGER NOT NULL," +
        "    owned BOOLEAN DEFAULT FALSE," +
        "    PRIMARY KEY (uuid, material, data_value)" +
        ")";
    
    private static final String CREATE_PLAYER_STATS_TABLE = 
        "CREATE TABLE IF NOT EXISTS player_stats (" +
        "    uuid TEXT NOT NULL," +
        "    category TEXT NOT NULL," +
        "    personal_best REAL DEFAULT 0," +
        "    attempts_completed INTEGER DEFAULT 0," +
        "    attempts_failed INTEGER DEFAULT 0," +
        "    average_time REAL DEFAULT 0," +
        "    average_time_amount INTEGER DEFAULT 0," +
        "    PRIMARY KEY (uuid, category)" +
        ")";
    
    private static final String CREATE_GLOBAL_LOGS_TABLE = 
        "CREATE TABLE IF NOT EXISTS global_logs (" +
        "    uuid TEXT PRIMARY KEY," +
        "    enabled BOOLEAN DEFAULT FALSE" +
        ")";
    
    private static final String CREATE_RESET_ANIMATIONS_TABLE = 
        "CREATE TABLE IF NOT EXISTS reset_animations (" +
        "    uuid TEXT NOT NULL," +
        "    animation TEXT NOT NULL," +
        "    owned BOOLEAN DEFAULT FALSE," +
        "    selected BOOLEAN DEFAULT FALSE," +
        "    PRIMARY KEY (uuid, animation)" +
        ")";
    
    // 创建索引以提高查询性能
    private static final String[] CREATE_INDEXES = {
        "CREATE INDEX IF NOT EXISTS idx_player_data_uuid ON player_data(uuid)",
        "CREATE INDEX IF NOT EXISTS idx_player_blocks_uuid ON player_blocks(uuid)",
        "CREATE INDEX IF NOT EXISTS idx_player_stats_uuid ON player_stats(uuid)",
        "CREATE INDEX IF NOT EXISTS idx_player_stats_category ON player_stats(category)",
        "CREATE INDEX IF NOT EXISTS idx_reset_animations_uuid ON reset_animations(uuid)"
    };
    
    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = new PluginConfig(plugin);
        this.executorService = Executors.newFixedThreadPool(PluginConfig.DB_THREAD_POOL_SIZE, r -> {
            Thread thread = new Thread(r, config.getDatabaseThreadNamePrefix());
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * 初始化数据库连接池和表结构
     */
    public void initialize() {
        try {
            setupDataSource();
            createTables();
            logger.info(Messages.DATABASE_INITIALIZED);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, Messages.DATABASE_INIT_FAILED, e);
            throw new RuntimeException("无法初始化数据库", e);
        }
    }
    
    /**
     * 设置HikariCP数据源
     */
    private void setupDataSource() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getDatabaseJdbcUrl());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");

        hikariConfig.setConnectionTestQuery("SELECT 1");

        hikariConfig.setMaximumPoolSize(PluginConfig.CONNECTION_POOL_MAX_SIZE);
        hikariConfig.setMinimumIdle(PluginConfig.CONNECTION_POOL_MIN_IDLE);
        hikariConfig.setIdleTimeout(PluginConfig.CONNECTION_IDLE_TIMEOUT);
        hikariConfig.setMaxLifetime(PluginConfig.CONNECTION_MAX_LIFETIME);
        hikariConfig.setConnectionTimeout(PluginConfig.CONNECTION_TIMEOUT);
        hikariConfig.setValidationTimeout(PluginConfig.VALIDATION_TIMEOUT);
        hikariConfig.setLeakDetectionThreshold(PluginConfig.LEAK_DETECTION_THRESHOLD);
        
        hikariConfig.setPoolName(config.getConnectionPoolName());
        
        this.dataSource = new HikariDataSource(hikariConfig);
        
        // 在连接建立后配置SQLite优化选项
        try (Connection conn = dataSource.getConnection()) {
            for (String pragma : PluginConfig.SQLITE_OPTIMIZATION_PRAGMAS) {
                try (PreparedStatement stmt = conn.prepareStatement(pragma)) {
                    stmt.execute();
                }
            }
            logger.info(Messages.DATABASE_OPTIMIZATION_APPLIED);
        } catch (SQLException e) {
            logger.log(Level.WARNING, Messages.DATABASE_OPTIMIZATION_FAILED, e);
        }
    }
    
    /**
     * 创建所有必需的数据库表
     */
    private void createTables() throws SQLException {
        try (Connection connection = getConnection()) {
            // 创建表
            executeUpdate(connection, CREATE_PLAYER_DATA_TABLE);
            executeUpdate(connection, CREATE_PLAYER_BLOCKS_TABLE);
            executeUpdate(connection, CREATE_PLAYER_STATS_TABLE);
            executeUpdate(connection, CREATE_GLOBAL_LOGS_TABLE);
            executeUpdate(connection, CREATE_RESET_ANIMATIONS_TABLE);
            
            // 创建索引
            for (String indexSql : CREATE_INDEXES) {
                executeUpdate(connection, indexSql);
            }
            
            logger.info(Messages.DATABASE_TABLES_CREATED);
        }
    }
    
    /**
     * 执行更新SQL语句
     */
    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
    
    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("数据源未初始化或已关闭");
        }
        return dataSource.getConnection();
    }
    
    /**
     * 异步执行数据库操作
     */
    public CompletableFuture<Void> executeAsync(DatabaseOperation operation) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                operation.execute(connection);
            } catch (SQLException e) {
                logger.log(Level.WARNING, Messages.DATABASE_ASYNC_OPERATION_FAILED, e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * 异步执行带返回值的数据库操作
     */
    public <T> CompletableFuture<T> executeAsync(DatabaseQuery<T> query) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                return query.execute(connection);
            } catch (SQLException e) {
                logger.log(Level.WARNING, Messages.DATABASE_ASYNC_QUERY_FAILED, e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * 关闭数据库连接池
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info(Messages.DATABASE_POOL_CLOSED);
        }
    }
    
    /**
     * 数据库操作接口
     */
    @FunctionalInterface
    public interface DatabaseOperation {
        void execute(Connection connection) throws SQLException;
    }
    
    /**
     * 数据库查询接口
     */
    @FunctionalInterface
    public interface DatabaseQuery<T> {
        T execute(Connection connection) throws SQLException;
    }
} 