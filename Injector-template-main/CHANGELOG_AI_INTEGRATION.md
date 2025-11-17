# 更新日志 - AI集成和自动隐藏功能

## 🎉 版本 2.0 - AI集成增强版

### 新增功能

#### 1. 系统托盘自动隐藏
- **新增全局变量和结构**
  - `g_trayIconData`: 托盘图标数据结构
  - `g_trayIconVisible`: 托盘图标可见状态
  - `g_autoHide`: 自动隐藏设置（可配置）

- **新增函数**
  - `CreateTrayIcon()`: 创建系统托盘图标
  - `RemoveTrayIcon()`: 移除系统托盘图标
  - `ShowTrayMenu()`: 显示托盘右键菜单
  - `HideWindowToTray()`: 隐藏窗口到托盘
  - `RestoreWindowFromTray()`: 从托盘恢复窗口

- **新增控件**
  - `IDC_CHK_AUTO_HIDE`: 自动隐藏复选框
  - `IDC_BTN_HIDE_TRAY`: 手动隐藏到托盘按钮

- **消息处理**
  - `WM_TRAYICON`: 托盘图标消息处理
  - 左键双击：恢复窗口
  - 右键菜单：显示/退出

#### 2. AI答题助手

- **新增数据结构**
  - `AiConfig`: AI配置结构（endpoint, apiKey, model）
  - `AiResponsePayload`: AI响应数据封装

- **新增函数**
  - `LoadAiConfig()`: 从INI文件加载AI配置
  - `SaveAiConfig()`: 保存AI配置到INI文件
  - `CallAiApi()`: 调用AI API接口（支持OpenAI兼容接口）

- **新增对话框**
  - **AI配置对话框** (`AiConfigWndProc`)
    - 配置API地址、API Key、模型
    - 测试连接功能
    - 使用说明
  - **AI答题助手对话框** (`AiHelperWndProc`)
    - 输入题目文本框
    - AI解答按钮（支持F5快捷键）
    - 答案显示区域
    - 响应式布局（支持窗口大小调整）

- **新增控件ID**
  - `IDC_BTN_AI_CONFIG`: AI配置按钮
  - `IDC_BTN_AI_HELPER`: AI答题助手按钮
  - `IDC_EDIT_AI_ENDPOINT`: API地址输入框
  - `IDC_EDIT_AI_KEY`: API Key输入框
  - `IDC_EDIT_AI_MODEL`: 模型输入框
  - `IDC_EDIT_AI_QUESTION`: 题目输入框
  - `IDC_EDIT_AI_ANSWER`: 答案显示框
  - `IDC_BTN_AI_ASK`: AI解答按钮
  - `IDC_BTN_AI_SAVE`: 保存配置按钮
  - `IDC_BTN_AI_TEST`: 测试连接按钮

#### 3. 配置持久化

- **配置文件**: `ai_config.ini`
  - `[AI]` 节：存储API配置
  - `[Settings]` 节：存储程序设置
- **自动加载**: 程序启动时自动加载配置
- **自动保存**: 配置更改时自动保存

### 代码改动详情

#### 主要修改文件
- `Project1.cpp` (主文件，约1200+行新增代码)

#### 新增头文件引用
```cpp
#include <string>
#include <sstream>
#include <memory>
#include <cstdio>
```

#### API功能实现
- HTTP/HTTPS请求支持（使用WinHTTP）
- JSON请求构造和响应解析
- 支持Bearer Token认证
- 证书验证（可忽略）
- 超时控制
- 异步处理（多线程）

### 界面布局变化

#### 第二界面新增控件布局
```
原有：
- 路径输入框 + 浏览按钮 + 自动查找
- 启动按钮 + 赞赏按钮
- 状态栏

新增：
- 自动隐藏复选框（启动按钮右侧）
- AI配置按钮
- AI答题助手按钮
- 隐藏到托盘按钮
```

### 使用流程

#### AI配置流程
1. 点击"AI配置"按钮
2. 输入API地址、API Key、模型名称
3. 点击"保存配置"
4. 点击"测试连接"验证配置

#### 答题流程
1. 点击"AI答题助手"打开助手窗口
2. 在题目框中输入或粘贴题目
3. 点击"AI解答"或按F5键
4. 等待AI响应，查看答案

#### 自动隐藏流程
1. 勾选"启动后自动隐藏到托盘"
2. 点击"启动"按钮
3. 程序自动隐藏到系统托盘
4. 双击托盘图标可恢复窗口

### 技术亮点

1. **多线程异步处理**: AI请求使用独立线程，不阻塞UI
2. **资源管理**: 使用RAII模式管理内存和句柄
3. **错误处理**: 完善的错误提示和日志输出
4. **用户体验**: 
   - 按钮禁用防止重复点击
   - 加载状态提示
   - 友好的错误消息
5. **扩展性**: 支持任何OpenAI兼容的API接口

### 配置文件示例

```ini
[AI]
Endpoint=https://api.openai.com/v1/chat/completions
ApiKey=sk-your-api-key-here
Model=gpt-3.5-turbo

[Settings]
AutoHide=1
```

### 安全考虑

- API Key以密文形式显示（PASSWORD模式）
- 配置文件存储在本地（用户应妥善保管）
- HTTPS连接（支持证书验证）
- 无日志记录敏感信息

### 兼容性

- Windows 7/8/10/11
- 需要网络连接（AI功能）
- 支持高DPI显示
- 支持窗口缩放

### 已知限制

1. AI请求需要网络连接
2. API调用可能产生费用（取决于服务商）
3. 响应速度取决于网络和服务商
4. 大量请求可能触发API限流

### 后续改进建议

1. 添加代理服务器配置
2. 支持批量题目处理
3. 添加历史记录功能
4. 支持自定义prompt模板
5. 添加快捷键自定义
6. 支持屏幕截图识别（OCR）

---

## 📝 修改统计

- **新增代码**: ~1300行
- **新增函数**: 12+个
- **新增对话框**: 2个
- **新增控件**: 15+个
- **新增配置项**: 4个

## 🔗 相关文档

- 详细使用说明：`AI_INTEGRATION_README.md`
- 原项目说明：`README.md`

---

**更新日期**: 2024
**作者**: sjyssr
**License**: GPL-3.0
