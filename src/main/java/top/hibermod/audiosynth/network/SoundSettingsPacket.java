package top.hibermod.audiosynth.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;
import top.hibermod.audiosynth.block.entity.SoundGeneratorBlockEntity;

import java.util.function.Supplier;

public class SoundSettingsPacket {
    private final BlockPos pos;
    private final float frequency;
    private final int waveform;
    private final float mixFactor;
    private final float modDepth;
    private final float modFreq;
    private final int range;
    private final float volumeDb;

    public SoundSettingsPacket(BlockPos pos, float freq, int wave, float mix, float modDepth, float modFreq, int range, float volumeDb) {
        this.pos = pos;
        this.frequency = freq;
        this.waveform = wave;
        this.mixFactor = mix;
        this.modDepth = modDepth;
        this.modFreq = modFreq;
        this.range = range;
        this.volumeDb = volumeDb;
    }

    public static void encode(SoundSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeFloat(msg.frequency);
        buf.writeInt(msg.waveform);
        buf.writeFloat(msg.mixFactor);
        buf.writeFloat(msg.modDepth);
        buf.writeFloat(msg.modFreq);
        buf.writeInt(msg.range);
        buf.writeFloat(msg.volumeDb);
    }

    public static SoundSettingsPacket decode(FriendlyByteBuf buf) {
        return new SoundSettingsPacket(
                buf.readBlockPos(),
                buf.readFloat(),
                buf.readInt(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readInt(),
                buf.readFloat()
        );
    }

    public static void handle(SoundSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level().hasChunkAt(msg.pos)) {
                BlockEntity be = player.level().getBlockEntity(msg.pos);
                if (be instanceof SoundGeneratorBlockEntity tile) {
                    tile.setFrequency(msg.frequency);
                    tile.setWaveform(msg.waveform);
                    tile.setMixFactor(msg.mixFactor);
                    tile.setModulationDepth(msg.modDepth);
                    tile.setModulationFrequency(msg.modFreq);
                    tile.setRange(msg.range);
                    tile.setVolumeDb(msg.volumeDb);

                    if (!FMLLoader.isProduction()) {
                        System.out.println("[AudioSynth-DEBUG] Received settings update for " + msg.pos);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}