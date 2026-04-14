# EssentialsC

一个轻量级的 Paper 服务器核心插件，灵感来自 CMI，但更加精简、易用且现代化。

## ✨ 功能特性

### 🎯 随身功能方块
随时随地打开各种功能性方块，无需放置实体方块：
- 工作台 (`/workbench`, `/wb`)
- 铁砧 (`/anvil`)
- 制图台 (`/cartographytable`, `/ct`)
- 砂轮 (`/grindstone`, `/gs`)
- 织布机 (`/loom`)
- 锻造台 (`/smithingtable`, `/st`)
- 切石机 (`/stonecutter`, `/sc`)

### 📦 容器管理
- **末影箱** (`/enderchest`, `/ec`) - 随时访问末影箱（参考 EssentialsX 实现）
- **潜影盒快捷打开** - 潜行+右键点击潜影盒直接打开内容（类似 CMI）
  - 支持自定义标题
  - 防刷物品机制
  - 防止套娃（不能在潜影盒中放入另一个潜影盒）

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

### 🌍 多语言支持
- 完整的中文和英文配置
- 方块标题自动跟随客户端语言
- 可自定义所有消息文本

### ⚡ 权限控制
- 精细的权限管理系统
- 帮助菜单智能显示（只显示有权限的命令）
- 默认仅 OP 可用，可通过权限插件授权
- CMI 风格的命令别名支持

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
/essc reload        # 重载配置（管理员）
/essc blocks        # 打开功能方块菜单
```

### 功能方块命令
```
/workbench, /wb     # 打开工作台
/anvil              # 打开铁砧
/cartographytable, /ct  # 打开制图台
/grindstone, /gs    # 打开砂轮
/loom               # 打开织布机
/smithingtable, /st # 打开锻造台
/stonecutter, /sc   # 打开切石机
/enderchest, /ec    # 打开末影箱
```

### 其他命令
```
/hat                # 将手中物品戴在头上
/suicide, /die      # 自杀
/fly                # 切换飞行模式
/heal               # 恢复生命值和饱食度
/vanish, /v         # 切换隐身模式（管理员）
/seen, /info <玩家> # 查看玩家信息（管理员）
/feed               # 补满饱食度
/repair, /rep       # 修复手中或所有物品
/repair all         # 修复所有物品
```

### 命令别名
- `/essentialsc` = `/essc`
- `/workbench` = `/wb`
- `/cartographytable` = `/ct`
- `/grindstone` = `/gs`
- `/smithingtable` = `/st`
- `/stonecutter` = `/sc`
- `/enderchest` = `/ec`
- `/suicide` = `/die`
- `/vanish` = `/v`
- `/seen` = `/info`
- `/repair` = `/rep`

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
```

### 自定义语言
编辑 `plugins/EssentialsC/lang/` 目录下的语言文件来自定义所有消息文本。

## 🔐 权限节点

所有命令默认需要 OP 权限。使用权限插件（如 LuckPerms）授予权限：

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
essentialsc.command.blocks            # 功能方块菜单
essentialsc.command.reload            # 重载配置
essentialsc.command.help              # 帮助（默认开放）
essentialsc.shulkerbox.open           # 潜行+右键潜影盒直接打开
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

## 📝 特性

- ✅ CMI 风格的命令别名系统
- ✅ 智能权限过滤的帮助菜单
- ✅ 完整的多语言支持
- ✅ 功能方块权限菜单
- ✅ 潜行+右键潜影盒直接打开（防刷机制）
- ✅ 末影箱参考 EssentialsX 实现（100% 安全）
- ✅ 潜影盒自定义标题配置
- ✅ 轻量级无依赖设计
- ✅ 现代化的 Paper API 支持

## 🔄 更新日志

### v1.2.0
- ✨ 新增潜行+右键潜影盒直接打开功能（类似 CMI）
  - 支持自定义标题（config.yml 配置）
  - 防刷物品机制（快照验证 + 数量检查）
  - 防止套娃（不能放入另一个潜影盒）
  - 异常恢复（物品丢失自动掉落）
- ✨ 末影箱改用 EssentialsX 实现方式（100% 安全）
- ✨ 功能方块菜单配置化（从 config.yml 读取）
- ✨ 功能方块菜单添加音效反馈
- ✨ CMI 风格命令别名系统
- ⚡ 优化代码结构和性能
- 📝 完善权限配置
- 🗑️ 移除管理员菜单
- 🗑️ 移除附魔台功能

### v1.1.0
- ✨ 新增功能方块菜单 (`/essc blocks`)
- ✨ 添加命令别名系统
- ⚡ 优化代码结构和性能
- 📝 完善权限配置

### v1.0.0
- 🎉 首次发布
- 实现基础功能方块命令
- 添加多语言支持
- 实现权限控制系统

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
