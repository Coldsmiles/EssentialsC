# EssentialsC

轻量、现代、面向 Paper 服务端的基础功能插件，灵感来自 CMI，但更聚焦于常用能力与模块化构建。

[![Version](https://img.shields.io/github/v/release/Coldsmiles/EssentialsC?style=flat-square)](https://github.com/Coldsmiles/EssentialsC/releases)
[![License](https://img.shields.io/github/license/Coldsmiles/EssentialsC?style=flat-square)](LICENSE)
[![Paper](https://img.shields.io/badge/Paper-1.21.11%2B-blue?style=flat-square)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)](https://adoptium.net/)

## 项目定位

- 最低支持版本为 `Paper 1.21.11`
- 已适配 `Paper 26.1.2`
- 构建环境固定为 `Java 21`
- 配置、模块开关与文本分离：行为配置放在 `config.yml`，模块开关放在 `modules.yml`，提示文本放在 `lang/`
- 支持运行期模块开关，避免为不同功能组合构建多个插件版本

## 主要功能

### 便捷方块

- `/workbench` `(/wb)`
- `/anvil`
- `/cartographytable` `(/ct, /cartography)`
- `/grindstone` `(/gs)`
- `/loom`
- `/smithingtable` `(/st, /smithing)`
- `/stonecutter` `(/sc)`
- `/enderchest` `(/ec)`
- `/essc blocks` 打开便捷菜单

### 玩家功能

- `/fly`
- `/nightvision` `(/nv)`
- `/glow`
- `/heal`
- `/feed`
- `/repair` `(/rep)`
- `/hat`
- `/suicide` `(/die)`
- `/vanish` `(/v)`
- `/seen` `(/info)`
- `/tpsbar`
- `/essc admin` 管理模式切换
- `/maintenance` `(/maint)` 维护模式管理

### 其它功能

- Shift + 右键快捷打开潜影盒
- 潜影盒交互保护，尽量避免刷物品、吞物品和嵌套放入问题
- 管理模式独立背包、装备栏与状态切换
- 维护模式：替换 MOTD，并阻止无绕过权限的玩家进入
- Enderman 掉落方块控制
- JEI 配方同步修复

## 模块配置

项目现在默认构建一个完整插件，功能是否启用由 `plugins/EssentialsC/modules.yml` 控制。

| 模块 | 默认状态 | 说明 |
| --- | --- | --- |
| `blocks` | 开启 | 便捷方块命令、`/essc blocks` 菜单、潜影盒快捷打开 |
| `player` | 开启 | 飞行、夜视、发光、治疗、喂食、修复、帽子、自杀、隐身、查询玩家 |
| `admin-mode` | 开启 | `/essc admin` 管理模式与独立状态保存 |
| `tpsbar` | 开启 | 插件版 TPSBar，仍受 `config.yml` 中 `tpsbar.mode` 控制 |
| `jei-sync` | 开启 | Fabric / NeoForge JEI 配方同步修复 |
| `mob-drops` | 关闭 | 末影人掉落控制，默认关闭以保留过去标准版行为 |
| `maintenance` | 开启 | 维护模式命令、MOTD 替换和登录拦截 |

修改模块开关后建议重启服务器，使命令注册表和监听器状态完全刷新。`/essc reload` 可以刷新配置和已注册命令的执行检查，但无法从 Bukkit 命令表中真正热移除或新增直连命令。

## 安装说明

1. 从 [Releases](https://github.com/Coldsmiles/EssentialsC/releases) 下载所需版本。
2. 将插件放入服务端的 `plugins/` 目录。
3. 启动一次服务端以生成配置文件。
4. 按需修改 `plugins/EssentialsC/config.yml` 与 `plugins/EssentialsC/lang/` 下的语言文件。
5. 如有需要，使用权限插件为玩家授权。

## 配置说明

当前配置结构以“行为配置”和“文本配置”分离为原则：

- `config.yml`
  - 语言选择
  - 管理模式行为
  - JEI 同步开关
  - 掉落控制
  - TPSBar 模式
  - 便捷菜单布局
- `modules.yml`
  - 功能模块开关
- `maintenance.yml`
  - 维护模式状态
  - 维护 MOTD
  - 维护踢出提示
  - 维护 BossBar
  - 绕过权限
- `lang/zh_CN.yml`、`lang/en_US.yml`
  - 命令反馈
  - 帮助信息
  - 菜单文本
  - 管理模式文本
  - TPSBar 文本

配置文件包含 `config-version`，后续如有结构升级，可基于版本号进行迁移与重建。

## 权限示例

常用权限节点：

```text
essentialsc.command.blocks
essentialsc.command.workbench
essentialsc.command.enderchest
essentialsc.command.fly
essentialsc.command.nightvision
essentialsc.command.glow
essentialsc.command.heal
essentialsc.command.feed
essentialsc.command.repair
essentialsc.command.vanish
essentialsc.command.seen
essentialsc.command.admin
essentialsc.command.tpsbar
essentialsc.shulkerbox.open
essentialsc.mobdrops.enderman
essentialsc.*
```

具体默认值与完整节点以 `paper-plugin.yml` 为准。

## 从源码构建

```bash
git clone https://github.com/Coldsmiles/EssentialsC.git
cd EssentialsC
./gradlew build
```

Windows 可使用：

```powershell
.\gradlew.bat build
```

构建产物输出到 `build/libs/EssentialsC-<version>.jar`。

常用任务：

```bash
./gradlew shadowJar
./gradlew build
./gradlew deployToPaper12111
./gradlew deployToPaper2612
```

## 本地测试服

项目包含两个本地测试服目录：

| 测试服 | 端口 | 部署任务 | 启动脚本 |
| --- | --- | --- | --- |
| Paper 1.21.11 | `25566` | `deployToPaper12111` | `test-server/paper-1.21.11/start.bat` |
| Paper 26.1.2 | `25565` | `deployToPaper2612` | `test-server/paper-26.1.2/start.bat` |

IDEA 运行配置会在启动测试服前自动执行对应部署任务。部署任务会替换 `EssentialsC*.jar`，并删除 `plugins/EssentialsC` 数据目录，以便测试新增默认配置和语言文本。

## 开发说明

- 使用 `paperweight-userdev` 进行 Paper 开发
- 运行时通过 `modules.yml` 控制模块加载，命令与监听器按模块状态注册
- 发布流程基于 GitHub Actions 和 Gradle Wrapper

## 许可证

本项目基于 [MIT License](LICENSE) 开源。

## 仓库

- GitHub: <https://github.com/Coldsmiles/EssentialsC>
- Gitea: <https://git.infstar.cn/InfStarMC/EssentialsC>
