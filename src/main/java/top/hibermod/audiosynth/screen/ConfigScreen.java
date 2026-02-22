package top.hibermod.audiosynth.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import top.hibermod.audiosynth.config.ClientConfig;

import java.text.DecimalFormat;

@OnlyIn(Dist.CLIENT)
public class ConfigScreen extends Screen {
    private final Screen lastScreen;
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");
    private AbstractSliderButton volumeSlider;

    public ConfigScreen(Screen lastScreen) {
        super(Component.translatable("audiosynth.config.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 全局音量滑块 (范围 0.0 - 2.0，滑块内部 0.0 - 1.0)
        double currentVolume = ClientConfig.GLOBAL_VOLUME.get();
        volumeSlider = new AbstractSliderButton(centerX - 100, centerY - 20, 200, 20,
                Component.translatable("audiosynth.config.globalVolume", FORMAT.format(currentVolume)), currentVolume / 2.0) {
            @Override
            protected void updateMessage() {
                double displayValue = this.value * 2.0;
                setMessage(Component.translatable("audiosynth.config.globalVolume", FORMAT.format(displayValue)));
            }

            @Override
            protected void applyValue() {
                // 实时保存
                double newValue = this.value * 2.0;
                ClientConfig.GLOBAL_VOLUME.set(newValue);
                ClientConfig.SPEC.save();
                if (!FMLLoader.isProduction()) {
                    System.out.println("[AudioSynth-DEBUG] Global volume saved: " + newValue);
                }
            }
        };
        addRenderableWidget(volumeSlider);

        // 完成按钮
        Button doneButton = Button.builder(
                        Component.translatable("gui.done"),
                        button -> this.onClose())
                .bounds(centerX - 50, centerY + 20, 100, 20)
                .build();
        addRenderableWidget(doneButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);
    }
}