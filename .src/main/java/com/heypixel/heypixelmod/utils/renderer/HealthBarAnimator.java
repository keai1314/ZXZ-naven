package com.heypixel.heypixelmod.utils.renderer;

public class HealthBarAnimator {
    private float displayedHealth;
    private long lastUpdateTime;
    private final float animationSpeed;

    public HealthBarAnimator(float initialHealth, float animationSpeed) {
        this.displayedHealth = initialHealth;
        this.lastUpdateTime = System.currentTimeMillis();
        this.animationSpeed = animationSpeed;
    }

    public void update(float targetHealth) {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - this.lastUpdateTime;
        this.lastUpdateTime = currentTime;

        if (Math.abs(targetHealth - this.displayedHealth) < 0.1f) {
            this.displayedHealth = targetHealth;
            return;
        }

        float change = (targetHealth - this.displayedHealth) * (deltaTime / 1000.0f) * this.animationSpeed;
        this.displayedHealth += change;

        if (targetHealth > this.displayedHealth && change < 0) {
            this.displayedHealth = targetHealth;
        } else if (targetHealth < this.displayedHealth && change > 0) {
            this.displayedHealth = targetHealth;
        }
    }

    public void reset(float newHealth) {
        this.displayedHealth = newHealth;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public float getDisplayedHealth() {
        return displayedHealth;
    }
}