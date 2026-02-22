package top.hibermod.audiosynth;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import top.hibermod.audiosynth.block.SoundGeneratorBlock;
import top.hibermod.audiosynth.block.entity.SoundGeneratorBlockEntity;
import top.hibermod.audiosynth.config.ClientConfig;
import top.hibermod.audiosynth.network.ModNetworking;
import top.hibermod.audiosynth.screen.SoundGeneratorMenu;

import com.google.gson.JsonObject;

@Mod(AudioSynth.MODID)
public class AudioSynth {
    public static final String MODID = "audiosynth";
    private static final Logger LOGGER = LogUtils.getLogger();

    // 版本检测枚举
    public enum CreateVersion {
        NONE,           // 未安装
        LOW_OR_NONE,    // 版本低于0.5或未安装（用于无Create时的配方）
        MID,            // 0.5 <= 版本 < 6.0
        HIGH            // 版本 >= 6.0
    }

    // 静态检测结果，在类加载时执行，确保配方加载前有值
    public static final CreateVersion CREATE_VERSION = detectCreateVersion();

    private static CreateVersion detectCreateVersion() {
        var modContainer = ModList.get().getModContainerById("create");
        if (modContainer.isEmpty()) {
            return CreateVersion.NONE;
        }
        String versionString = modContainer.get().getModInfo().getVersion().toString();
        // 简单的版本比较，处理 "0.5.1", "6.0.0" 等情况
        if (versionString.startsWith("6.") || versionString.compareTo("6.0") >= 0) {
            return CreateVersion.HIGH;
        } else if (versionString.startsWith("0.5") || versionString.compareTo("0.5") >= 0) {
            return CreateVersion.MID;
        } else {
            return CreateVersion.LOW_OR_NONE;
        }
    }

    // 注册表
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    // 方块与物品
    public static final RegistryObject<Block> SOUND_GENERATOR_BLOCK = BLOCKS.register("sound_generator", SoundGeneratorBlock::new);
    public static final RegistryObject<Item> SOUND_GENERATOR_ITEM = ITEMS.register("sound_generator",
            () -> new BlockItem(SOUND_GENERATOR_BLOCK.get(), new Item.Properties()));

    // 方块实体
    public static final RegistryObject<BlockEntityType<SoundGeneratorBlockEntity>> SOUND_GENERATOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("sound_generator",
                    () -> BlockEntityType.Builder.of(SoundGeneratorBlockEntity::new, SOUND_GENERATOR_BLOCK.get()).build(null));

    // 菜单类型
    public static final RegistryObject<MenuType<SoundGeneratorMenu>> SOUND_GENERATOR_MENU =
            MENUS.register("sound_generator", () -> IForgeMenuType.create((windowId, inv, data) -> {
                return new SoundGeneratorMenu(windowId, inv, data.readBlockPos());
            }));

    public AudioSynth() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);

        // 注册客户端配置（配置文件本身不依赖客户端类）
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "audiosynth-client.toml");

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModNetworking.register();
        // 注册自定义配方条件
        CraftingHelper.register(CheckCreateVersionCondition.Serializer.INSTANCE);
        LOGGER.info("AudioSynth common setup completed. Detected Create version: {}", CREATE_VERSION);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(SOUND_GENERATOR_ITEM.get());
        }
    }

    // 自定义配方条件：检查Create模组版本
    public static class CheckCreateVersionCondition implements ICondition {
        public static final ResourceLocation ID = new ResourceLocation(AudioSynth.MODID, "check_create_version");
        private final CreateVersion requiredVersion;

        public CheckCreateVersionCondition(CreateVersion requiredVersion) {
            this.requiredVersion = requiredVersion;
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }

        @Override
        public boolean test(IContext context) {
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
                return new CheckCreateVersionCondition(CreateVersion.valueOf(versionName));
            }

            @Override
            public ResourceLocation getID() {
                return CheckCreateVersionCondition.ID;
            }
        }
    }
}