package cn.enaium.ml4g.util;

import org.apache.commons.io.IOUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * @author Enaium
 */
public class UrlUtil {
    public static final String GAME_URL = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
    public static final String GAME_LIBRARIES = "https://libraries.minecraft.net/";
    public static final String GAME_RESOURCE = "https://resources.download.minecraft.net";

    public static String readString(String link) {

        StringBuilder stringBuilder = new StringBuilder();
        try {
            URL url = new URL(link);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection connection = null;
            if (urlConnection instanceof HttpURLConnection) {
                connection = (HttpURLConnection) urlConnection;
            }

            IOUtils.readLines(connection.getInputStream(), StandardCharsets.UTF_8).forEach(line -> stringBuilder.append(line).append("\n"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    public static byte[] readFile(String link) {
        byte[] bytes = null;
        try {
            URL url = new URL(link);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection connection = null;
            if (urlConnection instanceof HttpURLConnection) {
                connection = (HttpURLConnection) urlConnection;
            }

            bytes = IOUtils.toByteArray(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
