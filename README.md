# EssentialsC

> 一个轻量级的 Paper 服务器插件，灵感来自 CMI，但更加精简、易用且现代化。

[![Version](https://img.shields.io/github/v/release/Coldsmiles/EssentialsC?style=flat-square)](https://github.com/Coldsmiles/EssentialsC/releases)
[![License](https://img.shields.io/github/license/Coldsmiles/EssentialsC?style=flat-square)](LICENSE)
[![Paper](https://img.shields.io/badge/Paper-1.21+-8A2BE2?style=flat-square)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21+-orange?style=flat-square)](https://www.oracle.com/java/)

## ✨ 核心特性

### 🎯 随身功能方块
随时随地打开各种功能性方块，无需放置实体方块：
- **工作台** (`/workbench`, `/wb`)
- **铁砧** (`/anvil`)
- **制图台** (`/cartographytable`, `/ct`)
- **砂轮** (`/grindstone`, `/gs`)
- **织布机** (`/loom`)
- **锻造台** (`/smithingtable`, `/st`)
- **切石机** (`/stonecutter`, `/sc`)
- **末影箱** (`/enderchest`, `/ec`)

### 📦 智能容器管理
- **潜影盒快捷打开** - 潜行+右键直接打开（类似 CMI）
  - ✅ 支持自定义标题（可配置）
  - ✅ 防刷物品机制（快照验证 + 数量检查）
  - ✅ 防止套娃（不能放入另一个潜影盒）
  - ✅ 异常恢复（物品丢失自动掉落）

### 🔧 实用工具
- **帽子** (`/hat`) - 将手中物品戴在头上
- **自杀** (`/suicide`, `/die`) - 快速自杀
- **飞行** (`/fly`) - 切换飞行模式
- **修复** (`/repair`, `/rep`) - 修复手中或所有物品
- **饱食** (`/feed`) - 补满饱食度

### 💚 生存辅助
- **治疗** (`/heal`) - 恢复生命值和饱食度
- **隐身** (`/vanish`, `/v`) - 管理员隐身模式

### 📊 管理功能
- **玩家查询** (`/seen`, `/info`) - 查看玩家上线时间和信息
- **功能方块菜单** (`/essc blocks`) - GUI 方块集合面板
- **配置重载** (`/essc reload`) - 重新加载配置文件

---

## 🌍 多语言支持

- ✅ 完整的中文和英文配置
- ✅ 方块标题自动跟随客户端语言
- ✅ 可自定义所有消息文本

## ⚡ 权限系统

- ✅ 精细的权限管理
- ✅ 帮助菜单智能显示（只显示有权限的命令）
- ✅ 默认仅 OP 可用，可通过权限插件授权
- ✅ CMI 风格的命令别名支持

## 📦 快速开始

### 系统要求
- **服务器**: Paper 1.21+
- **Java**: 21+

### 安装步骤
1. 下载最新版本的 [`essentialsc-*.jar`](https://github.com/Coldsmiles/EssentialsC/releases)
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器
4. 编辑 `plugins/EssentialsC/config.yml` 配置语言
5. （可选）使用权限插件为玩家授予相应权限

## 🎮 命令列表

### 基础命令
| 命令 | 说明 |
|------|------|
| `/essc help` | 显示帮助菜单（根据权限动态显示） |
| `/essc reload` | 重载配置（管理员） |
| `/essc blocks` | 打开功能方块菜单 |

### 功能方块命令
| 命令 | 别名 | 说明 |
|------|------|------|
| `/workbench` | `/wb` | 打开工作台 |
| `/anvil` | - | 打开铁砧 |
| `/cartographytable` | `/ct` | 打开制图台 |
| `/grindstone` | `/gs` | 打开砂轮 |
| `/loom` | - | 打开织布机 |
| `/smithingtable` | `/st` | 打开锻造台 |
| `/stonecutter` | `/sc` | 打开切石机 |
| `/enderchest` | `/ec` | 打开末影箱 |

### 其他命令
| 命令 | 别名 | 说明 |
|------|------|------|
| `/hat` | - | 将手中物品戴在头上 |
| `/suicide` | `/die` | 自杀 |
| `/fly` | - | 切换飞行模式 |
| `/heal` | - | 恢复生命值和饱食度 |
| `/vanish` | `/v` | 切换隐身模式（管理员） |
| `/seen` | `/info` | 查看玩家信息（管理员） |
| `/feed` | - | 补满饱食度 |
| `/repair` | `/rep` | 修复手中或所有物品 |

> 💡 **提示**: 使用 `/repair all` 可以修复背包中的所有物品

## ⚙️ 配置说明

### config.yml
```yaml
# 语言设置 (en_US, zh_CN)
language: "zh_CN"

# 通用设置
settings:
  enable-feedback: true  # 启用命令反馈消息

# 潜影盒设置
shulkerbox:
  default-title: "&e潜影盒"  # 默认标题（支持颜色代码）
```

### 自定义语言
编辑 `plugins/EssentialsC/lang/` 目录下的语言文件来自定义所有消息文本。

## 🔐 权限节点

所有命令默认需要 OP 权限。使用权限插件授予权限：

### 基础权限
```
essentialsc.command.workbench         # 工作台
essentialsc.command.anvil             # 铁砧
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
essentialsc.command.feed              # 饱食度
essentialsc.command.repair            # 修复
essentialsc.shulkerbox.open           # 潜行+右键潜影盒
```

### 管理权限
```
essentialsc.command.blocks            # 功能方块菜单
essentialsc.command.reload            # 重载配置
essentialsc.command.help              # 帮助（默认开放）
```

### 通配符
```
essentialsc.*                         # 所有权限
```

## 🔨 从源码构建

```bash
git clone https://github.com/Coldsmiles/EssentialsC.git
cd EssentialsC
./gradlew build
```

编译后的文件位于 `build/libs/essentialsc-*.jar`

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 👨‍💻 作者
- GitHub: [@Coldsmiles](https://github.com/Coldsmiles)
- 网站: www.infstar.cn

## ⭐ 支持

如果觉得这个插件对你有帮助，请考虑在 GitHub 上给它一个 Star！
