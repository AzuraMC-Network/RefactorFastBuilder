package cc.azuramc.refactorfastbuilder.command;

import cc.azuramc.refactorfastbuilder.RefactorFastBuilder;
import cc.azuramc.refactorfastbuilder.constants.Messages;
import cc.azuramc.refactorfastbuilder.service.MigrationService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.logging.Level;

public class MainCommand implements CommandExecutor {

    private final RefactorFastBuilder plugin = RefactorFastBuilder.getInstance();

    /**
     * 颜色代码转换方法
     */
    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * 获取插件标题头部
     */
    private String getHeader(String function) {
        return color("&b&lRefactorFastBuilder &8- &7v" + plugin.getDescription().getVersion() 
            + " &8- &bSQLite实现FastBuilder数据库 - " + function);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"refactorfastbuilder".equals(command.getName())) {
            return false;
        }

        if (!sender.hasPermission("refactorfastbuilder.admin")) {
            sender.sendMessage(color(Messages.NO_PERMISSION));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "migrate":
                handleMigrateCommand(sender, args);
                break;
            case "validate":
                handleValidateCommand(sender, args);
                break;
            case "info":
                handleInfoCommand(sender);
                break;
            default:
                sendHelpMessage(sender);
        }

        return true;
    }

    /**
     * 处理数据迁移命令
     */
    private void handleMigrateCommand(CommandSender sender, String[] args) {
        sender.sendMessage(getHeader(Messages.HEADER_MIGRATION));
        sender.sendMessage("");
        sender.sendMessage(color(Messages.MIGRATION_IN_PROGRESS));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean success;

                MigrationService migrationService = plugin.getMigrationService();
                if (migrationService == null) {
                    sender.sendMessage(color(Messages.MIGRATION_COMMAND_ERROR + "迁移服务未初始化"));
                    return;
                }
                
                if (args.length > 1) {
                    success = migrationService.migrateFromPath(args[1]);
                } else {
                    success = migrationService.autoFindYmlFile();
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage(color(Messages.MIGRATION_COMMAND_SUCCESS));
                    } else {
                        sender.sendMessage(color(Messages.MIGRATION_COMMAND_FAILED));
                    }
                });

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, Messages.ERROR_MIGRATION_EXCEPTION, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(color(Messages.MIGRATION_COMMAND_ERROR + e.getMessage())));
            }
        });
    }

    /**
     * 处理数据验证命令
     */
    private void handleValidateCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getHeader(Messages.HEADER_VALIDATION));
            sender.sendMessage("");
            sender.sendMessage(color(Messages.VALIDATION_USAGE));
            return;
        }

        sender.sendMessage(getHeader(Messages.HEADER_VALIDATION));
        sender.sendMessage("");
        sender.sendMessage(color(Messages.VALIDATION_IN_PROGRESS));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                MigrationService migrationService = plugin.getMigrationService();
                if (migrationService == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(color(Messages.VALIDATION_ERROR + "迁移服务未初始化"));
                    });
                    return;
                }
                
                File yamlFile = new File(args[1]);
                boolean success = migrationService.validateMigration(yamlFile);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage(color(Messages.VALIDATION_SUCCESS));
                    } else {
                        sender.sendMessage(color(Messages.VALIDATION_FAILED));
                    }
                });

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, Messages.ERROR_VALIDATION_EXCEPTION, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(color(Messages.VALIDATION_ERROR + e.getMessage()));
                });
            }
        });
    }

    /**
     * 处理信息命令
     */
    private void handleInfoCommand(CommandSender sender) {
        sender.sendMessage(getHeader(Messages.HEADER_INFO));
        sender.sendMessage("");
        sender.sendMessage(color(Messages.INFO_VERSION + plugin.getDescription().getVersion()));
        sender.sendMessage(color(Messages.INFO_AUTHOR + plugin.getDescription().getAuthors()));
        sender.sendMessage(color(Messages.INFO_DATABASE));
        sender.sendMessage(color(Messages.INFO_CACHE));
        sender.sendMessage(color(Messages.INFO_CONNECTION_POOL));

        if (plugin.getDatabaseManager() != null) {
            sender.sendMessage(color(Messages.INFO_DATABASE_CONNECTED));
        } else {
            sender.sendMessage(color(Messages.INFO_DATABASE_DISCONNECTED));
        }

        // 检查FastBuilder数据文件
        MigrationService migrationService = plugin.getMigrationService();
        if (migrationService != null) {
            File fastBuilderFile = migrationService.findFastBuilderDataFile();
            if (fastBuilderFile != null) {
                sender.sendMessage(color(Messages.INFO_FASTBUILDER_DATA_FOUND));
            } else {
                // 检查是否安装了FastBuilder插件
                org.bukkit.plugin.Plugin fastBuilderPlugin = plugin.getServer().getPluginManager().getPlugin("FastBuilder");
                if (fastBuilderPlugin != null) {
                    if (fastBuilderPlugin.getDataFolder().exists()) {
                        sender.sendMessage(color(Messages.INFO_FASTBUILDER_DATA_NOT_FOUND));
                    } else {
                        sender.sendMessage(color(Messages.INFO_FASTBUILDER_FOLDER_NOT_EXIST));
                    }
                } else {
                    sender.sendMessage(color(Messages.INFO_FASTBUILDER_NOT_INSTALLED));
                }
            }

            // 检查本地数据文件
            File localFile = migrationService.findLocalDataFile();
            if (localFile != null) {
                sender.sendMessage(color(Messages.INFO_LOCAL_DATA_FOUND));
            } else {
                sender.sendMessage(color(Messages.INFO_LOCAL_DATA_NOT_FOUND));
            }
        }
        
        sender.sendMessage("");
        sender.sendMessage(color(Messages.INFO_SUPPORTED_FORMATS));
    }

    /**
     * 发送帮助信息
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(getHeader(Messages.HEADER_HELP));
        sender.sendMessage("");
        sender.sendMessage(color(Messages.HELP_MIGRATE_COMMAND));
        sender.sendMessage(color(Messages.HELP_VALIDATE_COMMAND));
        sender.sendMessage(color(Messages.HELP_INFO_COMMAND));
        sender.sendMessage("");
        sender.sendMessage(color(Messages.HELP_AUTO_DETECTION_INFO));
        sender.sendMessage(color(Messages.HELP_SUPPORTED_FILES));
        sender.sendMessage(color(Messages.HELP_SEARCH_ORDER));
    }
}
