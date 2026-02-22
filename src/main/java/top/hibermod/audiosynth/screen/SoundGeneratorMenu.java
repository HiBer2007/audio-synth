package top.hibermod.audiosynth.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import top.hibermod.audiosynth.AudioSynth;
import top.hibermod.audiosynth.block.entity.SoundGeneratorBlockEntity;

public class SoundGeneratorMenu extends AbstractContainerMenu {
    private final SoundGeneratorBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    private final DataSlot frequencyData = DataSlot.standalone();
    private final DataSlot waveformData = DataSlot.standalone();
    private final DataSlot mixFactorData = DataSlot.standalone();
    private final DataSlot modDepthData = DataSlot.standalone();
    private final DataSlot modFreqData = DataSlot.standalone();
    private final DataSlot rangeData = DataSlot.standalone();
    private final DataSlot volumeDbData = DataSlot.standalone();

    public SoundGeneratorMenu(int id, Inventory inv, BlockEntity entity) {
        super(AudioSynth.SOUND_GENERATOR_MENU.get(), id);
        this.blockEntity = (SoundGeneratorBlockEntity) entity;
        this.levelAccess = ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());

        this.addDataSlot(frequencyData);
        this.addDataSlot(waveformData);
        this.addDataSlot(mixFactorData);
        this.addDataSlot(modDepthData);
        this.addDataSlot(modFreqData);
        this.addDataSlot(rangeData);
        this.addDataSlot(volumeDbData);

        frequencyData.set(Float.floatToIntBits(blockEntity.getFrequency()));
        waveformData.set(blockEntity.getWaveform());
        mixFactorData.set(Float.floatToIntBits(blockEntity.getMixFactor()));
        modDepthData.set(Float.floatToIntBits(blockEntity.getModulationDepth()));
        modFreqData.set(Float.floatToIntBits(blockEntity.getModulationFrequency()));
        rangeData.set(blockEntity.getRange());
        volumeDbData.set(Float.floatToIntBits(blockEntity.getVolumeDb()));
    }

    public SoundGeneratorMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, AudioSynth.SOUND_GENERATOR_BLOCK.get());
    }

    public SoundGeneratorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public void updateSettings(float freq, int wave, float mix, float modDepth, float modFreq, int range, float volumeDb) {
        blockEntity.setFrequency(freq);
        blockEntity.setWaveform(wave);
        blockEntity.setMixFactor(mix);
        blockEntity.setModulationDepth(modDepth);
        blockEntity.setModulationFrequency(modFreq);
        blockEntity.setRange(range);
        blockEntity.setVolumeDb(volumeDb);
    }
}