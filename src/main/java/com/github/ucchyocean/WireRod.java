/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.ucchyocean.ct.ColorTeaming;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

/**
 * ワイヤロッドプラグイン
 * 
 * @author ucchy
 */
@SuppressWarnings("deprecation")
public class WireRod extends JavaPlugin implements Listener {

    private static final String NAME = "wirerod";
    private static final String DISPLAY_NAME = NAME;

    private static final String PROTECT_FALL_META_NAME = "wirerodfallprotect";

    protected static final int MAX_LEVEL = 20;

    protected static WireRod instance;

    private ItemStack item;
    private WireRodConfig config;
    private ShapedRecipe recipe;

    /**
     * プラグインが有効になったときに呼び出されるメソッド
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable() {

        instance = this;

        config = new WireRodConfig(this);

        getServer().getPluginManager().registerEvents(this, this);

        item = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta wirerodMeta = item.getItemMeta();
        wirerodMeta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(wirerodMeta);

        if (config.isEnableCraft()) {
            makeRecipe();
        }

        loadColorTeaming();
    }

    private void loadColorTeaming() {
        if (!getServer().getPluginManager().isPluginEnabled("ColorTeaming")) {
            return;
        }

        Plugin colorteaming = getServer().getPluginManager().getPlugin("ColorTeaming");
        if (!(colorteaming instanceof ColorTeaming)) {
            return;
        }

        String ctversion = colorteaming.getDescription().getVersion();
        if (!isUpperVersion(ctversion, "2.2.5")) {
            getLogger().warning("ColorTeaming was too old. The cooperation feature will be disabled.");
            getLogger().warning("NOTE: Please use ColorTeaming v2.2.5 or later version.");
            return;
        }

        getLogger().info(
                "ColorTeaming was loaded. " + getDescription().getName() + " is in cooperation with ColorTeaming.");
        new ColorTeamingBridge((ColorTeaming) colorteaming).registerItem(item, NAME, DISPLAY_NAME);
    }

    /**
     * レシピを登録する
     */
    private void makeRecipe() {

        recipe = new ShapedRecipe(getWirerod(config.getDefaultLevel()));
        recipe.shape("  I", " IS", "I S");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STRING);
        getServer().addRecipe(recipe);
    }

    private void removeRecipe() {

        Iterator<Recipe> it = getServer().recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            ItemStack result = recipe.getResult();
            if (!result.hasItemMeta() || !result.getItemMeta().hasDisplayName()
                    || !result.getItemMeta().getDisplayName().equals(DISPLAY_NAME)) {
                continue;
            }
            it.remove();
        }

        this.recipe = null;
    }

    /**
     * コマンドが実行されたときに呼び出されるメソッド
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length <= 0) {
            return false;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("wirerod.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission \"wirerod.reload\".");
                return true;
            }

            // コンフィグ再読込
            config.reloadConfig();

            if (recipe == null && config.isEnableCraft()) {
                makeRecipe();
            } else if (recipe != null && !config.isEnableCraft()) {
                removeRecipe();
            }

            sender.sendMessage(ChatColor.GREEN + "WireRod configuration was reloaded!");

            return true;
        }
        
        if (args[0].equalsIgnoreCase("get")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only use in game.");
                return true;
            }

            if (!sender.hasPermission("wirerod.get")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission \"wirerod.get\".");
                return true;
            }

            Player player = (Player) sender;

            int level = config.getDefaultLevel();
            if (args.length >= 2 && args[1].matches("^[0-9]+$")) {
                level = Integer.parseInt(args[1]);
            }

            giveWirerod(player, level);

            return true;
        }
        
        if (args.length >= 2 && args[0].equalsIgnoreCase("give")) {

            if (!sender.hasPermission("wirerod.give")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission \"wirerod.give\".");
                return true;
            }

            Player player = getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            int level = config.getDefaultLevel();
            if (args.length >= 3 && args[2].matches("^[0-9]+$")) {
                level = Integer.parseInt(args[2]);
            }

            giveWirerod(player, level);

            return true;
        }

        return false;
    }

    /**
     * 指定したプレイヤーに、指定したレベルのWirerodを取得する
     * 
     * @param level レベル
     */
    private ItemStack getWirerod(int level) {

        ItemStack rod = this.item.clone();

        if (level < 1) {
            level = 1;
        } else if (level > MAX_LEVEL) {
            level = MAX_LEVEL;
        }

        rod.addUnsafeEnchantment(Enchantment.OXYGEN, level);

        return rod;
    }

    /**
     * 指定したプレイヤーに、指定したレベルのWirerodを与える
     * 
     * @param player プレイヤー
     * @param level  レベル
     */
    private void giveWirerod(Player player, int level) {

        ItemStack rod = getWirerod(level);
        ItemStack temp = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(rod);
        if (temp != null) {
            player.getInventory().addItem(temp);
        }
    }

    /**
     * Wirerodの針を投げたり、針がかかったときに呼び出されるメソッド
     * 
     * @param event
     */
    @EventHandler
    public void onHook(PlayerFishEvent event) {

        final Player player = event.getPlayer();
        final FishHook hook = event.getHook();

        // パーミッションが無いなら何もしない
        if (!player.hasPermission("wirerod.action")) {
            return;
        }

        // 手に持っているアイテムがWireRodでないなら何もしない
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (rod == null || rod.getType() == Material.AIR || !rod.getItemMeta().hasDisplayName()
                || !rod.getItemMeta().getDisplayName().equals(DISPLAY_NAME)) {
            return;
        }

        // 水中呼吸エンチャントがついていないなら何もしない
        if (!rod.containsEnchantment(Enchantment.OXYGEN)) {
            return;
        }

        // 針を投げるときの処理
        if (event.getState() == State.FISHING) {

            // 向いている方向のブロックを取得し、その中にフックをワープさせる
            Location target = hookTargetBlockOrLivingEntity(player, hook, config.getWireRange());
            if (target == null) {
                player.sendMessage(ChatColor.RED + "too far!!");
                event.setCancelled(true);
                return;
            }

            // フックにメタデータを入れる
            hook.setMetadata(NAME, new FixedMetadataValue(this, true));

            // 刺さった場所にエフェクトを発生させる
            hook.getWorld().playEffect(hook.getLocation(), Effect.STEP_SOUND, 8);
            return;
        }

        // 針をひっぱるとき、つまり
        // 魚がかかった状態、魚を釣っている状態、竿を投げた状態以外の場合の処理
        if (event.getState() != State.CAUGHT_FISH && event.getState() != State.BITE) {

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
                player.setMetadata(PROTECT_FALL_META_NAME, new FixedMetadataValue(this, true));
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
            event.getEntity().removeMetadata(PROTECT_FALL_META_NAME, this);
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
                e.addPassenger(hook);
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

    /**
     * 指定されたバージョンが、基準より新しいバージョンかどうかを確認する<br>
     * 完全一致した場合もtrueになることに注意。
     * 
     * @param version 確認するバージョン
     * @param border  基準のバージョン
     * @return 基準より確認対象の方が新しいバージョンかどうか
     */
    private boolean isUpperVersion(String version, String border) {

        int hyphen = version.indexOf("-");
        if (hyphen > 0) {
            version = version.substring(0, hyphen);
        }

        String[] versionArray = version.split("\\.");
        int[] versionNumbers = new int[versionArray.length];
        for (int i = 0; i < versionArray.length; i++) {
            if (!versionArray[i].matches("[0-9]+"))
                return false;
            versionNumbers[i] = Integer.parseInt(versionArray[i]);
        }

        String[] borderArray = border.split("\\.");
        int[] borderNumbers = new int[borderArray.length];
        for (int i = 0; i < borderArray.length; i++) {
            if (!borderArray[i].matches("[0-9]+"))
                return false;
            borderNumbers[i] = Integer.parseInt(borderArray[i]);
        }

        int index = 0;
        while ((versionNumbers.length > index) && (borderNumbers.length > index)) {
            if (versionNumbers[index] > borderNumbers[index]) {
                return true;
            } else if (versionNumbers[index] < borderNumbers[index]) {
                return false;
            }
            index++;
        }
        if (borderNumbers.length == index) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 指定した名前のプレイヤーを取得する
     * 
     * @param name プレイヤー名
     * @return プレイヤー
     */
    public static Player getPlayer(String name) {
        return Bukkit.getPlayerExact(name);
    }
}
