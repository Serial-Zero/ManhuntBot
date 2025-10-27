package com.manhuntbot;

public enum HunterDifficulty {
    CALM(0.0D, 1.0D, 120),
    EASY(0.20D, 4.0D, 60),
    MEDIUM(0.30D, 6.0D, 40),
    HARD(0.42D, 8.0D, 20);

    private final double moveSpeed;
    private final double attackDamage;
    private final int pathIntervalTicks;

    HunterDifficulty(double moveSpeed, double attackDamage, int pathIntervalTicks) {
        this.moveSpeed = moveSpeed;
        this.attackDamage = attackDamage;
        this.pathIntervalTicks = pathIntervalTicks;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public int getPathIntervalTicks() {
        return pathIntervalTicks;
    }

    public String displayName() {
        return name().toLowerCase();
    }
}
