package cc.azuramc.refactorfastbuilder.constants;

/**
 * 消息常量类
 * 
 * @author an5w1r@163.com
 */
public class Messages {

    public static final String PLUGIN_STARTING = "启动 RefactorFastBuilder 插件...";
    public static final String PLUGIN_STARTED = "RefactorFastBuilder 插件启动完成！";
    public static final String PLUGIN_STOPPING = "关闭 RefactorFastBuilder 插件...";
    public static final String PLUGIN_STOPPED = "RefactorFastBuilder 插件已关闭";
    public static final String PLUGIN_START_FAILED = "插件启动失败";

    public static final String DATABASE_INITIALIZED = "数据库初始化完成，使用SQLite存储";
    public static final String DATABASE_INIT_FAILED = "数据库初始化失败";
    public static final String DATABASE_TABLES_CREATED = "数据库表和索引创建完成";
    public static final String DATABASE_OPTIMIZATION_APPLIED = "SQLite优化配置已应用";
    public static final String DATABASE_OPTIMIZATION_FAILED = "SQLite优化配置失败，将使用默认设置";
    public static final String DATABASE_POOL_CLOSED = "数据库连接池已关闭";
    public static final String DATABASE_ASYNC_OPERATION_FAILED = "异步数据库操作失败";
    public static final String DATABASE_ASYNC_QUERY_FAILED = "异步数据库查询失败";

    public static final String MIGRATION_FILES_DETECTED = "§e检测到FastBuilder数据文件，推荐使用 /refactorfastbuilder migrate 命令迁移数据";
    public static final String SUPPORTED_FORMATS_INFO = "§6支持的数据文件格式: .yml, .yaml";
    public static final String USE_INFO_COMMAND = "§6使用 /refactorfastbuilder info 查看详细信息";

    public static final String NO_PERMISSION = "&c你没有权限执行此命令！";
    public static final String MIGRATION_IN_PROGRESS = "&7正在开始从YAML迁移数据到SQLite...";
    public static final String MIGRATION_COMMAND_SUCCESS = "&a&l✓ &a数据迁移成功完成！";
    public static final String MIGRATION_COMMAND_FAILED = "&c&l✗ &c数据迁移失败，请检查控制台日志";
    public static final String MIGRATION_COMMAND_ERROR = "&c&l✗ &c数据迁移时发生错误: ";
    
    public static final String VALIDATION_IN_PROGRESS = "&7正在开始验证迁移数据...";
    public static final String VALIDATION_SUCCESS = "&a&l✓ &a数据验证通过，迁移数据正确！";
    public static final String VALIDATION_FAILED = "&c&l✗ &c数据验证失败，发现错误，请检查控制台日志";
    public static final String VALIDATION_ERROR = "&c&l✗ &c数据验证时发生错误: ";
    public static final String VALIDATION_USAGE = "&c&l✗ &c用法: /refactorfastbuilder validate <yaml文件>";

    public static final String INFO_VERSION = "&7 • &f版本: &b";
    public static final String INFO_AUTHOR = "&7 • &f作者: &b";
    public static final String INFO_DATABASE = "&7 • &f数据库: &bSQLite";
    public static final String INFO_CACHE = "&7 • &f缓存: &b启用 (5分钟过期)";
    public static final String INFO_CONNECTION_POOL = "&7 • &f连接池: &bHikariCP";
    public static final String INFO_DATABASE_CONNECTED = "&7 • &f数据库状态: &a已连接";
    public static final String INFO_DATABASE_DISCONNECTED = "&7 • &f数据库状态: &c未连接";
    
    public static final String INFO_FASTBUILDER_DATA_FOUND = "&7 • &fFastBuilder数据文件: &6发现 &7(可迁移)";
    public static final String INFO_FASTBUILDER_DATA_NOT_FOUND = "&7 • &fFastBuilder数据文件: &c未找到";
    public static final String INFO_FASTBUILDER_FOLDER_NOT_EXIST = "&7 • &fFastBuilder数据文件: &c插件文件夹不存在";
    public static final String INFO_FASTBUILDER_NOT_INSTALLED = "&7 • &fFastBuilder插件: &c未安装";
    
    public static final String INFO_LOCAL_DATA_FOUND = "&7 • &f本地数据文件: &6发现 &7(可迁移)";
    public static final String INFO_LOCAL_DATA_NOT_FOUND = "&7 • &f本地数据文件: &c未找到";
    public static final String INFO_SUPPORTED_FORMATS = "&8支持的文件格式: &7.yml, .yaml";
    
    public static final String HELP_MIGRATE_COMMAND = "&7 • &f/refactorfastbuilder migrate [文件路径]: 从YAML迁移数据到SQLite";
    public static final String HELP_VALIDATE_COMMAND = "&7 • &f/refactorfastbuilder validate <文件路径>: 验证迁移的数据是否正确";
    public static final String HELP_INFO_COMMAND = "&7 • &f/refactorfastbuilder info: 显示插件信息和数据文件状态";
    
    public static final String HELP_AUTO_DETECTION_INFO = "&8说明: 不指定路径时自动从FastBuilder插件文件夹查找";
    public static final String HELP_SUPPORTED_FILES = "&8支持的数据文件: FastBuilder的playerdata.yml/playerdata.yaml";
    public static final String HELP_SEARCH_ORDER = "&8查找顺序: FastBuilder插件文件夹 → 本插件文件夹";

    public static final String HEADER_MIGRATION = "数据迁移";
    public static final String HEADER_VALIDATION = "数据验证";
    public static final String HEADER_INFO = "信息";
    public static final String HEADER_HELP = "命令帮助";

    public static final String ERROR_MIGRATION_EXCEPTION = "数据迁移时发生错误";
    public static final String ERROR_VALIDATION_EXCEPTION = "验证迁移时发生错误";

} 