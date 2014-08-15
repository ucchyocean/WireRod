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

import org.bukkit.configuration.file.FileConfiguration;

/**
 * WireRodのコンフィグクラス
 * @author ucchy
 */
public class WireRodConfig {

    private WireRod parent;

    private int defaultLevel;
    private int decreaseDurability;
    private int wireRange;
    private double distanceBonusRatio;

    /**
     * コンストラクタ
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

        if ( !parent.getDataFolder().exists() ) {
            parent.getDataFolder().mkdirs();
        }

        File file = new File(parent.getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            copyFileFromJar(
                    parent.getJarFile(), file, "config_ja.yml", false);
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        defaultLevel = conf.getInt("defaultLevel", 4);
        if ( defaultLevel < 1 ) {
            defaultLevel = 1;
        } else if ( defaultLevel > WireRod.MAX_LEVEL ) {
            defaultLevel = WireRod.MAX_LEVEL;
        }

        decreaseDurability = conf.getInt("decreaseDurability", 2);
        if ( decreaseDurability < 0 ) {
            decreaseDurability = 0;
        }

        wireRange = conf.getInt("wireRange", 30);

        distanceBonusRatio = conf.getDouble("distanceBonusRatio", 1.0);
    }

    /**
     * jarファイルの中に格納されているファイルを、jarファイルの外にコピーするメソッド
     * @param jarFile jarファイル
     * @param targetFile コピー先
     * @param sourceFilePath コピー元
     * @param isBinary バイナリファイルかどうか
     */
    private static void copyFileFromJar(
            File jarFile, File targetFile, String sourceFilePath, boolean isBinary) {

        InputStream is = null;
        FileOutputStream fos = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        File parent = targetFile.getParentFile();
        if ( !parent.exists() ) {
            parent.mkdirs();
        }

        try {
            JarFile jar = new JarFile(jarFile);
            ZipEntry zipEntry = jar.getEntry(sourceFilePath);
            is = jar.getInputStream(zipEntry);

            fos = new FileOutputStream(targetFile);

            if ( isBinary ) {
                byte[] buf = new byte[8192];
                int len;
                while ( (len = is.read(buf)) != -1 ) {
                    fos.write(buf, 0, len);
                }
                fos.flush();
                fos.close();
                is.close();

            } else {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                writer = new BufferedWriter(new OutputStreamWriter(fos));

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
            if ( writer != null ) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( reader != null ) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( fos != null ) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( is != null ) {
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
     * @return decreaseDurability
     */
    public int getDecreaseDurability() {
        return decreaseDurability;
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
}
