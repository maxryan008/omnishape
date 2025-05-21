package dev.omnishape;

import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;

public class BlockRotationProperty extends Property<BlockRotation> {
    private final List<BlockRotation> values;

    public BlockRotationProperty(String name) {
        super(name, BlockRotation.class);
        this.values = generateDiscreteRotations();
    }

    private List<BlockRotation> generateDiscreteRotations() {
        List<BlockRotation> list = new ArrayList<>();
        for (int pitch = 0; pitch < 360; pitch += 90)
            for (int yaw = 0; yaw < 360; yaw += 90)
                for (int roll = 0; roll < 360; roll += 90)
                    list.add(new BlockRotation(pitch, yaw, roll));
        return Collections.unmodifiableList(list);
    }

    @Override
    public List<BlockRotation> getPossibleValues() {
        return values;
    }

    @Override
    public String getName(BlockRotation value) {
        return "p%d_y%d_r%d".formatted(value.pitch, value.yaw, value.roll);
    }

    @Override
    public Optional<BlockRotation> getValue(String name) {
        for (BlockRotation val : values) {
            if (getName(val).equals(name)) return Optional.of(val);
        }
        return Optional.empty();
    }
}