/*
 * @author     ucchy
 * @license    GPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Fish;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * @author ucchy
 * ワイヤロッドプラグイン
 */
public class WireRod extends JavaPlugin implements Listener {

    private static final String NAME = "wirerod";
    private static final String DISPLAY_NAME =
            ChatColor.BLUE.toString() + ChatColor.BOLD.toString() + NAME;
    private static final int DEFAULT_LEVEL = 4;
    private static final int DEFAULT_COST = 10;
    private static final int MAX_LEVEL = 20;
    private static final int REVIVE_SECONDS = 5;
    private static final int REVIVE_AMOUNT = 30;
    private static final int HOOK_LAUNCH_SPEED = 3;

    private ItemStack item;

    private int level;
    private int cost;
    private int speed;
    private boolean revive;
    private int reviveSeconds;
    private int reviveAmount;


    /**
     * プラグインが有効になったときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable(){

        saveDefaultConfig();
        loadConfigDatas();

        getServer().getPluginManager().registerEvents(this, this);

        item = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta wirerodMeta = item.getItemMeta();
        wirerodMeta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(wirerodMeta);
    }

    /**
     * 設定情報の読み込み処理
     */
    private void loadConfigDatas() {

        FileConfiguration config = getConfig();
        level = config.getInt("defaultLevel", DEFAULT_LEVEL);
        cost = config.getInt("cost", DEFAULT_COST);
        speed = config.getInt("speed", HOOK_LAUNCH_SPEED);
        revive = config.getBoolean("revive", true);
        if ( revive ) {
            reviveSeconds = config.getInt("reviveSeconds", REVIVE_SECONDS);
            reviveAmount = config.getInt("reviveAmount", REVIVE_AMOUNT);
        }
    }

    /**
     * コマンドが実行されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {

        if ( args.length <= 0 ) {
            return false;
        }

        if ( args[0].equalsIgnoreCase("get") ) {

            if ( !(sender instanceof Player) ) {
                sender.sendMessage(ChatColor.RED + "This command can only use in game.");
                return true;
            }

            Player player = (Player)sender;

            int level = this.level;
            if ( args.length >= 2 && args[1].matches("^[0-9]+$") ) {
                level = Integer.parseInt(args[1]);
            }

            giveWirerod(player, level);

            return true;

        } else if ( args.length >= 2 && args[0].equalsIgnoreCase("give") ) {

            Player player = getServer().getPlayerExact(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            int level = this.level;
            if ( args.length >= 3 && args[2].matches("^[0-9]+$") ) {
                level = Integer.parseInt(args[2]);
            }

            giveWirerod(player, level);

            return true;
        }

        return false;
    }

    /**
     * 指定したプレイヤーに、指定したレベルのWirerodを与える
     * @param player プレイヤー
     * @param level レベル
     */
    private void giveWirerod(Player player, int level) {

        ItemStack rod = this.item.clone();

        if ( level < 1 ) {
            level = 1;
        } else if ( level > MAX_LEVEL ) {
            level = MAX_LEVEL;
        }

        rod.addUnsafeEnchantment(Enchantment.OXYGEN, level);

        ItemStack temp = player.getItemInHand();
        player.setItemInHand(rod);
        if ( temp != null ) {
            player.getInventory().addItem(temp);
        }
    }

    /**
     * Wirerodの針を投げたり、針がかかったときに呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onHook(PlayerFishEvent event) {

        final Player player = event.getPlayer();
        final Fish hook = event.getHook();

        if ( player.getItemInHand() == null ||
                player.getItemInHand().getType() == Material.AIR ||
                player.getItemInHand().getItemMeta().getDisplayName() == null ||
                !player.getItemInHand().getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
            return;
        }

        if ( event.getState() == State.FISHING ) {
            // 針を投げるときの処理

            // 針の速度を上げる
            hook.setVelocity(hook.getVelocity().multiply(speed));

        } else if ( event.getState() == State.CAUGHT_ENTITY ||
                event.getState() == State.IN_GROUND ) {
            // 針をひっぱるときの処理

            Location eLoc = player.getEyeLocation();

            if ( !hasExperience(player, cost) ) {
                player.sendMessage(ChatColor.RED + "no fuel!!");
                player.playEffect(eLoc, Effect.SMOKE, 4);
                player.playEffect(eLoc, Effect.SMOKE, 4);
                player.playSound(eLoc, Sound.IRONGOLEM_THROW, (float)1.0, (float)1.5);
                return;
            }

            ItemStack rod = player.getItemInHand();
            double level = (double)rod.getEnchantmentLevel(Enchantment.OXYGEN);

            takeExperience(player, cost);
            rod.setDurability((short)0);

            if ( revive && !hasExperience(player, cost) ) {
                BukkitRunnable runnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        takeExperience(player, -reviveAmount);
                    }
                };
                runnable.runTaskLater(this, reviveSeconds * 20);
                player.sendMessage(ChatColor.GOLD + "your fuel will revive after " + reviveSeconds + " seconds.");
            }

            // 針がかかった場所に向かって飛び出す
            Location loc1 = hook.getLocation();
            Location loc2 = player.getLocation();
            Vector vector = new Vector(
                    loc1.getX()-loc2.getX(),
                    loc1.getY()-loc2.getY(),
                    loc1.getZ()-loc2.getZ());
            player.setVelocity(vector.normalize().multiply(level/2));
            player.setFallDistance(-1000F);
            player.playEffect(eLoc, Effect.POTION_BREAK, 22);
            player.playEffect(eLoc, Effect.POTION_BREAK, 22);
        }
    }

    /**
     * Wirerodの針が、地面やブロック、MOBに刺さったときに呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onHit(ProjectileHitEvent event) {

        Projectile projectile = event.getEntity();
        LivingEntity shooter = projectile.getShooter();

        if ( shooter == null || !(shooter instanceof Player) ) {
            return;
        }

        Player player = (Player)shooter;

        if ( player.getItemInHand() == null ||
                player.getItemInHand().getType() == Material.AIR ||
                player.getItemInHand().getItemMeta().getDisplayName() == null ||
                !player.getItemInHand().getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
            return;
        }

        // 音を出す
        player.playSound(player.getEyeLocation(), Sound.ARROW_HIT, 1, (float)0.5);
    }

    /**
     * プレイヤーから、指定した経験値量を減らす。
     * @param player プレイヤー
     * @param amount 減らす量
     */
    public static void takeExperience(final Player player, int amount) {
        player.giveExp(-amount);
        updateExp(player);
    }

    /**
     * プレイヤーが指定した量の経験値を持っているかどうか判定する。
     * @param player プレイヤー
     * @param amount 判定する量
     * @return もっているかどうか
     */
    public static boolean hasExperience(final Player player, int amount) {
        return (player.getTotalExperience() >= amount);
    }

    /**
     * プレイヤーの経験値量を、指定値に設定する。
     * @param player プレイヤー
     * @param amount 経験値の量
     */
    public static void setExperience(final Player player, int amount) {
        player.setTotalExperience(amount);
        updateExp(player);
    }

    /**
     * 経験値表示を更新する
     * @param player 更新対象のプレイヤー
     */
    private static void updateExp(final Player player) {

        int total = player.getTotalExperience();
        player.setLevel(0);
        player.setExp(0);
        while ( total > player.getExpToLevel() ) {
            total -= player.getExpToLevel();
            player.setLevel(player.getLevel()+1);
        }
        float xp = (float)total / (float)player.getExpToLevel();
        player.setExp(xp);
    }
}
