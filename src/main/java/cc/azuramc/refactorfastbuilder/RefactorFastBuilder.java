package cc.azuramc.refactorfastbuilder;

import cc.azuramc.refactorfastbuilder.command.MainCommand;
import cc.azuramc.refactorfastbuilder.constants.Messages;
import cc.azuramc.refactorfastbuilder.database.DatabaseManager;
import cc.azuramc.refactorfastbuilder.service.MigrationService;
import cc.azuramc.refactorfastbuilder.service.PluginServiceManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * RefactorFastBuilder 主类
 * 提供高性能的SQLite PlayerData实现和YAML数据迁移功能
 * 
 * @author an5w1r@163.com
 */
public final class RefactorFastBuilder extends JavaPlugin {

    public static RefactorFastBuilder instance;
    private PluginServiceManager serviceManager;

    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // 初始化服务管理器
            serviceManager = new PluginServiceManager(this);
            serviceManager.initializeServices();
            
            // 检查是否需要从YML迁移数据
            serviceManager.checkForMigrationFiles();

            // 注册命令
            registerCommand();
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, Messages.PLUGIN_START_FAILED, e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (serviceManager != null) {
            serviceManager.shutdownServices();
        }
    }

    public static RefactorFastBuilder getInstance() {
        return instance;
    }

    private void registerCommand() {
        getServer().getPluginCommand("refactorfastbuilder").setExecutor(new MainCommand());
    }
    

    /**
     * 获取数据库管理器
     * 
     * @return 数据库管理器实例
     */
    public DatabaseManager getDatabaseManager() {
        return serviceManager != null ? serviceManager.getDatabaseManager() : null;
    }
    
    /**
     * 获取迁移服务
     * 
     * @return 迁移服务实例
     */
    public MigrationService getMigrationService() {
        return serviceManager != null ? serviceManager.getMigrationService() : null;
    }
}
