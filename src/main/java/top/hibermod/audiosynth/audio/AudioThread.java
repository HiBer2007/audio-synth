package top.hibermod.audiosynth.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import top.hibermod.audiosynth.block.entity.SoundGeneratorBlockEntity;
import top.hibermod.audiosynth.config.ClientConfig;

import javax.sound.sampled.*;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class AudioThread extends Thread {
    private final SoundGeneratorBlockEntity tile;
    private volatile boolean running = true;
    private SourceDataLine line;
    private final Random random = new Random();

    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE = 16;
    private static final int CHANNELS = 1;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, true, false);

    // 增大缓冲区到 100ms (SAMPLE_RATE / 10 = 4410 样本)
    private static final int BUFFER_MS = 100;
    private static final int SAMPLES_PER_BUFFER = SAMPLE_RATE / (1000 / BUFFER_MS); // 4410
    private static final int BYTES_PER_BUFFER = SAMPLES_PER_BUFFER * 2;

    public AudioThread(SoundGeneratorBlockEntity tile) {
        this.tile = tile;
    }

    private void debugLog(String message) {
        if (!FMLLoader.isProduction()) {
            System.out.println("[AudioSynth-DEBUG] " + message);
        }
    }

    @Override
    public void run() {
        String[] waveformNames = {"Sine", "Square", "Triangle", "Sawtooth", "Noise"};
        debugLog("Audio thread started for " + tile.getBlockPos());
        debugLog("Initial params: waveform=" + waveformNames[tile.getWaveform()] +
                ", freq=" + tile.getFrequency() +
                ", range=" + tile.getRange() +
                ", globalVolume=" + ClientConfig.GLOBAL_VOLUME.get());

        try {
            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT, BYTES_PER_BUFFER * 2); // 双缓冲区
            line.start();
            debugLog("Audio line opened successfully");
        } catch (LineUnavailableException e) {
            debugLog("Could not open audio line: " + e.getMessage());
            return;
        }

        byte[] buffer = new byte[BYTES_PER_BUFFER];
        float[] samples = new float[SAMPLES_PER_BUFFER];
        double phase = 0.0; // 相位累积

        while (running && tile.hasLevel()) {
            try {
                Player player = Minecraft.getInstance().player;
                if (player == null) {
                    Thread.sleep(10);
                    continue;
                }

                // 一次性读取所有参数，减少volatile访问
                float baseFreq = tile.getFrequency();
                int waveform = tile.getWaveform();
                float modDepth = tile.getModulationDepth();
                float modFreq = tile.getModulationFrequency();
                int range = tile.getRange();
                float volumeDb = tile.getVolumeDb();

                // 距离衰减
                Vec3 tilePos = new Vec3(
                        tile.getBlockPos().getX() + 0.5,
                        tile.getBlockPos().getY() + 0.5,
                        tile.getBlockPos().getZ() + 0.5
                );
                double dist = player.distanceToSqr(tilePos.x, tilePos.y, tilePos.z);
                double maxDist = range;
                double volumeFactor = Math.max(0, 1.0 - (Math.sqrt(dist) / maxDist));
                if (volumeFactor <= 0) {
                    volumeFactor = 0;
                }

                // 全局音量与主音量
                float globalVol = ClientConfig.GLOBAL_VOLUME.get().floatValue();
                float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
                float volumeLinear = (float) Math.pow(10.0, volumeDb / 20.0);
                float finalVolume = (float) (volumeFactor * globalVol * masterVolume * volumeLinear);

                // 生成样本（传入相位，以便连续）
                phase = generateSamples(samples, phase, baseFreq, waveform, modDepth, modFreq);

                // 写入音频线
                for (int i = 0; i < samples.length; i++) {
                    short val = (short) (samples[i] * finalVolume * Short.MAX_VALUE);
                    buffer[i * 2] = (byte) (val & 0xFF);
                    buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
                }
                line.write(buffer, 0, buffer.length);

            } catch (InterruptedException e) {
                debugLog("Audio thread interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                debugLog("Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (line != null) {
            line.drain();
            line.stop();
            line.close();
            debugLog("Audio line closed");
        }
        debugLog("Audio thread stopped for " + tile.getBlockPos());
    }

    /**
     * 生成样本并返回更新后的相位
     */
    private double generateSamples(float[] samples, double phase, float baseFreq, int waveform,
                                   float modDepth, float modFreq) {
        double timeStep = 1.0 / SAMPLE_RATE;
        boolean useModulation = modDepth > 0.0001f; // 避免不必要的计算

        for (int i = 0; i < samples.length; i++) {
            double t = i * timeStep;
            double instantFreq = baseFreq;

            if (useModulation) {
                double fm = modDepth * Math.sin(2 * Math.PI * modFreq * t);
                instantFreq = baseFreq * (1 + fm);
            }

            phase += instantFreq * timeStep;
            double phaseNorm = phase % 1.0;

            float sample;
            switch (waveform) {
                case 0 -> sample = (float) Math.sin(2 * Math.PI * phaseNorm);
                case 1 -> sample = phaseNorm < 0.5 ? 1.0f : -1.0f;
                case 2 -> sample = (float) (2 * Math.abs(2 * phaseNorm - 1) - 1);
                case 3 -> sample = (float) (2 * phaseNorm - 1);
                case 4 -> sample = random.nextFloat() * 2 - 1;
                default -> sample = 0;
            }
            samples[i] = sample;
        }
        return phase;
    }

    public void stopPlaying() {
        debugLog("stopPlaying() called");
        running = false;
        if (line != null) {
            line.stop();
            line.close();
        }
    }
}