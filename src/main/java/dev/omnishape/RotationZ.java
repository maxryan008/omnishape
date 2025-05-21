package dev.omnishape;

import net.minecraft.util.StringRepresentable;

public enum RotationZ implements StringRepresentable {
    Z0(0), Z90(90), Z180(180), Z270(270);

    public final int degrees;

    RotationZ(int degrees) {
        this.degrees = degrees;
    }

    @Override
    public String getSerializedName() {
        return "z" + degrees;
    }
}