# EssentialsC

一个轻量级的 Paper 服务器核心插件，灵感来自 CMI，但更加精简、易用且现代化。

## ✨ 功能特性

### 🎯 随身功能方块
随时随地打开各种功能性方块，无需放置实体方块：
- 工作台 (`/workbench`)
- 铁砧 (`/anvil`)
- 附魔台 (`/enchantingtable`)
- 制图台 (`/cartographytable`)
- 砂轮 (`/grindstone`)
- 织布机 (`/loom`)
- 锻造台 (`/smithingtable`)
- 切石机 (`/stonecutter`)

### 🔧 实用工具
- **末影箱** (`/enderchest`) - 随时访问末影箱
- **帽子** (`/hat`) - 将手中物品戴在头上
- **自杀** (`/suicide`) - 快速自杀
- **飞行** (`/fly`) - 切换飞行模式

### 💚 生存辅助
- **治疗** (`/heal`) - 恢复生命值和饱食度
- **隐身** (`/vanish`) - 管理员隐身模式

### 📊 管理功能
- **玩家查询** (`/seen`) - 查看玩家上线时间和信息
- **管理菜单** (`/admin`) - 可视化 GUI 管理面板
  - 时间/天气控制
  - 快捷状态恢复
  - 插件重载

### 🌍 多语言支持
- 完整的中文和英文配置
- 方块标题自动跟随客户端语言
- 可自定义所有消息文本

### ⚡ 权限控制
- 精细的权限管理系统
- 帮助菜单智能显示（只显示有权限的命令）
- 默认仅 OP 可用，可通过权限插件授权

## 📦 安装

### 要求
- Paper 1.21+ 服务器
- Java 21+

### 步骤
1. 下载最新版本的 [`essentialsc-*.jar`](https://github.com/Coldsmiles/EssentialsC/releases)
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器
4. 编辑 `plugins/EssentialsC/config.yml` 配置语言
5. （可选）使用权限插件为玩家授予相应权限

## 🎮 命令

### 基础命令
```
/essc help          # 显示帮助菜单（根据权限动态显示）
```

### 功能方块命令
```
/workbench          # 打开工作台
/anvil              # 打开铁砧
/enchantingtable    # 打开附魔台
/cartographytable   # 打开制图台
/grindstone         # 打开砂轮
/loom               # 打开织布机
/smithingtable      # 打开锻造台
/stonecutter        # 打开切石机
/enderchest         # 打开末影箱
```

### 其他命令
```
/hat                # 将手中物品戴在头上
/suicide            # 自杀
/fly                # 切换飞行模式
/heal               # 恢复生命值和饱食度
/vanish             # 切换隐身模式（管理员）
/seen <玩家>        # 查看玩家信息（管理员）
/admin              # 打开管理菜单（管理员）
```

### 命令别名
- `/essentialsc` = `/essc`

## ⚙️ 配置

### config.yml
```yaml
# 语言设置
# 可用语言: en_US, zh_CN
language: "zh_CN"

# 通用设置
settings:
  # 启用或禁用命令反馈消息
  enable-feedback: true
  
  # 所有插件消息的前缀
  message-prefix: "&6[EssentialsC] &r"
```

### 自定义语言
编辑 `plugins/EssentialsC/lang/` 目录下的语言文件来自定义所有消息文本。

## 🔐 权限节点

所有命令默认需要 OP 权限。使用权限插件（如 LuckPerms）授予权限：

```
essentialsc.command.workbench         # 工作台
essentialsc.command.anvil             # 铁砧
essentialsc.command.enchantingtable   # 附魔台
essentialsc.command.cartographytable  # 制图台
essentialsc.command.grindstone        # 砂轮
essentialsc.command.loom              # 织布机
essentialsc.command.smithingtable     # 锻造台
essentialsc.command.stonecutter       # 切石机
essentialsc.command.enderchest        # 末影箱
essentialsc.command.hat               # 帽子
essentialsc.command.suicide           # 自杀
essentialsc.command.fly               # 飞行
essentialsc.command.heal              # 治疗
essentialsc.command.vanish            # 隐身
essentialsc.command.seen              # 玩家查询
essentialsc.command.admin             # 管理菜单
essentialsc.command.help              # 帮助（默认开放）
essentialsc.*                         # 所有权限
```

### 示例：使用 LuckPerms 授权
```bash
# 给单个玩家授权
/lp user <玩家名> permission set essentialsc.command.workbench true

# 给用户组授权
/lp group vip permission set essentialsc.command.workbench true

# 授权所有命令
/lp group admin permission set essentialsc.* true
```

## 🔨 构建

从源代码构建插件：

```bash
git clone https://github.com/Coldsmiles/EssentialsC.git
cd EssentialsC
mvn clean package
```

编译后的文件位于 `target/essentialsc-*.jar`

## 📝 开发计划

- [ ] 冷却时间系统
- [ ] 更多管理功能
- [ ] 数据统计
- [ ] API 支持

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 👨‍💻 作者

**Coldsmiles_7**

- GitHub: [@Coldsmiles](https://github.com/Coldsmiles)
- 网站: www.infstar.cn

## ⭐ 支持

如果觉得这个插件对你有帮助，请考虑在 GitHub 上给它一个 Star！
