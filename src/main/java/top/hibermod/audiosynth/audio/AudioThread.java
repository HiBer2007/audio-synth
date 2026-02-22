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
        // 输出启动信息：波形、频率、音量乘数等
        String[] waveformNames = {"Sine", "Square", "Triangle", "Sawtooth", "Noise"};
        debugLog("Audio thread started for " + tile.getBlockPos());
        debugLog("Initial params: waveform=" + waveformNames[tile.getWaveform()] +
                ", freq=" + tile.getFrequency() +
                ", range=" + tile.getRange() +
                ", globalVolume=" + ClientConfig.GLOBAL_VOLUME.get());

        try {
            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT, SAMPLE_RATE * 2);
            line.start();
            debugLog("Audio line opened successfully");
        } catch (LineUnavailableException e) {
            debugLog("Could not open audio line: " + e.getMessage());
            return;
        }

        int samplesPerBuffer = SAMPLE_RATE / 20;
        byte[] buffer = new byte[samplesPerBuffer * 2];
        float[] samples = new float[samplesPerBuffer];

        while (running && tile.hasLevel()) {
            try {
                Player player = Minecraft.getInstance().player;
                if (player == null) {
                    Thread.sleep(50);
                    continue;
                }

                // 从 TileEntity 读取参数
                float baseFreq = tile.getFrequency();
                int waveform = tile.getWaveform();
                float modDepth = tile.getModulationDepth();
                float modFreq = tile.getModulationFrequency();
                int range = tile.getRange();

                // 从客户端配置读取全局音量
                float globalVol = ClientConfig.GLOBAL_VOLUME.get().floatValue();

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

                float volumeLinear = (float) Math.pow(10.0, tile.getVolumeDb() / 20.0);
                float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
                float finalVolume = (float) (volumeFactor * globalVol * masterVolume * volumeLinear);

                generateSamples(samples, baseFreq, waveform, modDepth, modFreq);

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

    private void generateSamples(float[] samples, float baseFreq, int waveform,
                                 float modDepth, float modFreq) {
        double timeStep = 1.0 / SAMPLE_RATE;
        double phase = 0.0;

        for (int i = 0; i < samples.length; i++) {
            double t = i * timeStep;

            double fm = modDepth * Math.sin(2 * Math.PI * modFreq * t);
            double instantFreq = baseFreq * (1 + fm);
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