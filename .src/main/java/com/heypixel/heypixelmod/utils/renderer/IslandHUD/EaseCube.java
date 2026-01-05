package com.heypixel.heypixelmod.utils.renderer.IslandHUD;

public class EaseCube implements Easing {
    @Override
    public double apply(double t) {
        t = clamp01(t);

        double y = 1.0 - t;
        return y * y * y;
    }
}