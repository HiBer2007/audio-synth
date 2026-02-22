package top.hibermod.audiosynth;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.hibermod.audiosynth.screen.ConfigScreen;
import top.hibermod.audiosynth.screen.SoundGeneratorMenu;
import top.hibermod.audiosynth.screen.SoundGeneratorScreen;

@Mod.EventBusSubscriber(modid = AudioSynth.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注册屏幕
        MenuScreens.register(AudioSynth.SOUND_GENERATOR_MENU.get(), SoundGeneratorScreen::new);

        // 注册配置 GUI
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new ConfigScreen(screen)));
    }
}