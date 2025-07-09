# RefactorFastBuilder

*🇨🇳 中文版 | [🇺🇸 English](README.md)*

一个为FastBuilder插件提供SQLite数据存储实现的Minecraft服务端插件，支持YAML数据迁移功能。

## ⚠️ 重要说明

### 必需依赖
**构建此插件前，您必须将自己的FastBuilder插件jar文件放入 `libs/` 目录中！**

这是因为：
- 不同版本的FastBuilder可能有不同的类结构和混淆
- 直接依赖可能导致SQL PlayerData实现出现兼容性问题
- 需要确保与您服务器上实际使用的FastBuilder版本完全一致

### 代码质量声明
**本项目代码质量一般，仅作为学习和参考用途。**

可能存在以下可优化的设计点：
- **架构设计**：缺乏完善的分层架构
- **代码规范**：部分类和方法缺乏充分的文档注释
- **异常处理**：异常处理机制不够完善，缺乏优雅的降级策略
- **事务管理**：数据库事务管理可以进一步优化
- **测试覆盖**：缺乏单元测试和集成测试
- **性能优化**：可以进一步优化数据库查询和连接池配置
- **设计模式**：可以更好地运用设计模式来提高代码的可维护性

## 功能说明

### 🗄数据存储
- **SQLite数据库**：使用SQLite作为数据存储引擎
- **连接池管理**：集成HikariCP提供高性能数据库连接池
- **数据表设计**：
  - `player_data` - 玩家基础数据
  - `player_blocks` - 玩家拥有的方块
  - `player_stats` - 玩家统计数据
  - `global_logs` - 全局日志设置
  - `reset_animations` - 重置动画数据

### 数据迁移
- **YAML迁移**：支持从YAML文件迁移数据到SQLite
- **自动发现**：自动查找FastBuilder的数据文件
- **数据验证**：迁移后提供数据完整性验证
- **命令行工具**：提供便捷的迁移命令

### 性能优化
- **异步操作**：数据库操作采用异步执行避免阻塞主线程
- **缓存机制**：创建缓存防止频繁数据库IO

## 构建说明

### 环境要求
- Java 8
- Gradle 构建工具

### 构建步骤

1. **准备FastBuilder插件**
   ```bash
   # 将您的FastBuilder插件jar文件复制到libs目录
   cp /path/to/your/fastbuilder.jar libs/
   ```

2. **构建插件**
   ```bash
   # Windows
   gradlew shadowJar
   
   # Linux/Mac
   ./gradlew shadowJar
   ```

3. **部署插件**
   ```bash
   # 构建产物位于 build/libs/RefactorFastBuilder-1.0-SNAPSHOT-all.jar
   cp build/libs/RefactorFastBuilder-1.0-SNAPSHOT-all.jar /path/to/server/plugins/
   ```

## 使用方法

### 基础命令
```
/refactorfastbuilder migrate [路径]  # 迁移YAML数据到SQLite
/refactorfastbuilder validate       # 验证数据迁移结果
/refactorfastbuilder info           # 显示插件信息

# 命令别名
/rfb migrate
/rfb validate 
/rfb info
```

### 权限配置
```yaml
permissions:
  refactorfastbuilder.admin:        # 管理员权限
    default: op
  refactorfastbuilder.allblocks:    # 所有方块权限
    default: false
  refactorfastbuilder.allanimations: # 所有动画权限
    default: false
  refactorfastbuilder.logs:         # 日志管理权限
    default: op
```

## 配置文件

插件会自动在 `plugins/RefactorFastBuilder/` 目录下创建配置文件：
- `database.db` - SQLite数据库文件
- 配置选项通过代码中的常量进行设置

## 技术架构

### 主要组件
- **DatabaseManager** - 数据库连接和表管理
- **MigrationService** - 数据迁移服务接口
- **YamlToSqliteConverter** - YAML到SQLite转换实现
- **PluginServiceManager** - 服务生命周期管理

## 开发说明

### 改进建议
如果您想要改进此项目，建议关注以下方面：

1. **重构服务层**：引入更清晰的服务层抽象
2. **添加配置文件**：将硬编码配置移至外部配置文件
3. **完善异常处理**：建立统一的异常处理机制
4. **添加单元测试**：提高代码质量和可维护性
5. **优化SQL查询**：使用预编译语句和批量操作

### 依赖管理
```gradle
dependencies {
    compileOnly fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'org.xerial:sqlite-jdbc:3.46.0.0'
    implementation 'com.zaxxer:HikariCP:4.0.3'
    implementation 'org.yaml:snakeyaml:2.2'
}
```

## 许可证

本项目仅作为学习和参考用途，请根据您的需求进行适当的修改和优化。

## 作者

- 作者：an5w1r@163.com
- 项目性质：学习项目，代码质量有限

## 免责声明

此插件是学习项目，代码质量一般，可能存在bug和设计缺陷。生产环境使用前请进行充分测试和代码审查。 