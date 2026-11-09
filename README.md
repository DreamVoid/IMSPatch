# IMS Patch

对 Google Pixel 设备的电话服务进行修补，使其正常使用运营商服务

基于 vvb2060 的使用 Shizuku 实现的 [Ims](https://github.com/vvb2060/Ims)。

## 使用

本应用是一个 Xposed 模块，至少需要 Xposed API 102。

对电话服务（`com.android.phone`）激活本模块，之后进入模块设置，依次启用以下三个开关：
- 启用 Xposed 注入
- CarrierConfig 覆盖
- 平台能力检查绕过

之后强行停止电话服务、切换飞行模式即可生效。

在搭载 Android 17 的 Google Pixel 9 Pro XL 经过测试，中国电信运营商可以正常使用，如果其他运营商无法正常使用，考虑自行启用“Provisioning 检查绕过”。

## 开发

至少需要 Android SDK 37 才能正常编译。

你可以自由使用本应用的代码，只需要遵守[许可证](LICENSE)。

## 贡献

欢迎进行各种形式的贡献，包括：
- 反馈本模块在你的 Google Pixel 设备是否可用
- 提交用于改进的代码

我不反对使用 AI 工具，只需合理使用，具有基本的编码和审查能力，并对 AI 工具生成的代码负责。