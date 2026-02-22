package top.hibermod.audiosynth.datagen;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import top.hibermod.audiosynth.AudioSynth;

public class CheckCreateVersionCondition implements ICondition {
    public static final ResourceLocation ID = new ResourceLocation(AudioSynth.MODID, "check_create_version");
    private final AudioSynth.CreateVersion requiredVersion;

    public CheckCreateVersionCondition(AudioSynth.CreateVersion requiredVersion) {
        this.requiredVersion = requiredVersion;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        // 核心逻辑：比较实际版本与要求的版本
        return AudioSynth.CREATE_VERSION == requiredVersion;
    }

    public static class Serializer implements IConditionSerializer<CheckCreateVersionCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, CheckCreateVersionCondition value) {
            json.addProperty("required_version", value.requiredVersion.name());
        }

        @Override
        public CheckCreateVersionCondition read(JsonObject json) {
            String versionName = json.get("required_version").getAsString();
            return new CheckCreateVersionCondition(AudioSynth.CreateVersion.valueOf(versionName));
        }

        @Override
        public ResourceLocation getID() {
            return CheckCreateVersionCondition.ID;
        }
    }
}