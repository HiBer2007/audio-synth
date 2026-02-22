package top.hibermod.audiosynth.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = top.hibermod.audiosynth.AudioSynth.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue GLOBAL_VOLUME;

    static {
        BUILDER.push("Audio Settings");
        GLOBAL_VOLUME = BUILDER
                .comment("Global volume multiplier for all sound generator blocks (0.0 - 2.0)")
                .defineInRange("globalVolume", 1.0, 0.0, 2.0);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}