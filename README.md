# RefactorFastBuilder

*[🇨🇳 中文版](README_ZH_CN.md) | 🇺🇸 English*

A Minecraft server plugin that provides SQLite data storage implementation for FastBuilder plugin with YAML data migration support.

## ⚠️ Important Notice

### Required Dependencies
**You MUST place your own FastBuilder plugin jar file into the `libs/` directory before building this plugin!**

This is because:
- Different versions of FastBuilder may have different class structures and obfuscation
- Direct dependencies may cause compatibility issues with SQL PlayerData implementation
- Need to ensure complete consistency with the FastBuilder version actually used on your server

### Code Quality Statement
**This project has mediocre code quality and is intended for learning and reference purposes only.**

Possible design points that could be optimized:
- **Architecture Design**: Lacks comprehensive layered architecture
- **Code Standards**: Some classes and methods lack sufficient documentation comments
- **Exception Handling**: Exception handling mechanism is not comprehensive enough, lacks graceful degradation strategies
- **Transaction Management**: Database transaction management can be further optimized
- **Test Coverage**: Lacks unit tests and integration tests
- **Performance Optimization**: Database queries and connection pool configuration can be further optimized
- **Design Patterns**: Better use of design patterns to improve code maintainability

## Features

### Data Storage
- **SQLite Database**: Uses SQLite as data storage engine
- **Connection Pool Management**: Integrates HikariCP for high-performance database connection pooling
- **Database Table Design**:
  - `player_data` - Player basic data
  - `player_blocks` - Player owned blocks
  - `player_stats` - Player statistics
  - `global_logs` - Global log settings
  - `reset_animations` - Reset animation data

### Data Migration
- **YAML Migration**: Supports migrating data from YAML files to SQLite
- **Auto Discovery**: Automatically finds FastBuilder data files
- **Data Validation**: Provides data integrity validation after migration
- **Command Line Tools**: Provides convenient migration commands

### Performance Optimization
- **Asynchronous Operations**: Database operations use asynchronous execution to avoid blocking the main thread
- **Caching Mechanism**: Creates cache to prevent frequent database I/O

## Build Instructions

### Requirements
- Java 8
- Gradle build tool

### Build Steps

1. **Prepare FastBuilder Plugin**
   ```bash
   # Copy your FastBuilder plugin jar file to libs directory
   cp /path/to/your/fastbuilder.jar libs/
   ```

2. **Build Plugin**
   ```bash
   # Windows
   gradlew shadowJar
   
   # Linux/Mac
   ./gradlew shadowJar
   ```

3. **Deploy Plugin**
   ```bash
   # Build artifact located at build/libs/RefactorFastBuilder-1.0-SNAPSHOT-all.jar
   cp build/libs/RefactorFastBuilder-1.0-SNAPSHOT-all.jar /path/to/server/plugins/
   ```

## Usage

### Basic Commands
```
/refactorfastbuilder migrate [path]  # Migrate YAML data to SQLite
/refactorfastbuilder validate       # Validate migration results
/refactorfastbuilder info           # Show plugin information

# Command aliases
/rfb migrate
/rfb validate 
/rfb info
```

### Permission Configuration
```yaml
permissions:
  refactorfastbuilder.admin:        # Administrator permission
    default: op
  refactorfastbuilder.allblocks:    # All blocks permission
    default: false
  refactorfastbuilder.allanimations: # All animations permission
    default: false
  refactorfastbuilder.logs:         # Log management permission
    default: op
```

## Configuration Files

The plugin will automatically create configuration files in the `plugins/RefactorFastBuilder/` directory:
- `database.db` - SQLite database file
- Configuration options are set through constants in the code

## Technical Architecture

### Main Components
- **DatabaseManager** - Database connection and table management
- **MigrationService** - Data migration service interface
- **YamlToSqliteConverter** - YAML to SQLite conversion implementation
- **PluginServiceManager** - Service lifecycle management

## Development Notes

### Improvement Suggestions
If you want to improve this project, consider focusing on the following aspects:

1. **Refactor Service Layer**: Introduce clearer service layer abstraction
2. **Add Configuration Files**: Move hardcoded configurations to external configuration files
3. **Improve Exception Handling**: Establish unified exception handling mechanism
4. **Add Unit Tests**: Improve code quality and maintainability
5. **Optimize SQL Queries**: Use prepared statements and batch operations

### Dependency Management
```gradle
dependencies {
    compileOnly fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'org.xerial:sqlite-jdbc:3.46.0.0'
    implementation 'com.zaxxer:HikariCP:4.0.3'
    implementation 'org.yaml:snakeyaml:2.2'
}
```

## License

This project is for learning and reference purposes only. Please modify and optimize according to your needs.

## Author

- Author: an5w1r@163.com
- Project Nature: Learning project with limited code quality

## Disclaimer

This plugin is a learning project with mediocre code quality and may contain bugs and design flaws. Please conduct thorough testing and code review before using in production environments. 