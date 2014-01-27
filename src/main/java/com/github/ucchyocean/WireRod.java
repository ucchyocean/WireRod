/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.ComplexLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fish;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

/**
 * ワイヤロッドプラグイン
 * @author ucchy
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
    private static final boolean DEFAULT_REVIVE = true;
    private static final int DEFAULT_WIRE_RANGE = 30;

    protected static WireRod instance;
    
    private ItemStack item;

    private int configLevel;
    private int configCost;
    private boolean configRevive;
    private int configReviveSeconds;
    private int configReviveAmount;
    private int configWireRange;

    /**
     * プラグインが有効になったときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable(){

        instance = this;
        
        saveDefaultConfig();
        loadConfigDatas();

        getServer().getPluginManager().registerEvents(this, this);

        item = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta wirerodMeta = item.getItemMeta();
        wirerodMeta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(wirerodMeta);

        // ColorTeaming のロード
        Plugin colorteaming = null;
        if ( getServer().getPluginManager().isPluginEnabled("ColorTeaming") ) {
            colorteaming = getServer().getPluginManager().getPlugin("ColorTeaming");
            String ctversion = colorteaming.getDescription().getVersion();
            if ( isUpperVersion(ctversion, "2.2.5") ) {
                getLogger().info("ColorTeaming was loaded. " 
                        + getDescription().getName() + " is in cooperation with ColorTeaming.");
                ColorTeamingBridge bridge = new ColorTeamingBridge(colorteaming);
                bridge.registerItem(item, NAME, DISPLAY_NAME);
            } else {
                getLogger().warning("ColorTeaming was too old. The cooperation feature will be disabled.");
                getLogger().warning("NOTE: Please use ColorTeaming v2.2.5 or later version.");
            }
        }
    }

    /**
     * 設定情報の読み込み処理
     */
    private void loadConfigDatas() {

        FileConfiguration config = getConfig();
        configLevel = config.getInt("defaultLevel", DEFAULT_LEVEL);
        configCost = config.getInt("cost", DEFAULT_COST);
        configRevive = config.getBoolean("revive", DEFAULT_REVIVE);
        if (configRevive) {
            configReviveSeconds = config.getInt("reviveSeconds", REVIVE_SECONDS);
            configReviveAmount = config.getInt("reviveAmount", REVIVE_AMOUNT);
        }
        configWireRange = config.getInt("wireRange", DEFAULT_WIRE_RANGE);
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

        if (args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("wirerod.reload")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"wirerod.reload\".");
                return true;
            }

            // コンフィグ再読込
            this.reloadConfig();
            this.loadConfigDatas();
            sender.sendMessage(ChatColor.GREEN + "WireRod configuration was reloaded!");

            return true;

        } else if (args[0].equalsIgnoreCase("get")) {

            if ( !(sender instanceof Player) ) {
                sender.sendMessage(ChatColor.RED + "This command can only use in game.");
                return true;
            }

            if (!sender.hasPermission("wirerod.get")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"wirerod.get\".");
                return true;
            }

            Player player = (Player)sender;

            int level = configLevel;
            if ( args.length >= 2 && args[1].matches("^[0-9]+$") ) {
                level = Integer.parseInt(args[1]);
            }

            giveWirerod(player, level);

            return true;

        } else if ( args.length >= 2 && args[0].equalsIgnoreCase("give") ) {

            if (!sender.hasPermission("wirerod.give")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"wirerod.give\".");
                return true;
            }

            Player player = getServer().getPlayerExact(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            int level = configLevel;
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

        if ( !player.hasPermission("wirerod.action") ) return;

        if ( player.getItemInHand() == null ||
                player.getItemInHand().getType() == Material.AIR ||
                !player.getItemInHand().getItemMeta().hasDisplayName() ||
                !player.getItemInHand().getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
            return;
        }

        if ( event.getState() == State.FISHING ) {
            // 針を投げるときの処理

            // 向いている方向のブロックを取得し、その中にフックをワープさせる
            Location target = hookTargetBlockOrLivingEntity(player, hook, configWireRange);
            if ( target == null ) {
                event.setCancelled(true);
                return;
            }
            
            // フックにメタデータを入れる
            hook.setMetadata(NAME, new FixedMetadataValue(this, true));

            // 刺さった場所にエフェクトを発生させる
            hook.getWorld().playEffect(hook.getLocation(), Effect.STEP_SOUND, 8);

        } else if ( event.getState() == State.CAUGHT_ENTITY ||
                event.getState() == State.IN_GROUND || 
                event.getState() == State.FAILED_ATTEMPT ) {
            // 針をひっぱるときの処理

            // メタデータが入っていないなら無視する
            if ( !hook.hasMetadata(NAME) ) {
                return;
            }
            
            // ひっかかっているのは自分なら、2ダメージ(1ハート)を与える
            if ( event.getCaught() != null &&
                    event.getCaught().equals(player) ) {
                player.damage(2F, player);
                return;
            }

            Location eLoc = player.getEyeLocation();

            // 経験値が不足している場合は、燃料切れとして終了する
            if ( !hasExperience(player, configCost) ) {
                player.sendMessage(ChatColor.RED + "no fuel!!");
                player.getWorld().playEffect(eLoc, Effect.SMOKE, 4);
                player.getWorld().playEffect(eLoc, Effect.SMOKE, 4);
                player.playSound(eLoc, Sound.IRONGOLEM_THROW, (float)1.0, (float)1.5);
                return;
            }

            // ロッドと、そのレベルを取得
            ItemStack rod = player.getItemInHand();
            double level = configLevel;
            if ( rod.containsEnchantment(Enchantment.OXYGEN) ) {
                level = (double)rod.getEnchantmentLevel(Enchantment.OXYGEN);
            }

            // 経験値を消費する、耐久値を0に戻す
            takeExperience(player, configCost);
            rod.setDurability((short)0);

            // もし今回の操作で燃料切れになった場合は、指定秒後に復活させる
            if ( configRevive && !hasExperience(player, configCost) ) {
                BukkitRunnable runnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        takeExperience(player, -configReviveAmount);
                    }
                };
                runnable.runTaskLater(this, configReviveSeconds * 20);
                player.sendMessage(ChatColor.GOLD +
                        "your fuel will revive after " + configReviveSeconds + " seconds.");
            }

            // 飛翔
            Location hookLoc = hook.getLocation();
            Location baseLoc = player.getLocation();
            Vector vector = new Vector(
                    hookLoc.getX()-baseLoc.getX(),
                    hookLoc.getY()-baseLoc.getY(),
                    hookLoc.getZ()-baseLoc.getZ());
            player.setVelocity(vector.normalize().multiply(level/2));
            player.setFallDistance(-1000F);
            player.getWorld().playEffect(eLoc, Effect.POTION_BREAK, 22);
            player.getWorld().playEffect(eLoc, Effect.POTION_BREAK, 22);
        }
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
    
    /**
     * プレイヤーが向いている方向にあるブロックまたはLivingEntityを取得し、
     * 釣り針をそこに移動する。
     * @param player プレイヤー
     * @param hook 釣り針
     * @param size 取得する最大距離、140以上を指定しないこと
     * @return プレイヤーが向いている方向にあるブロックまたはLivingEntityのLocation、
     * 取得できない場合はnullがかえされる
     */
    private static Location hookTargetBlockOrLivingEntity(Player player, Fish hook, int range) {
        
        // ターゲット先周辺のエンティティを取得する
        Location center = player.getLocation().clone();
        double halfrange = (double)range / 2.0;
        center.add(center.getDirection().multiply(halfrange));
        Entity sb = center.getWorld().spawnEntity(center, EntityType.SNOWBALL);
        ArrayList<Entity> targets = new ArrayList<Entity>();
        for ( Entity e : sb.getNearbyEntities(halfrange, halfrange, halfrange) ) {
            if ( e instanceof LivingEntity && !player.equals(e) ) {
                targets.add(e);
            } else if ( e instanceof ComplexLivingEntity ) {
                for ( ComplexEntityPart part : ((ComplexLivingEntity)e).getParts() ) {
                    targets.add(part);
                }
            }
        }
        sb.remove();
        
        // 視線の先にあるブロックを取得する
        BlockIterator it = new BlockIterator(player, range);
        
        while ( it.hasNext() ) {
            Block block = it.next();
            
            if ( block.getType() != Material.AIR ) {
                // ブロックが見つかった、針を中にワープさせる
                Location location = block.getLocation();
                location.add(0.5, 0.5, 0.5);
                hook.teleport(location);
                return location;
                
            } else {
                // 位置が一致するLivingEntityがないか探す
                for ( Entity e : targets ) {
                    Location location = e.getLocation();
                    if ( block.getLocation().distance(e.getLocation()) <= 2.0 ) {
                        // LivingEntityが見つかった、針を載せる
                        hook.teleport(location);
                        e.setPassenger(hook);
                        if ( e instanceof LivingEntity ) {
                            ((LivingEntity)e).damage(0F, player);
                        } else if ( e instanceof ComplexEntityPart ) {
                            ((ComplexEntityPart)e).getParent().damage(0F, player);
                        }
                        return location;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 指定されたバージョンが、基準より新しいバージョンかどうかを確認する<br>
     * 完全一致した場合もtrueになることに注意。
     * @param version 確認するバージョン
     * @param border 基準のバージョン
     * @return 基準より確認対象の方が新しいバージョンかどうか
     */
    private boolean isUpperVersion(String version, String border) {

        String[] versionArray = version.split("\\.");
        int[] versionNumbers = new int[versionArray.length];
        for ( int i=0; i<versionArray.length; i++ ) {
            if ( !versionArray[i].matches("[0-9]+") )
                return false;
            versionNumbers[i] = Integer.parseInt(versionArray[i]);
        }

        String[] borderArray = border.split("\\.");
        int[] borderNumbers = new int[borderArray.length];
        for ( int i=0; i<borderArray.length; i++ ) {
            if ( !borderArray[i].matches("[0-9]+") )
                return false;
            borderNumbers[i] = Integer.parseInt(borderArray[i]);
        }

        int index = 0;
        while ( (versionNumbers.length > index) && (borderNumbers.length > index) ) {
            if ( versionNumbers[index] > borderNumbers[index] ) {
                return true;
            } else if ( versionNumbers[index] < borderNumbers[index] ) {
                return false;
            }
            index++;
        }
        if ( borderNumbers.length == index ) {
            return true;
        } else {
            return false;
        }
    }
}
