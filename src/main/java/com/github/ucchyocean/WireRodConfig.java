/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package com.github.ucchyocean;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * WireRodのコンフィグクラス
 * @author ucchy
 */
public class WireRodConfig {

    private WireRod parent;

    private int defaultLevel;
    private int wireRange;
    private double distanceBonusRatio;
    private boolean protectFallDamage;
    private boolean enableCraft;

    /**
     * コンストラクタ
     * @param parent
     */
    public WireRodConfig(WireRod parent) {
        this.parent = parent;
        reloadConfig();
    }

    /**
     * コンフィグを読み込む
     */
    protected void reloadConfig() {
        
        parent.saveDefaultConfig();
        parent.reloadConfig();

        FileConfiguration conf = parent.getConfig();

        defaultLevel = Math.max(1, Math.min(WireRodUtil.MAX_LEVEL, conf.getInt("defaultLevel", 4)));
        wireRange = conf.getInt("wireRange", 30);
        distanceBonusRatio = conf.getDouble("distanceBonusRatio", 1.0);
        protectFallDamage = conf.getBoolean("protectFallDamage", true);
        enableCraft = conf.getBoolean("enableCraft", true);
    }

    /**
     * @return defaultLevel
     */
    public int getDefaultLevel() {
        return defaultLevel;
    }

    /**
     * @return wireRange
     */
    public int getWireRange() {
        return wireRange;
    }

    /**
     * @return distanceBonusRatio
     */
    public double getDistanceBonusRatio() {
        return distanceBonusRatio;
    }

    /**
     * @return protectFallDamage
     */
    public boolean isProtectFallDamage() {
        return protectFallDamage;
    }

    /**
     * @return enableCraft
     */
    public boolean isEnableCraft() {
        return enableCraft;
    }
}
