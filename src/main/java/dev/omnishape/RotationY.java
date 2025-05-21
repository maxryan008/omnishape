package dev.omnishape;

import net.minecraft.util.StringRepresentable;

public enum RotationY implements StringRepresentable {
    Y0(0), Y90(90), Y180(180), Y270(270);

    public final int degrees;

    RotationY(int degrees) {
        this.degrees = degrees;
    }

    @Override
    public String getSerializedName() {
        return "y" + degrees;
    }
}
