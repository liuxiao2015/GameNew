# 游戏配置模块

## 目录结构

```
common/common-config/src/main/java/com/game/config/
├── config/          # 配置类 (由导表工具生成)
│   ├── GlobalConfig.java
│   ├── ItemConfig.java
│   ├── LevelConfig.java
│   └── MonsterConfig.java
│
└── container/       # 容器类 (手动维护或工具生成)
    ├── GlobalConfigContainer.java
    ├── ItemConfigContainer.java
    ├── LevelConfigContainer.java
    └── MonsterConfigContainer.java
```

## 导表工具对接

### 配置类生成规则

导表工具生成的配置类放在 `config/` 目录，需满足：

1. **包名**: `com.game.config.config`
2. **类名**: `{表名}Config` (如 `ItemConfig`)
3. **实现接口**: `GameConfig`
4. **必须字段**: `id` (主键)

**模板示例:**

```java
package com.game.config.config;

import com.game.core.config.game.GameConfig;
import lombok.Data;

/**
 * {表名}配置
 * <p>
 * 对应配置文件: {file}.json
 * </p>
 *
 * @author 导表工具生成
 */
@Data
public class {表名}Config implements GameConfig {

    private int id;
    
    // ... 其他字段
    
    @Override
    public int getId() {
        return id;
    }
}
```

### 容器类生成规则

容器类放在 `container/` 目录，需满足：

1. **包名**: `com.game.config.container`
2. **类名**: `{表名}ConfigContainer`
3. **继承**: `BaseConfigContainer<{表名}Config>`
4. **注解**: `@Component` 和 `@ConfigContainer`

**模板示例:**

```java
package com.game.config.container;

import com.game.config.config.{表名}Config;
import com.game.core.config.game.BaseConfigContainer;
import com.game.core.config.game.ConfigContainer;
import org.springframework.stereotype.Component;

/**
 * {表名}配置容器
 *
 * @author 导表工具生成
 */
@Component
@ConfigContainer(file = "{file}.json", configClass = {表名}Config.class)
public class {表名}ConfigContainer extends BaseConfigContainer<{表名}Config> {

    // 可添加自定义查询方法
}
```

## JSON 配置文件

配置文件放在 `resources/config/` 目录：

```
resources/
└── config/
    ├── global.json
    ├── item.json
    ├── level.json
    └── monster.json
```

**JSON 格式:**

```json
[
  {"id": 1, "name": "物品1", "type": 1, "quality": 1},
  {"id": 2, "name": "物品2", "type": 2, "quality": 2}
]
```

## 使用方法

### 注入容器

```java
@Autowired
private ItemConfigContainer itemConfigContainer;

public void example() {
    // 根据 ID 获取
    ItemConfig item = itemConfigContainer.get(1001);
    
    // 获取所有
    List<ItemConfig> allItems = itemConfigContainer.getAll();
    
    // 自定义查询
    List<ItemConfig> equipments = itemConfigContainer.getAllEquipments();
}
```

### 热重载

```java
// 重载单个配置
itemConfigContainer.reload();

// 重载所有配置 (通过 ConfigLoader)
@Autowired
private ConfigLoader configLoader;

configLoader.reloadAll();
```

## 导表工具输出目录

导表工具应将生成的文件输出到以下位置：

| 文件类型 | 输出目录 |
|---------|---------|
| 配置类 (.java) | `common/common-config/src/main/java/com/game/config/config/` |
| 容器类 (.java) | `common/common-config/src/main/java/com/game/config/container/` |
| 配置数据 (.json) | `服务模块/src/main/resources/config/` |

## 导表工具命令行示例

```bash
# 假设使用 Python 导表工具
python export_tool.py \
    --input ./excel/ \
    --output-config ./common/common-config/src/main/java/com/game/config/config/ \
    --output-container ./common/common-config/src/main/java/com/game/config/container/ \
    --output-json ./services/service-game/src/main/resources/config/
```
