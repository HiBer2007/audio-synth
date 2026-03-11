package top.hibermod.audiosynth.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import top.hibermod.audiosynth.AudioSynth;
import top.hibermod.audiosynth.network.ModNetworking;
import top.hibermod.audiosynth.network.SoundSettingsPacket;

@OnlyIn(Dist.CLIENT)
public class SoundGeneratorScreen extends AbstractContainerScreen<SoundGeneratorMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(AudioSynth.MODID, "textures/gui/sound_generator.png");

    private EditBox frequencyField;
    private Button waveformButton;
    private EditBox mixField;
    private EditBox modDepthField;
    private EditBox modFreqField;
    private EditBox rangeField;
    private EditBox volumeDbField; // 新增分贝输入框

    private int waveformIndex = 0;
    private final String[] waveformKeys = {
            "audiosynth.gui.waveform.sine",          // 0: 正弦
            "audiosynth.gui.waveform.square",        // 1: 方波
            "audiosynth.gui.waveform.triangle",      // 2: 三角波
            "audiosynth.gui.waveform.sawtooth",      // 3: 锯齿波
            "audiosynth.gui.waveform.noise",         // 4: 噪声
            "audiosynth.gui.waveform.rectangle",     // 5: 矩形波
            "audiosynth.gui.waveform.wavetable",     // 6: 波表
            "audiosynth.gui.waveform.fm",            // 7: FM合成
            "audiosynth.gui.waveform.electricPiano", // 8: 电钢琴
            "audiosynth.gui.waveform.glockenspiel",  // 9: 钟琴
            "audiosynth.gui.waveform.supersaw",      // 10: 超锯齿
            "audiosynth.gui.waveform.tripleSaw"      // 11: 三重锯齿
    };

    public SoundGeneratorScreen(SoundGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 220; // 增加高度容纳分贝控件
    }

    @Override
    protected void init() {
        super.init();
        int left = leftPos;
        int top = topPos;

        // 第一列：频率、波形、调制频率、分贝
        frequencyField = new EditBox(font, left + 10, top + 30, 80, 20, Component.translatable("audiosynth.gui.frequency"));
        frequencyField.setValue(String.valueOf(menu.getBlockEntity().getFrequency()));
        addRenderableWidget(frequencyField);

        waveformIndex = menu.getBlockEntity().getWaveform();
        waveformButton = Button.builder(
                        Component.translatable(waveformKeys[waveformIndex]),
                        button -> {
                            waveformIndex = (waveformIndex + 1) % waveformKeys.length;
                            button.setMessage(Component.translatable(waveformKeys[waveformIndex]));
                        })
                .bounds(left + 10, top + 70, 80, 20)
                .tooltip(Tooltip.create(Component.translatable("audiosynth.gui.waveform.tooltip")))
                .build();
        addRenderableWidget(waveformButton);

        modFreqField = new EditBox(font, left + 10, top + 110, 80, 20, Component.translatable("audiosynth.gui.modFreq"));
        modFreqField.setValue(String.valueOf(menu.getBlockEntity().getModulationFrequency()));
        addRenderableWidget(modFreqField);

        volumeDbField = new EditBox(font, left + 10, top + 150, 80, 20, Component.translatable("audiosynth.gui.volumeDb"));
        volumeDbField.setValue(String.valueOf(menu.getBlockEntity().getVolumeDb()));
        addRenderableWidget(volumeDbField);

        // 第二列：混合因子、调制深度、范围、应用按钮
        mixField = new EditBox(font, left + 100, top + 30, 80, 20, Component.translatable("audiosynth.gui.mix"));
        mixField.setValue(String.valueOf(menu.getBlockEntity().getMixFactor()));
        addRenderableWidget(mixField);

        modDepthField = new EditBox(font, left + 100, top + 70, 80, 20, Component.translatable("audiosynth.gui.modDepth"));
        modDepthField.setValue(String.valueOf(menu.getBlockEntity().getModulationDepth()));
        addRenderableWidget(modDepthField);

        rangeField = new EditBox(font, left + 100, top + 110, 80, 20, Component.translatable("audiosynth.gui.range"));
        rangeField.setValue(String.valueOf(menu.getBlockEntity().getRange()));
        addRenderableWidget(rangeField);

        Button applyButton = Button.builder(
                        Component.translatable("audiosynth.gui.apply"),
                        button -> sendUpdateAndClose())
                .bounds(left + 100, top + 150, 80, 20)
                .build();
        addRenderableWidget(applyButton);
    }

    private void sendUpdateAndClose() {
        try {
            float freq = Float.parseFloat(frequencyField.getValue());
            int wave = waveformIndex;
            float mix = Float.parseFloat(mixField.getValue());
            float modDepth = Float.parseFloat(modDepthField.getValue());
            float modFreq = Float.parseFloat(modFreqField.getValue());
            int range = Integer.parseInt(rangeField.getValue());
            float volumeDb = Float.parseFloat(volumeDbField.getValue());

            ModNetworking.INSTANCE.sendToServer(new SoundSettingsPacket(
                    menu.getBlockEntity().getBlockPos(),
                    freq, wave, mix, modDepth, modFreq, range, volumeDb
            ));

            menu.updateSettings(freq, wave, mix, modDepth, modFreq, range, volumeDb);
            this.onClose();

            if (!FMLLoader.isProduction()) {
                System.out.println("[AudioSynth-DEBUG] GUI applied and closed");
            }
        } catch (NumberFormatException e) {
            // 忽略无效输入
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 不绘制默认库存文字
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int left = leftPos;
        int top = topPos;

        // 绘制所有标签
        guiGraphics.drawString(font, Component.translatable("audiosynth.gui.frequency"), left + 10, top + 15, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("audiosynth.gui.waveform"), left + 10, top + 55, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("audiosynth.gui.modFreq"), left + 10, top + 95, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("audiosynth.gui.volumeDb"), left + 10, top + 135, 0xFFFFFF);

        guiGraphics.drawString(font, Component.translatable("audiosynth.gui.mix"), left + 100, top + 15, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("audiosynth.gui.modDepth"), left + 100, top + 55, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("audiosynth.gui.range"), left + 100, top + 95, 0xFFFFFF);
    }
}