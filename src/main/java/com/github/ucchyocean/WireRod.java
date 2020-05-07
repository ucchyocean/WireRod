/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean;

import com.github.ucchyocean.ct.ColorTeaming;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ワイヤロッドプラグイン
 * 
 * @author ucchy
 */
public class WireRod extends JavaPlugin {

    private static final String NAME = "wirerod";

    private static WireRod instance;

    private WireRodConfig config;

    /**
     * プラグインが有効になったときに呼び出されるメソッド
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable() {

        instance = this;
        config = new WireRodConfig(this);

        getServer().getPluginManager().registerEvents(new WireRodListener(config), this);

        if (config.isEnableCraft()) {
            WireRodUtil.addRecipe();
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
        new ColorTeamingBridge((ColorTeaming) colorteaming)
                .registerItem(WireRodUtil.getWireRod(0), NAME, WireRodUtil.DISPLAY_NAME);
    }

    static WireRod getInstance() {
        if (instance == null) {
            instance = JavaPlugin.getPlugin(WireRod.class);
        }

        return instance;
    }

    WireRodConfig getWireRodConfig() {
        return config;
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

            if (!WireRodUtil.isWireRodRecipeSet() && config.isEnableCraft()) {
                WireRodUtil.addRecipe();
            } else if (WireRodUtil.isWireRodRecipeSet() && !config.isEnableCraft()) {
                WireRodUtil.removeRecipe();
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

            WireRodUtil.giveWireRod(player, level);

            return true;
        }
        
        if (args.length >= 2 && args[0].equalsIgnoreCase("give")) {

            if (!sender.hasPermission("wirerod.give")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission \"wirerod.give\".");
                return true;
            }

            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            int level = config.getDefaultLevel();
            if (args.length >= 3 && args[2].matches("^[0-9]+$")) {
                level = Integer.parseInt(args[2]);
            }

            WireRodUtil.giveWireRod(player, level);

            return true;
        }

        return false;
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
}
