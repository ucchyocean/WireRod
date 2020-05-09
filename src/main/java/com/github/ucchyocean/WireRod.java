/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ワイヤロッドプラグイン
 *
 * @author ucchy
 */
public class WireRod extends JavaPlugin {

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
    }

    /**
     * プラグインが無効化されたときに呼び出されるメソッド
     *
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onDisable() {
        WireRodUtil.removeRecipe();
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
}
