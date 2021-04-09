package cn.enaium.ml4g.task;

import cn.enaium.ml4g.ML4GExtension;
import cn.enaium.ml4g.entity.AssetObject;
import cn.enaium.ml4g.util.GameUtil;
import cn.enaium.ml4g.util.UrlUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Enaium
 */
public class DownloadAssetTask extends Task {
    @TaskAction
    public void downloadAsset() {
        String asset = GameUtil.getClientAsset(extension.gameVersion);
        File index = GameUtil.getClientIndexFile(extension);

        try {
            if (!GameUtil.fileVerify(index, GameUtil.getClientAssetSha1(extension.gameVersion))) {
                FileUtils.write(index, asset, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            getProject().getLogger().lifecycle(e.getMessage(), e);
        }


        HashMap<String, AssetObject> objectHashMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> objects : new Gson().fromJson(asset, JsonObject.class).get("objects").getAsJsonObject().entrySet()) {
            objectHashMap.put(objects.getKey(), new Gson().fromJson(objects.getValue(), AssetObject.class));
        }

        objectHashMap.forEach((name, assetObject) -> {
            File local = GameUtil.getLocalClientObjectFile(assetObject.getHash());
            File objectFile = GameUtil.getClientObjectFile(extension, assetObject.getHash());


            try {
                if (!GameUtil.fileVerify(objectFile, assetObject.getHash())) {
                    if (GameUtil.fileVerify(local, assetObject.getHash())) {
                        try {
                            FileUtils.copyFile(local, objectFile);
                        } catch (IOException e) {
                            getProject().getLogger().lifecycle(e.getMessage(), e);
                        }
                    } else {
                        FileUtils.writeByteArrayToFile(objectFile, UrlUtil.readFile(UrlUtil.GAME_RESOURCE + "/" + assetObject.getHash().substring(0, 2) + "/" + assetObject.getHash()));
                    }
                }
            } catch (IOException e) {
                getProject().getLogger().lifecycle(e.getMessage(), e);
            }
        });

        File nativeJarDir = GameUtil.getNativeJarDir(extension);
        File nativeFileDir = GameUtil.getNativeFileDir(extension);

        GameUtil.getNatives(extension.gameVersion).forEach(link -> {
            String name = link.substring(link.lastIndexOf("/") + 1);
            File nativeJarFile = new File(nativeJarDir, name);
            if (!nativeJarFile.exists()) {
                try {
                    FileUtils.writeByteArrayToFile(nativeJarFile, UrlUtil.readFile(link));
                } catch (IOException e) {
                    getProject().getLogger().lifecycle(e.getMessage(), e);
                }
            }

            try {
                ZipFile zipFile = new ZipFile(nativeJarFile);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    if (zipEntry.isDirectory() || zipEntry.getName().contains("META-INF"))
                        continue;
                    FileUtils.writeByteArrayToFile(new File(nativeFileDir, zipEntry.getName()), IOUtils.toByteArray(zipFile.getInputStream(zipEntry)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
