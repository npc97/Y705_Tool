## 介绍

一个适用于Y700第五代的LSPosed模块，实现了如下功能：
1. 阻止选定的应用被「最近使用」页面清除，包括“全部清除”和滑动清除。（不影响安全中心的内存加速功能）
2. 阻止状态栏弹出的 OTA 更新提示

（理论上也适用于所有使用ZUXOS、并且安装了「com.zui.launcher（ZUX桌面）」和「com.lenovo.ota（系统更新）」的设备 ）

## 安装需求
LSPosed >= 101.0.0

com.zui.launcher（ZUX桌面） == 18.2.0.0375

com.lenovo.ota（系统更新） == V9.2.1.260114


## 使用方法
1. 下载并安装本模块的APK
2. 在LSPosed管理器中激活本模块
3. 在模块作用域中选定「com.zui.launcher（ZUX桌面）」和「com.lenovo.ota（系统更新）」
4. 为本模块授权ROOT权限（可选，用于修改后自动结束「ZUX桌面」和「系统更新」的进程）
5. 打开模块的APP界面，即可使用