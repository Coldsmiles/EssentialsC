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
- 配置与文本分离：行为配置放在 `config.yml`，提示文本放在 `lang/`
- 支持模块裁剪，便于按需构建不同版本

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

### 其它功能

- Shift + 右键快捷打开潜影盒
- 潜影盒交互保护，尽量避免刷物品、吞物品和嵌套放入问题
- 管理模式独立背包、装备栏与状态切换
- Enderman 掉落方块控制
- JEI 配方同步修复

## 构建变体

项目目前提供三个常用构建版本：

| 版本 | 产物名 | 说明 |
| --- | --- | --- |
| 标准版 | `EssentialsC-<version>.jar` | 默认版本，不包含 `mob-drops` 模块 |
| 完整版 | `EssentialsC-all-<version>.jar` | 包含全部模块 |
| 精简版 | `EssentialsC-lite-<version>.jar` | 仅排除 `blocks` 模块，保留 `mob-drops` |

如果需要进一步裁剪模块，也可以使用自定义构建参数生成 `custom` 版本。

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
./gradlew buildAllVersions
```

Windows 可使用：

```powershell
.\gradlew.bat buildAllVersions
```

构建产物输出到 `build/libs/`。

常用任务：

```bash
./gradlew shadowJarStandard
./gradlew shadowJarAll
./gradlew shadowJarLite
```

## 开发说明

- 使用 `paperweight-userdev` 进行 Paper 开发
- 运行时通过反射加载可选模块，避免裁剪版本因类缺失而启动失败
- 发布流程基于 GitHub Actions 和 Gradle Wrapper

## 许可证

本项目基于 [MIT License](LICENSE) 开源。

## 仓库

- GitHub: <https://github.com/Coldsmiles/EssentialsC>
- Gitea: <https://git.infstar.cn/InfStarMC/EssentialsC>
