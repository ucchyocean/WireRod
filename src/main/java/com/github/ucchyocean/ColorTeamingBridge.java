/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.github.ucchyocean.ct.ColorTeaming;

/**
 * ColorTeaming連携クラス
 * 
 * @author ucchy
 */
public class ColorTeamingBridge {

    private ColorTeaming colorteaming;

    public ColorTeamingBridge(Plugin colorteaming) {
        this.colorteaming = (ColorTeaming)colorteaming;
    }

    public void registerItem(ItemStack item, String name, String displayName) {
        colorteaming.getAPI().registerCustomItem(item, name, displayName);
    }
}
