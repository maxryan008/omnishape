package dev.omnishape;

import net.minecraft.util.StringRepresentable;

public enum RotationX implements StringRepresentable {
    X0(0), X90(90), X180(180), X270(270);

    public final int degrees;

    RotationX(int degrees) {
        this.degrees = degrees;
    }

    @Override
    public String getSerializedName() {
        return "x" + degrees;
    }
}
