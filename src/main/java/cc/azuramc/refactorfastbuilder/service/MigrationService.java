package cc.azuramc.refactorfastbuilder.service;

import java.io.File;

/**
 * 数据迁移服务接口
 * 定义YAML到SQLite数据迁移的核心功能
 * 
 * @author an5w1r@163.com
 */
public interface MigrationService {
    
    /**
     * 从指定路径迁移数据
     * 
     * @param yamlFilePath YAML文件路径
     * @return 是否迁移成功
     */
    boolean migrateFromPath(String yamlFilePath);
    
    /**
     * 自动查找YAML文件
     * 
     * @return 是否迁移成功
     */
    boolean autoFindYmlFile();
    
    /**
     * 验证迁移结果
     * 
     * @param yamlFile 原始YAML文件
     * @return 是否验证通过
     */
    boolean validateMigration(File yamlFile);
    
    /**
     * 查找FastBuilder插件的数据文件
     * 
     * @return 找到的数据文件，如果未找到则返回null
     */
    File findFastBuilderDataFile();
    
    /**
     * 查找本地数据文件
     * 
     * @return 找到的数据文件，如果未找到则返回null
     */
    File findLocalDataFile();
} 