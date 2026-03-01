# Audio Synth - Sound Synthesizer Module

![Modrinth](https://img.shields.io/badge/Forge-1.20.1-1976d2?style=flat-square)
![Version](https://img.shields.io/badge/version-1.1.0-green?style=flat-square)

**Audio Synth** is a Minecraft Forge mod that adds a custom block capable of synthesizing and playing multiple waveforms—the **Sound Synthesizer**. It supports Sine, Square, Triangle, Sawtooth waves, and White Noise, featuring frequency modulation, mixed output, independent volume control, redstone activation, range-based attenuation, and a simple GUI configuration interface. Global volume can be adjusted via the mod's configuration screen, and each block can have its own decibel gain setting.

---

## ✨ Features

- 🎵 **Multiple Waveforms**: Sine, Square, Triangle, Sawtooth, White Noise
- 🎛 **Frequency Modulation**: Supports FM synthesis with adjustable modulation depth and frequency
- 🔀 **Mixed Output**: Adjustable mix factor (reserved for extension; current version uses a single waveform)
- 🔊 **Independent Volume Control**: Per-block decibel gain adjustment (-60dB ~ +60dB)
- 📏 **Range Attenuation**: Configurable sound propagation range (in blocks); silent beyond range, with distance-based volume falloff
- 🔴 **Redstone Activation**: Starts playing upon receiving a redstone signal, stops when signal is lost
- 🖥 **GUI Configuration**: Right-click the block to open the configuration interface, adjust parameters in real-time, click "Apply" to save and close
- ⚙️ **Global Volume**: Adjust the global volume multiplier for all blocks via the mod configuration screen (Mods → Audio Synth → Config)
- 🧩 **Conditional Recipes**: Automatically adapts to three different crafting recipes based on whether the Create mod is installed and its version
- 🧵 **Thread Safety**: Audio playback runs on independent threads, supporting simultaneous sound from multiple blocks

---

## 📦 Dependencies

- **Minecraft Forge 47.4.4+** (for 1.20.1)
- **Create Mod** (Optional): If installed, the recipe adjusts automatically based on its version

> Note: The global volume configuration screen requires Forge's Mods menu support; no additional mods are needed.

---

## 🔧 Installation

1. Install **Minecraft Forge 1.20.1**.
2. Download the `.jar` file for this mod and place it in the `mods` folder.
3. (Optional) Install the corresponding version of the Create mod if you wish to use its recipes.
4. Launch the game.

---

## 🎮 Usage Guide

### Obtaining the Block
- Craft the **Sound Synthesizer** according to its recipe (see below).

### Placement and Activation
- After placement, right-click to open the GUI and configure parameters.
- Apply a redstone signal (e.g., lever, redstone dust) to start playback.
- Removing the redstone signal stops playback.

### GUI Parameters
| Parameter | Description | Range |
|------|------|------|
| Frequency (Hz) | Base frequency | 20 ~ 2000 |
| Waveform | Select waveform type | Sine/Square/Triangle/Sawtooth/Noise |
| Mix Factor | Reserved (currently fixed at 0.5) | 0.0 ~ 1.0 |
| Modulation Depth | Frequency modulation depth | 0.0 ~ 1.0 |
| Modulation Frequency (Hz) | Modulation frequency | 0.1 ~ 20 |
| Range (blocks) | Sound propagation range | 1 ~ 64 |
| Volume (dB) | Per-block independent volume gain | -60 ~ +60 |

### Global Volume Configuration
- From the main menu, click **Mods** → Select **Audio Synth** → Click **Config**.
- Adjust the global volume multiplier using the slider (0.0 ~ 2.0), click "Done" to save.

---

## 🔨 Crafting Recipes

The recipe automatically adapts based on whether the **Create** mod is installed and its version.

### Case 1: Create version ≥ 6.0
- **Recipe Type**: Shapeless
- **Materials**:
  - 1 × Brass Casing (`create:brass_casing`)
  - 1 × Note Block (`minecraft:note_block`)
  - 1 × Pulse Timer (`create:pulse_timer`)
  - 1 × Electron Tube (`create:electron_tube`)

### Case 2: Create version ≥ 0.5 and < 6.0
- **Recipe Type**: Shapeless
- **Materials**:
  - 1 × Brass Casing (`create:brass_casing`)
  - 1 × Note Block (`minecraft:note_block`)
  - 1 × Electron Tube (`create:electron_tube`)
  - 1 × Pulse Extender (`create:pulse_extender`)
  - 2 × Pulse Repeaters (`create:pulse_repeater`)

### Case 3: Create not installed or version < 0.5
- **Recipe Type**: Shaped (3×3)
- **Pattern**:
  ```
  G C G
  R N R
  G C G
  ```
- **Materials**:
  - G = Gold Ingot (`minecraft:gold_ingot`)
  - R = Redstone Repeater (`minecraft:repeater`)
  - N = Note Block (`minecraft:note_block`)
  - C = Redstone Comparator (`minecraft:comparator`)
- **Output**: 1 × Sound Synthesizer

> The game automatically detects the environment and loads the correct recipe. Mods like JEI will display the corresponding recipe.

---

## ⚙️ Configuration File

The client configuration file is located at `.minecraft/config/audiosynth-client.toml`:

```toml
[Audio Settings]
    # Global volume multiplier for all sound generator blocks (0.0 - 2.0)
    globalVolume = 1.0
```

It can be modified via the Config button in the Mods menu or by directly editing the file.

---

## 🧪 Development Information

- **Mod ID**: `audiosynth`
- **Main Class**: `top.hibermod.audiosynth.AudioSynth`
- **MCP Mappings**: official 1.20.1
- **Forge Version**: 47.4.4

### Building
Use Gradle to build:
```bash
./gradlew build
```
The output will be in `build/libs/`.

---

## 📄 License

This project is open-sourced under the **MIT License**. See the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements

- Thanks to the Forge team for the mod development framework
- Thanks to the Create mod for its excellent mechanical components
- Thanks to all players who use and test this mod

---

## 📮 Feedback & Support

For questions or suggestions, please submit them on the project's GitHub Issues page.

---

**Enjoy your sonic experiments!** 🎶
