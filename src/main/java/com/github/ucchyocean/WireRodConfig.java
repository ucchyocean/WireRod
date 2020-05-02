/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package com.github.ucchyocean;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * WireRodのコンフィグクラス
 * 
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
     * 
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

        if (!parent.getDataFolder().exists()) {
            parent.getDataFolder().mkdirs();
        }

        File file = new File(parent.getDataFolder(), "config.yml");
        if (!file.exists()) {
            copyFileFromJar(parent.getJarFile(), file, "config_ja.yml", false);
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        defaultLevel = conf.getInt("defaultLevel", 4);
        if (defaultLevel < 1) {
            defaultLevel = 1;
        } else if (defaultLevel > WireRod.MAX_LEVEL) {
            defaultLevel = WireRod.MAX_LEVEL;
        }

        wireRange = conf.getInt("wireRange", 30);

        distanceBonusRatio = conf.getDouble("distanceBonusRatio", 1.0);

        protectFallDamage = conf.getBoolean("protectFallDamage", true);

        enableCraft = conf.getBoolean("enableCraft", true);
    }

    /**
     * jarファイルの中に格納されているファイルを、jarファイルの外にコピーするメソッド
     * 
     * @param jarFile        jarファイル
     * @param targetFile     コピー先
     * @param sourceFilePath コピー元
     * @param isBinary       バイナリファイルかどうか
     */
    private static void copyFileFromJar(File jarFile, File targetFile, String sourceFilePath, boolean isBinary) {

        InputStream is = null;
        FileOutputStream fos = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        File parent = targetFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            JarFile jar = new JarFile(jarFile);
            ZipEntry zipEntry = jar.getEntry(sourceFilePath);
            is = jar.getInputStream(zipEntry);

            fos = new FileOutputStream(targetFile);

            if (isBinary) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }
                fos.flush();
                fos.close();
                is.close();

            } else {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                // CB190以降は、書き出すファイルエンコードにUTF-8を強制する。
                if (isCB19orLater()) {
                    writer = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
                } else {
                    writer = new BufferedWriter(new OutputStreamWriter(fos));
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }
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

    /**
     * 現在動作中のCraftBukkitが、v1.9 以上かどうかを確認する
     * 
     * @return v1.9以上ならtrue、そうでないならfalse
     */
    public static boolean isCB19orLater() {
        return isUpperVersion(Bukkit.getBukkitVersion(), "1.9");
    }

    /**
     * 指定されたバージョンが、基準より新しいバージョンかどうかを確認する
     * 
     * @param version 確認するバージョン
     * @param border  基準のバージョン
     * @return 基準より確認対象の方が新しいバージョンかどうか<br/>
     *         ただし、無効なバージョン番号（数値でないなど）が指定された場合はfalseに、 2つのバージョンが完全一致した場合はtrueになる。
     */
    private static boolean isUpperVersion(String version, String border) {

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
