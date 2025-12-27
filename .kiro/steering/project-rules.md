## 基本规则
1. 中文界面
2. 不生成文档
3. 不保留兼容接口
4. 修改后不编译启动
5. 每次修改前先 commit

## UI 规范
- 窗口：80%高×95%宽，背景 `Color(0xFFECECEC)`，圆角 8dp
- 标题栏：`Color(0xFFE7E7E7)`，按钮 `Color(0xFF007AFF)`
- 分组标题：13sp，高度 35dp
- 列表项：15sp，高度 38dp
- 内边距：10dp，间距：10dp

## 统一组件
- `SectionTitle`：分组标题
- `AppDivider`：分隔线
- `DialogHeader`：标题栏
- SectionTitle 和 Card 同一 Column，Card 间用 Spacer(10.dp)

## 项目说明
基于 dadb + scrcpy，参考 scrcpy-mobile，无 tailscare/vnc