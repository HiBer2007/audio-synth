# Audio Synth - 声音合成器模组

![Modrinth](https://img.shields.io/badge/Forge-1.20.1-1976d2?style=flat-square)
![Version](https://img.shields.io/badge/version-1.1.0-green?style=flat-square)

**Audio Synth** 是一个 Minecraft Forge 模组，添加了一个能够合成并播放多种波形的自定义方块——**声音合成器**。支持正弦波、方波、三角波、锯齿波和白噪声，具有频率调制、混合输出、独立音量控制、红石激活、范围衰减等功能，并提供简洁的 GUI 配置界面。全局音量可通过模组配置界面调整，每个方块还可以单独设置分贝增益。

---

## ✨ 功能特性

- 🎵 **多种波形**：正弦波、方波、三角波、锯齿波、白噪声
- 🎛 **频率调制**：支持频率调制（FM），可调节调制深度和频率
- 🔀 **混合输出**：混合因子可调节（预留扩展，当前版本使用单一波形）
- 🔊 **独立音量控制**：每个方块可单独设置分贝增益（-60dB ~ +60dB）
- 📏 **范围衰减**：可设置声音传播范围（格），超出范围静音，近大远小效果
- 🔴 **红石激活**：接收到红石信号后开始播放，信号消失后停止
- 🖥 **GUI 配置**：右键方块打开配置界面，实时调整参数，点击“应用”后保存并关闭
- ⚙️ **全局音量**：通过模组配置界面（Mods → Audio Synth → Config）调整所有方块的全局音量乘数
- 🧩 **条件合成表**：根据是否安装 Create 模组及其版本，自动适配三种不同的合成配方
- 🧵 **线程安全**：音频播放独立线程，支持多方块同时发声

---

## 📦 依赖

- **Minecraft Forge 47.4.4+**（对应 1.20.1）
- **Create 模组**（可选）：如果安装，合成表会根据版本自动调整

> 注：全局音量配置界面需要 Forge 的 Mods 菜单支持，无需额外模组。

---

## 🔧 安装方法

1. 安装 **Minecraft Forge 1.20.1**。
2. 下载本模组的 `.jar` 文件并放入 `mods` 文件夹。
3. （可选）如需使用 Create 相关配方，请安装对应版本的 Create 模组。
4. 启动游戏即可。

---

## 🎮 使用指南

### 获取方块
- 按合成表合成 **声音合成器**（见下文）。

### 放置与激活
- 放置后，右键打开 GUI 配置参数。
- 通入红石信号（如拉杆、红石粉）开始发声。
- 断开红石信号后停止发声。

### GUI 参数说明
| 参数 | 说明 | 范围 |
|------|------|------|
| 频率 (Hz) | 基础频率 | 20 ~ 2000 |
| 波形 | 选择波形类型 | 正弦/方波/三角/锯齿/噪声 |
| 混合因子 | 预留（当前固定为0.5） | 0.0 ~ 1.0 |
| 调制深度 | 频率调制深度 | 0.0 ~ 1.0 |
| 调制频率 (Hz) | 调制频率 | 0.1 ~ 20 |
| 范围 (格) | 声音传播范围 | 1 ~ 64 |
| 音量 (dB) | 方块独立音量增益 | -60 ~ +60 |

### 全局音量配置
- 在主菜单点击 **Mods** → 选择 **Audio Synth** → 点击 **Config**。
- 滑动滑块调整全局音量乘数（0.0 ~ 2.0），点击“完成”保存。

---

## 🔨 合成配方

合成表根据是否安装 **Create** 模组及其版本自动适配。

### 情况 1：Create 版本 ≥ 6.0
- **配方类型**：无序合成
- **材料**：
  - 1 × 黄铜机壳（`create:brass_casing`）
  - 1 × 音符盒（`minecraft:note_block`）
  - 1 × 脉冲计时器（`create:pulse_timer`）
  - 1 × 电子管（`create:electron_tube`）

### 情况 2：Create 版本 ≥ 0.5 且 < 6.0
- **配方类型**：无序合成
- **材料**：
  - 1 × 黄铜机壳（`create:brass_casing`）
  - 1 × 音符盒（`minecraft:note_block`）
  - 1 × 电子管（`create:electron_tube`）
  - 1 × 脉冲延长器（`create:pulse_extender`）
  - 2 × 脉冲中继器（`create:pulse_repeater`）

### 情况 3：未安装 Create 或版本低于 0.5
- **配方类型**：有序合成（3×3）
- **模式**：
  ```
  G   G
  R N R
  G   G
  ```
- **材料**：
  - G = 金锭（`minecraft:gold_ingot`）
  - R = 红石中继器（`minecraft:repeater`）
  - N = 音符盒（`minecraft:note_block`）
- **输出**：1 × 声音合成器

> 游戏会自动检测环境并加载正确的配方，JEI 等模组也会显示对应配方。

---

## ⚙️ 配置文件

客户端配置文件位于 `.minecraft/config/audiosynth-client.toml`：

```toml
[Audio Settings]
    # Global volume multiplier for all sound generator blocks (0.0 - 2.0)
    globalVolume = 1.0
```

可通过 Mods 菜单的 Config 按钮或直接编辑文件修改。

---

## 🧪 开发信息

- **模组 ID**：`audiosynth`
- **主类**：`top.hibermod.audiosynth.AudioSynth`
- **MCP 映射**：official 1.20.1
- **Forge 版本**：47.4.4

### 构建
使用 Gradle 构建：
```bash
./gradlew build
```
产物位于 `build/libs/`。

---

## 📄 许可证

本项目基于 **MIT 许可证** 开源。详情见 [LICENSE](LICENSE) 文件。

---

## 🙏 致谢

- 感谢 Forge 团队提供的模组开发框架
- 感谢 Create 模组提供的优秀机械元件
- 所有使用和测试本模组的玩家

---

## 📮 反馈与支持

如有问题或建议，请在项目的 GitHub Issues 页面提交。

---

**Enjoy your sonic experiments!** 🎶
