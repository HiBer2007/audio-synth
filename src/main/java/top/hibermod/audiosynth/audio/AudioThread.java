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

    // 缓冲区大小：100ms (SAMPLE_RATE / 10 = 4410 样本)
    private static final int BUFFER_MS = 100;
    private static final int SAMPLES_PER_BUFFER = SAMPLE_RATE / (1000 / BUFFER_MS); // 4410
    private static final int BYTES_PER_BUFFER = SAMPLES_PER_BUFFER * 2;

    // 波表 (64点正弦波表，线性插值)
    private static final int WAVETABLE_SIZE = 64;
    private static final float[] WAVETABLE = new float[WAVETABLE_SIZE];
    static {
        for (int i = 0; i < WAVETABLE_SIZE; i++) {
            double phase = (double) i / WAVETABLE_SIZE;
            WAVETABLE[i] = (float) Math.sin(2 * Math.PI * phase);
        }
    }

    // 超锯齿波的失谐系数（音分转频率因子）
    private static final double[] SUPERSAW_DETUNE = {
            Math.pow(2.0, -5.0 / 1200.0),
            Math.pow(2.0, -3.0 / 1200.0),
            Math.pow(2.0, -1.0 / 1200.0),
            Math.pow(2.0,  0.0 / 1200.0),
            Math.pow(2.0,  1.0 / 1200.0),
            Math.pow(2.0,  3.0 / 1200.0),
            Math.pow(2.0,  5.0 / 1200.0)
    };

    // 三重锯齿波的失谐系数
    private static final double[] TRIPLE_SAW_DETUNE = {
            Math.pow(2.0, -2.0 / 1200.0),
            Math.pow(2.0,  0.0 / 1200.0),
            Math.pow(2.0,  2.0 / 1200.0)
    };

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
        String[] waveformNames = {"Sine", "Square", "Triangle", "Sawtooth", "Noise",
                "Rectangle", "Wavetable", "FM", "Electric Piano",
                "Glockenspiel", "Supersaw", "Triple Saw"};
        debugLog("Audio thread started for " + tile.getBlockPos());
        debugLog("Initial params: waveform=" + waveformNames[tile.getWaveform()] +
                ", freq=" + tile.getFrequency() +
                ", range=" + tile.getRange() +
                ", volumeDb=" + tile.getVolumeDb() +
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
        double phase = 0.0;               // 累积相位（用于需要连续相位的波形）
        double timeStep = 1.0 / SAMPLE_RATE;
        double startTime = 0.0;            // 本批样本的起始绝对时间

        while (running && tile.hasLevel()) {
            try {
                Player player = Minecraft.getInstance().player;
                if (player == null) {
                    Thread.sleep(10);
                    continue;
                }

                // 一次性读取所有参数
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

                // 全局音量、主音量、分贝转线性
                float globalVol = ClientConfig.GLOBAL_VOLUME.get().floatValue();
                float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
                float volumeLinear = (float) Math.pow(10.0, volumeDb / 20.0);
                float finalVolume = (float) (volumeFactor * globalVol * masterVolume * volumeLinear);

                // 生成样本
                phase = generateSamples(samples, phase, startTime, baseFreq, waveform, modDepth, modFreq);
                startTime += SAMPLES_PER_BUFFER * timeStep;

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
     * @param samples    输出样本数组
     * @param phase      当前累积相位（用于需要相位累积的波形）
     * @param startTime  本批样本开始时的绝对时间（秒）
     * @param baseFreq   基频
     * @param waveform   波形类型（0-11）
     * @param modDepth   调制深度
     * @param modFreq    调制频率
     * @return 更新后的相位
     */
    private double generateSamples(float[] samples, double phase, double startTime, float baseFreq, int waveform,
                                   float modDepth, float modFreq) {
        double timeStep = 1.0 / SAMPLE_RATE;
        boolean useModulation = modDepth > 0.0001f && waveform != 7; // 波形7已自行处理调制

        for (int i = 0; i < samples.length; i++) {
            double t = startTime + i * timeStep;
            double instantFreq = baseFreq;

            if (useModulation) {
                double fm = modDepth * Math.sin(2 * Math.PI * modFreq * t);
                instantFreq = baseFreq * (1 + fm);
            }

            phase += instantFreq * timeStep;
            double phaseNorm = phase % 1.0;

            float sample = 0f;
            switch (waveform) {
                // 原始波形
                case 0: // 正弦
                    sample = (float) Math.sin(2 * Math.PI * phaseNorm);
                    break;
                case 1: // 方波 (50%占空比)
                    sample = phaseNorm < 0.5 ? 1.0f : -1.0f;
                    break;
                case 2: // 三角波
                    sample = (float) (2 * Math.abs(2 * phaseNorm - 1) - 1);
                    break;
                case 3: // 锯齿波
                    sample = (float) (2 * phaseNorm - 1);
                    break;
                case 4: // 白噪声
                    sample = random.nextFloat() * 2 - 1;
                    break;
                case 5: // 矩形波 (占空比25%)
                    sample = phaseNorm < 0.25 ? 1.0f : -1.0f;
                    break;
                case 6: { // 波表 (64点正弦波表，线性插值)
                    double tablePos = phaseNorm * WAVETABLE_SIZE;
                    int idx1 = (int) Math.floor(tablePos) % WAVETABLE_SIZE;
                    int idx2 = (idx1 + 1) % WAVETABLE_SIZE;
                    double frac = tablePos - idx1;
                    sample = (float) (WAVETABLE[idx1] * (1 - frac) + WAVETABLE[idx2] * frac);
                    break;
                }
                case 7: { // FM合成: 两个正弦波，载波频率为基频，调制波频率为基频
                    double modPhase = 2 * Math.PI * baseFreq * t;
                    double modSignal = Math.sin(modPhase);
                    double carrierPhase = 2 * Math.PI * baseFreq * t + modDepth * modSignal;
                    sample = (float) Math.sin(carrierPhase);
                    break;
                }
                // 新增波形（基于绝对时间t，不依赖累积相位）
                case 8: { // 电钢琴: FM合成，调制波频率 = 4*baseFreq
                    double modSignal = Math.sin(2 * Math.PI * 4 * baseFreq * t);
                    double carrierPhase = 2 * Math.PI * baseFreq * t + modDepth * modSignal;
                    sample = (float) Math.sin(carrierPhase);
                    break;
                }
                case 9: { // 钟琴: 两个正弦波混合，基频和基频的5.6倍
                    double freq1 = baseFreq;
                    double freq2 = baseFreq * 5.6;
                    double amp1 = 0.7;
                    double amp2 = 0.3;
                    double val1 = amp1 * Math.sin(2 * Math.PI * freq1 * t);
                    double val2 = amp2 * Math.sin(2 * Math.PI * freq2 * t);
                    sample = (float) (val1 + val2);
                    break;
                }
                case 10: { // 超锯齿: 7个失谐锯齿波叠加
                    double sum = 0;
                    for (double factor : SUPERSAW_DETUNE) {
                        double sawPhase = (baseFreq * factor * t) % 1.0;
                        double saw = 2 * sawPhase - 1;
                        sum += saw;
                    }
                    sample = (float) (sum / SUPERSAW_DETUNE.length);
                    break;
                }
                case 11: { // 三重锯齿: 3个失谐锯齿波叠加
                    double sum = 0;
                    for (double factor : TRIPLE_SAW_DETUNE) {
                        double sawPhase = (baseFreq * factor * t) % 1.0;
                        double saw = 2 * sawPhase - 1;
                        sum += saw;
                    }
                    sample = (float) (sum / TRIPLE_SAW_DETUNE.length);
                    break;
                }
                default:
                    sample = 0;
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