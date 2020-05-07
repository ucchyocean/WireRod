package com.github.ucchyocean;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

/**
 * 1.8から1.15.2までの互換性を保つためのメソッドを集めたクラス。
 * TODO: 例外キャッチより、バージョンを取得してそれぞれのバージョンに適した処理を実装する。
 */
@SuppressWarnings("deprecation")
final class Compatibles {

    static ShapedRecipe createWireRodRecipe(int defualtLevel) {
        ShapedRecipe recipe;
        ItemStack rod = WireRodUtil.getWireRod(defualtLevel);
        try {
            NamespacedKey key = new NamespacedKey(WireRod.getPlugin(WireRod.class), WireRodUtil.DISPLAY_NAME);
            recipe = new ShapedRecipe(key, rod);
        } catch (NoClassDefFoundError e) { // <= 1.11
            recipe = new ShapedRecipe(rod);
        }

        recipe.shape("  I", " IS", "I S");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STRING);
        return recipe;
    }

    static ItemStack getItemInMainHand(Player player) {
        try {
            return player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError e) { // <= 1.8.8
            return player.getInventory().getItemInHand();
        }
    }

    static void setItemInMainHand(Player player, ItemStack item) {
        try {
            player.getInventory().setItemInMainHand(item);
        } catch (NoSuchMethodError e) { // <= 1.8.8
            player.getInventory().setItemInHand(item);
        }
    }

    /**
     * 両手のどちらかにあるワイヤーロッドを取得する。
     * Minecraftのバージョン1.9以前で、片手にしかアイテムを持てない場合は昔のメソッドを使う。
     *
     * @param player
     * @return
     */
    static ItemStack getHoldingWireRod(Player player) {

        ItemStack rod;
        try {
            ItemStack offHandItem = player.getInventory().getItemInOffHand();
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();

            // メインハンドにワイヤーロッドがある
            if (WireRodUtil.isWireRod(mainHandItem)) {
                rod = mainHandItem;
            // オフハンドにワイヤーロッドがある
            } else if (WireRodUtil.isWireRod(offHandItem)) {
                // メインハンドが釣り竿でないなら終了
                if (mainHandItem.getType() == Material.FISHING_ROD) {
                    return null;
                }
                // メインハンドが釣り竿以外ならオフハンドで発動する
                rod = offHandItem;
            } else {
                return null;
            }
        } catch (NoSuchMethodError e) { // <= 1.8.8
            // 手に持っているアイテムがWireRodでないなら何もしない
            rod = player.getInventory().getItemInHand();
            if (!WireRodUtil.isWireRod(rod)) {
                return null;
            }
        }

        return rod;
    }

    /**
     * 釣り竿を投げる動作をしている場合のイベント発火かどうかを調べる。
     *
     * @param event PlayerFishEvent
     * @return 釣り竿を投げる動作をしているかどうか
     */
    static boolean isThrowHookState(PlayerFishEvent event) {
        return event.getState() == State.FISHING;
    }

    /**
     * 釣り竿を引く動作をしている場合のイベント発火かどうかを調べる。
     *
     * @param event PlayerFishEvent
     * @return 釣り竿を引く動作をしているかどうか
     */
    static boolean isPullHookState(PlayerFishEvent event) {
        boolean pullHook = event.getState() != State.CAUGHT_FISH;
        try {
            pullHook = pullHook && event.getState() != State.valueOf("BITE");
        } catch (IllegalArgumentException ignored) { // 1.8.8 では PlayerFishEvent.State.BITE は存在しない。
        }
        return pullHook;
    }

    /**
     * {@code addPassenger}メソッドの追加前のバージョンの場合は、一番上に居るモブを取得して{@code setPassenger}を実行する。
     * {@code addPassenger}が使える場合はそのまま使う。
     *
     * @param vehicle 乗られるエンティティ
     * @param passenger 乗るエンティティ
     */
    static void addPassenger(Entity vehicle, Entity passenger) {
        try {
            vehicle.addPassenger(passenger);
        } catch (NoSuchMethodError e) { // <= 1.10.2
            getTopPassenger(vehicle).setPassenger(passenger);
        }
    }

    /**
     * 再帰的に一番上に乗っているモブを取得する。
     *
     * @param vehicle 下に居るモブ
     * @return 一番上のモブ
     */
    private static Entity getTopPassenger(Entity vehicle) {
        Entity passenger = vehicle.getPassenger();
        if (passenger == null) {
            return vehicle;
        } else {
            return getTopPassenger(passenger);
        }
    }
}