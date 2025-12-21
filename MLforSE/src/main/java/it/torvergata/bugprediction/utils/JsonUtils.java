package it.torvergata.bugprediction.utils;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class JsonUtils {

    private JsonUtils() {}

    public static JSONObject readJsonFromUrl(String url) throws IOException {
        URLConnection conn = new URL(url).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        try (InputStream is = conn.getInputStream();
             Reader rd = new InputStreamReader(is, StandardCharsets.UTF_8)) {

            return new JSONObject(new JSONTokener(rd));
        }
    }

}
