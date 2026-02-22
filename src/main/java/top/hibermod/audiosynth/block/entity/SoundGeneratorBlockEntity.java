package top.hibermod.audiosynth.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.hibermod.audiosynth.AudioSynth;
import top.hibermod.audiosynth.audio.AudioThread;
import top.hibermod.audiosynth.screen.SoundGeneratorMenu;

public class SoundGeneratorBlockEntity extends BlockEntity implements MenuProvider {
    // 音频参数
    private float frequency = 440.0f;
    private int waveform = 0; // 0:正弦 1:方波 2:三角 3:锯齿 4:噪波
    private float mixFactor = 0.5f;
    private float modulationDepth = 0.0f;
    private float modulationFrequency = 5.0f;
    private int range = 20;
    private float volumeDb = 0.0f; // 分贝值，默认0dB

    private boolean powered = false;

    // 客户端音频线程
    private transient Object audioThread;

    // 物品栏（未使用）
    private final ItemStackHandler itemHandler = new ItemStackHandler(0);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    // DEBUG 日志
    private void debugLog(String message) {
        if (!FMLLoader.isProduction()) {
            System.out.println("[AudioSynth-DEBUG] " + message);
        }
    }

    public SoundGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(AudioSynth.SOUND_GENERATOR_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.audiosynth.sound_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SoundGeneratorMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("frequency", frequency);
        tag.putInt("waveform", waveform);
        tag.putFloat("mixFactor", mixFactor);
        tag.putFloat("modDepth", modulationDepth);
        tag.putFloat("modFreq", modulationFrequency);
        tag.putInt("range", range);
        tag.putFloat("volumeDb", volumeDb);
        tag.putBoolean("powered", powered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        frequency = tag.getFloat("frequency");
        waveform = tag.getInt("waveform");
        mixFactor = tag.getFloat("mixFactor");
        modulationDepth = tag.getFloat("modDepth");
        modulationFrequency = tag.getFloat("modFreq");
        range = tag.getInt("range");
        volumeDb = tag.getFloat("volumeDb");
        boolean oldPowered = powered;
        powered = tag.getBoolean("powered");

        debugLog("TileEntity loaded: powered=" + powered + " at " + worldPosition);

        if (level != null && level.isClientSide) {
            if (powered) {
                startAudio();
            } else {
                stopAudio();
            }
        }
    }

    public void setRedstonePowered(boolean powered) {
        if (this.powered == powered) return;
        this.powered = powered;
        setChanged();
        debugLog("Redstone changed: " + powered + " at " + worldPosition);

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        if (level != null && level.isClientSide) {
            if (powered) {
                startAudio();
            } else {
                stopAudio();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void startAudio() {
        if (audioThread == null) {
            debugLog("Starting audio thread");
            audioThread = new AudioThread(this);
            ((AudioThread) audioThread).start();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void stopAudio() {
        if (audioThread != null) {
            debugLog("Stopping audio thread");
            ((AudioThread) audioThread).stopPlaying();
            audioThread = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            stopAudio();
        }
        lazyItemHandler.invalidate();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && level.isClientSide) {
            stopAudio();
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            boolean oldPowered = powered;
            load(tag);
            debugLog("Received update packet: powered=" + powered + " (was " + oldPowered + ")");
        }
    }

    // 访问器
    public float getFrequency() { return frequency; }
    public void setFrequency(float f) {
        this.frequency = f;
        setChanged();
        syncToClient();
        debugLog("Frequency set to " + f);
    }

    public int getWaveform() { return waveform; }
    public void setWaveform(int w) {
        this.waveform = w;
        setChanged();
        syncToClient();
        debugLog("Waveform set to " + w);
    }

    public float getMixFactor() { return mixFactor; }
    public void setMixFactor(float m) {
        this.mixFactor = m;
        setChanged();
        syncToClient();
        debugLog("Mix factor set to " + m);
    }

    public float getModulationDepth() { return modulationDepth; }
    public void setModulationDepth(float d) {
        this.modulationDepth = d;
        setChanged();
        syncToClient();
        debugLog("Mod depth set to " + d);
    }

    public float getModulationFrequency() { return modulationFrequency; }
    public void setModulationFrequency(float f) {
        this.modulationFrequency = f;
        setChanged();
        syncToClient();
        debugLog("Mod freq set to " + f);
    }

    public int getRange() { return range; }
    public void setRange(int r) {
        this.range = r;
        setChanged();
        syncToClient();
        debugLog("Range set to " + r);
    }

    public float getVolumeDb() { return volumeDb; }
    public void setVolumeDb(float v) {
        this.volumeDb = v;
        setChanged();
        syncToClient();
        debugLog("Volume dB set to " + v);
    }

    public boolean isPowered() { return powered; }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}