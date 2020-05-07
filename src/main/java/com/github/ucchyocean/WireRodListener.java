package com.github.ucchyocean;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.ComplexLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class WireRodListener implements Listener {

    private static final String NAME = "wirerod";
    private static final String PROTECT_FALL_META_NAME = "wirerodfallprotect";

    private final WireRodConfig config;

    WireRodListener(WireRodConfig config) {
        this.config = config;
    }

    /**
     * Wirerodの針を投げたり、針がかかったときに呼び出されるメソッド
     * 
     * @param event
     */
    @EventHandler
    public void onHook(PlayerFishEvent event) {

        final Player player = event.getPlayer();
        FishHook hook;
        try {
            hook = event.getHook();
        } catch (NoSuchMethodError e) {
            try {
                hook = (FishHook) PlayerFishEvent.class.getMethod("getHook").invoke(event);
            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException ex) {
                e.printStackTrace();
                return;
            }
        }
            
        // パーミッションが無いなら何もしない
        if (!player.hasPermission("wirerod.action")) {
            return;
        }

        // 手に持っているアイテムがWireRodでないなら何もしない
        ItemStack rod = Compatibles.getHoldingWireRod(player);
        if (rod == null) {
            return;
        }

        // 水中呼吸エンチャントがついていないなら何もしない
        if (!rod.containsEnchantment(Enchantment.OXYGEN)) {
            return;
        }

        // 針を投げるときの処理
        if (Compatibles.isThrowHookState(event)) {

            // 向いている方向のブロックを取得し、その中にフックをワープさせる
            Location target = hookTargetBlockOrLivingEntity(player, hook, config.getWireRange());
            if (target == null) {
                player.sendMessage(ChatColor.RED + "too far!!");
                event.setCancelled(true);
                return;
            }

            // フックにメタデータを入れる
            hook.setMetadata(NAME, new FixedMetadataValue(WireRod.getInstance(), true));

            // 刺さった場所にエフェクトを発生させる
            hook.getWorld().playEffect(hook.getLocation(), Effect.STEP_SOUND, 8);
            return;
        }

        // 針をひっぱるとき、つまり
        // 魚がかかった状態、魚を釣っている状態、竿を投げた状態以外の場合の処理
        if (Compatibles.isPullHookState(event)) {

            // メタデータが入っていないなら無視する
            if (!hook.hasMetadata(NAME)) {
                return;
            }

            // ひっかかっているのは自分なら、2ダメージ(1ハート)を与える
            if (event.getCaught() != null && event.getCaught().equals(player)) {
                player.damage(2F, player);
                return;
            }

            Location eLoc = player.getEyeLocation();

            // ロッドと、そのレベルを取得
            int level = config.getDefaultLevel();
            if (rod.containsEnchantment(Enchantment.OXYGEN)) {
                level = rod.getEnchantmentLevel(Enchantment.OXYGEN);
            }

            // 針との距離と方向を調べる
            Location hookLoc = hook.getLocation();
            Location baseLoc = player.getLocation();
            Vector vector = hook.getLocation().subtract(baseLoc).toVector().normalize();

            // 針との距離で、飛び出す力を算出する
            double bonus = (hookLoc.distance(baseLoc) / 30.0 * config.getDistanceBonusRatio()) + 1.0;
            double power = level * bonus / 2;
            if (power > 10.0) {
                power = 10.0;
            }

            // 飛翔
            player.setVelocity(vector.multiply(power));

            // 落下ダメージ保護を加える
            if (config.isProtectFallDamage()) {
                player.setMetadata(PROTECT_FALL_META_NAME, new FixedMetadataValue(WireRod.getInstance(), true));
            }

            // エフェクト
            player.getWorld().playEffect(eLoc, Effect.POTION_BREAK, 22);
            player.getWorld().playEffect(eLoc, Effect.POTION_BREAK, 22);
        }
    }

    /**
     * エンティティがダメージを受けたときのイベント
     * 
     * @param event
     */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {

        // プレイヤーの被ダメージイベントで、落下ダメージで、
        // ダメージ保護用のメタデータを持っているなら、ダメージから保護する
        if (event.getEntity() instanceof Player && event.getCause() == DamageCause.FALL
                && event.getEntity().hasMetadata(PROTECT_FALL_META_NAME)) {

            event.setCancelled(true);
            event.getEntity().removeMetadata(PROTECT_FALL_META_NAME, WireRod.getInstance());
        }
    }

    /**
     * プレイヤーが向いている方向にあるブロックまたはLivingEntityを取得し、 釣り針をそこに移動する。
     * 
     * @param player プレイヤー
     * @param hook   釣り針
     * @param size   取得する最大距離、140以上を指定しないこと
     * @return プレイヤーが向いている方向にあるブロックまたはLivingEntityのLocation、 取得できない場合はnullがかえされる
     */
    private static Location hookTargetBlockOrLivingEntity(Player player, FishHook hook, int range) {

        // ターゲット先周辺のエンティティを取得する
        Location center = player.getLocation().clone();
        double halfrange = (double) range / 2.0;
        center.add(center.getDirection().multiply(halfrange));
        Entity orb = center.getWorld().spawnEntity(center, EntityType.EXPERIENCE_ORB);
        List<Entity> targets = new ArrayList<Entity>();
        for (Entity e : orb.getNearbyEntities(halfrange, halfrange, halfrange)) {
            if (e instanceof LivingEntity && !player.equals(e)) {
                targets.add(e);
            } else if (e instanceof ComplexLivingEntity) {
                for (ComplexEntityPart part : ((ComplexLivingEntity) e).getParts()) {
                    targets.add(part);
                }
            }
        }
        orb.remove();

        // 視線の先にあるブロックを取得する
        BlockIterator it = new BlockIterator(player, range);

        while (it.hasNext()) {
            Block block = it.next();

            if (block.getType() != Material.AIR) {
                // ブロックが見つかった、針を中にワープさせる
                Location location = block.getLocation().add(0.5, 0.5, 0.5);
                hook.teleport(location);
                return location;

            }

            // 位置が一致するLivingEntityがないか探す
            for (Entity e : targets) {
                if (block.getLocation().distanceSquared(e.getLocation()) > 4.0) {
                    continue;
                }
                Location location = e.getLocation();

                // LivingEntityが見つかった、針を載せる
                hook.teleport(location);
                Compatibles.addPassenger(e, hook);
                if (e instanceof LivingEntity) {
                    ((LivingEntity) e).damage(0F, player);
                } else if (e instanceof ComplexEntityPart) {
                    ((ComplexEntityPart) e).getParent().damage(0F, player);
                }
                return location;
            }
        }
        return null;
    }
}