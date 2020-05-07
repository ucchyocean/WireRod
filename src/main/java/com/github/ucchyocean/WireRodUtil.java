package com.github.ucchyocean;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

final class WireRodUtil {

    static final String DISPLAY_NAME = "wirerod";
    static final int MAX_LEVEL = 20;

    private static boolean isWireRodRecipeSet = false;

    /**
     * ワイヤーロッドのレシピを登録する
     */
    static void addRecipe() {
        Recipe recipe = Compatibles.createWireRodRecipe(WireRod.getInstance().getWireRodConfig().getDefaultLevel());
        Bukkit.addRecipe(recipe);
        isWireRodRecipeSet = true;
    }

    /**
     * ワイヤーロッドのレシピを解除する
     */
    static void removeRecipe() {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            if (isWireRod(it.next().getResult())) {
                it.remove();
                isWireRodRecipeSet = false;
                return;
            }
        }
    }

    /**
     * ワイヤーロッドのレシピが登録されているかどうかを調べる。
     *
     * @return ワイヤーロッドのレシピが登録されているかどうか
     */
    static boolean isWireRodRecipeSet() {
        return isWireRodRecipeSet;
    }

    /**
     * 指定したレベルのWirerodを取得する
     *
     * @param level レベル
     */
    static ItemStack getWireRod(int level) {

        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        meta.setDisplayName(DISPLAY_NAME);
        rod.setItemMeta(meta);
        rod.addUnsafeEnchantment(Enchantment.OXYGEN, Math.max(1, Math.min(MAX_LEVEL, level)));

        return rod;
    }

    /**
     * 渡したアイテムがワイヤーロッドか調べる。
     *
     * @param rod アイテム
     * @return rodがワイヤーロッドならtrue
     */
    static boolean isWireRod(ItemStack rod) {
        return rod != null
                && rod.getType() == Material.FISHING_ROD
                && rod.hasItemMeta()
                && rod.getItemMeta().hasDisplayName()
                && rod.getItemMeta().getDisplayName().equals(DISPLAY_NAME);
    }

    /**
     * 指定したプレイヤーに、指定したレベルのWirerodを与える
     *
     * @param player プレイヤー
     * @param level  レベル
     */
    static void giveWireRod(Player player, int level) {
        ItemStack rod = WireRodUtil.getWireRod(level);

        ItemStack temp = Compatibles.getItemInMainHand(player);
        Compatibles.setItemInMainHand(player, rod);

        if (temp != null) {
            player.getInventory().addItem(temp);
        }
    }
}