package cn.enaium.ml4g.util;

import cn.enaium.ml4g.ML4GExtension;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static cn.enaium.ml4g.util.UrlUtil.*;

/**
 * @author Enaium
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class GameUtil {

    public static File getMinecraftDir() {
        File minecraftFolder;
        if (getOsName().contains("win")) {
            minecraftFolder = new File(System.getenv("APPDATA"), File.separator + ".minecraft");
        } else if (getOsName().contains("mac")) {
            minecraftFolder = new File(System.getProperty("user.home"), File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft");
        } else {
            minecraftFolder = new File(System.getProperty("user.home"), File.separator + ".minecraft");
        }
        return minecraftFolder;
    }

    private static String getOsName() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT);
    }

    public static String getRelease() {
        return new Gson().fromJson(readString(GAME_URL), JsonObject.class).get("latest").getAsJsonObject().get("release").getAsString();
    }

    public static String getSnapshot() {
        return new Gson().fromJson(readString(GAME_URL), JsonObject.class).get("latest").getAsJsonObject().get("snapshot").getAsString();
    }

    public static String getJson(String version) {
        String jsonUrl = "";
        for (JsonElement jsonElement : new Gson().fromJson(readString(GAME_URL), JsonObject.class).get("versions").getAsJsonArray()) {
            if (jsonElement.getAsJsonObject().get("id").getAsString().equals(version)) {
                jsonUrl = jsonElement.getAsJsonObject().get("url").getAsString();
            }
        }
        return readString(jsonUrl);
    }


    public static List<String> getLibraries(String version) {
        LinkedHashMap<String, String> list = new LinkedHashMap<>();
        for (JsonElement jsonElement : new Gson().fromJson(getJson(version), JsonObject.class).get("libraries").getAsJsonArray()) {
            String name = jsonElement.getAsJsonObject().get("name").getAsString();
            list.put(name.substring(0, name.lastIndexOf(":")), name.substring(name.lastIndexOf(":")));
        }
        List<String> libraries = new ArrayList<>();
        for (Map.Entry<String, String> entry : list.entrySet()) {
            libraries.add(entry.getKey() + entry.getValue());
        }
        return libraries;
    }

    public static List<String> getNatives(String version) {
        List<String> libraries = new ArrayList<>();

        for (JsonElement jsonElement : new Gson().fromJson(getJson(version), JsonObject.class).get("libraries").getAsJsonArray()) {
            JsonObject downloads = jsonElement.getAsJsonObject().get("downloads").getAsJsonObject();
            if (downloads.has("classifiers")) {
                String name = "natives-linux";
                if (getOsName().contains("win")) {
                    name = "natives-windows";
                } else if (getOsName().contains("mac")) {
                    name = "natives-macos";
                }
                JsonObject classifiers = downloads.get("classifiers").getAsJsonObject();
                if (classifiers.has(name)) {
                    libraries.add(downloads.get("classifiers").getAsJsonObject().get(name).getAsJsonObject().get("url").getAsString());
                }
            }
        }
        return libraries;
    }

    public static File getLocalJar(String version) {
        return new File(getMinecraftDir(), "versions" + File.separator + version + File.separator + version + ".jar");
    }

    public static File getClientNativeDir(ML4GExtension extension) {
        File file = new File(getGameDir(extension), extension.gameVersion + File.separator + extension.gameVersion + "-native");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    public static File getNativeJarDir(ML4GExtension extension) {
        File nativeJarDir = new File(GameUtil.getClientNativeDir(extension), "jars");
        if (!nativeJarDir.exists()) {
            nativeJarDir.mkdir();
        }
        return nativeJarDir;
    }

    public static File getNativeFileDir(ML4GExtension extension) {
        File nativeFileDir = new File(GameUtil.getClientNativeDir(extension), "natives");
        if (!nativeFileDir.exists()) {
            nativeFileDir.mkdir();
        }
        return nativeFileDir;
    }

    public static File getClientFile(ML4GExtension extension) {
        return new File(getGameDir(extension), extension.gameVersion + File.separator + extension.gameVersion + "-client.jar");
    }

    public static File getClientCleanFile(ML4GExtension extension) {
        return new File(getGameDir(extension), extension.gameVersion + File.separator + extension.gameVersion + "-client-clean.jar");
    }

    public static File getMappingDir(ML4GExtension extension) {
        File mapping = new File(extension.getUserCache(), "mapping");
        if (!mapping.exists()) {
            mapping.mkdir();
        }
        return mapping;
    }

    public static File getClientMappingFile(ML4GExtension extension) {
        File file = new File(getMappingDir(extension), extension.gameVersion + "-client.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static File getGameDir(ML4GExtension extension) {
        File game = new File(extension.getUserCache(), "game");
        if (!game.exists()) {
            game.mkdir();
        }
        return game;
    }

    public static String getClientJarSha1(String version) {
        return new Gson().fromJson(getJson(version), JsonObject.class).get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("sha1").getAsString();
    }

    public static byte[] getClientJar(String version) {
        return readFile(new Gson().fromJson(getJson(version), JsonObject.class).get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("url").getAsString());
    }

    public static String getClientMapping(String version) {
        return readString(new Gson().fromJson(getJson(version), JsonObject.class).get("downloads").getAsJsonObject().get("client_mappings").getAsJsonObject().get("url").getAsString());
    }

    public static String getClientMappingSha1(String version) {
        return new Gson().fromJson(getJson(version), JsonObject.class).get("downloads").getAsJsonObject().get("client_mappings").getAsJsonObject().get("sha1").getAsString();
    }

    public static File getClientAssetDir(ML4GExtension extension) {
        File assets = new File(extension.getUserCache(), "assets");
        if (!assets.exists()) {
            assets.mkdir();
        }
        return assets;
    }

    public static File getClientIndexDir(ML4GExtension extension) {
        File index = new File(getClientAssetDir(extension), "indexes");
        if (!index.exists()) {
            index.mkdir();
        }
        return index;
    }

    public static File getClientIndexFile(ML4GExtension extension) {
        File file = new File(GameUtil.getClientIndexDir(extension), extension.gameVersion + ".json");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static File getClientObjectFile(ML4GExtension extension, String name) {
        File file = new File(GameUtil.getClientObjectDir(extension), name.substring(0, 2));
        if (!file.exists()) {
            file.mkdir();
        }
        return new File(file, name);
    }

    public static File getLocalClientObjectFile(String name) {
        File file = new File(GameUtil.getLocalClientObjectDir(), name.substring(0, 2));
        if (!file.exists()) {
            file.mkdir();
        }
        return new File(file, name);
    }


    public static File getClientObjectDir(ML4GExtension extension) {
        File index = new File(getClientAssetDir(extension), "objects");
        if (!index.exists()) {
            index.mkdir();
        }
        return index;
    }

    public static File getLocalClientObjectDir() {
        return new File(getMinecraftDir(), "assets" + File.separator + "objects");
    }

    public static File getClientSkinDir(ML4GExtension extension) {
        File index = new File(getClientAssetDir(extension), "skins");
        if (!index.exists()) {
            index.mkdir();
        }
        return index;
    }

    public static String getClientAsset(String version) {
        return readString(new Gson().fromJson(getJson(version), JsonObject.class).get("assetIndex").getAsJsonObject().get("url").getAsString());
    }

    public static String getClientAssetSha1(String version) {
        return new Gson().fromJson(getJson(version), JsonObject.class).get("assetIndex").getAsJsonObject().get("sha1").getAsString();
    }

    public static boolean fileVerify(File file, String sha1) throws IOException {
        if (!file.exists()) {
            return false;
        }

        FileInputStream localJarStream = new FileInputStream(file);
        if (DigestUtils.sha1Hex(localJarStream).equals(sha1)) {
            localJarStream.close();
            return true;
        }

        localJarStream.close();
        return false;
    }
}
