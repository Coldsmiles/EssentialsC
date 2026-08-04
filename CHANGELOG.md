# 更新日志

## 1.3.0 - 2026-08-02

### 新增

- 按 HuskHomes 的单服逻辑完整实现 TPA 请求、接受、拒绝、忽略、预热、冷却、位置快照、音效与相关权限。
- 新增维护模式，包括 MOTD、BossBar、登录拦截、白名单、管理员通知和 LuckPerms 异步权限检查。
- 新增 SkinBridge，可识别 InfstarMC、LittleSkin 与自定义 Blessing Skin Provider，并通过 MineSkin 与 Paper Profile API 同步皮肤。
- 新增管理模式独立状态存储、玩家功能状态管理与 JEI 配方同步版本适配。

### 优化

- 按便捷方块、管理、修复与皮肤功能重新整理运行时模块。
- 完善潜影盒会话保护、TPA 请求队列、配置迁移、数据原子写入与持久化错误处理。
- 适配 Paper `1.21.11`、`26.1.2` 与 `26.2`，最终插件保持 Java 21 字节码兼容。
- 迁移 Paper 弃用 API，并将弃用警告设为构建错误。
- 统一 HuskHomes 风格的中英文消息、颜色和配置注释。

### 注意事项

- 最低服务端版本为 Paper `1.21.11`，不支持 Folia 或 Fabric 服务端。
- LuckPerms 为推荐软依赖；未安装时维护模式会在玩家加入后复核绕过权限。
- SkinBridge 默认关闭，启用前必须配置有效的 MineSkin API Key。
- 主配置版本为 `2`。
