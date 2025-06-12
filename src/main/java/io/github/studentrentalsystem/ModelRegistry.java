package io.github.studentrentalsystem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class ModelRegistry {
    private static boolean load = false;
    private static final Set<String> availableModels = new HashSet<>();


    public static void loadModelsFromOllama(String address, int port) throws Exception {
        URL url = new URL(String.format("%s:%d/api/tags", address, port));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream()))) {
            String response = scanner.useDelimiter("\\A").next();
            JSONObject json = new JSONObject(response);
            JSONArray models = json.getJSONArray("models");
            for (int i = 0; i < models.length(); i++) {
                JSONObject model = models.getJSONObject(i);
                availableModels.add(model.getString("name"));
            }
        }
    }

    public static boolean isAvailable(String address, int port, String modelName) throws RuntimeException{
        try {
            if (!load) loadModelsFromOllama(address, port);
            load = true;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load models from Ollama", e);
        }
        return availableModels.contains(modelName);
    }

    public static Set<String> getAllModels() {
        return Set.copyOf(availableModels);
    }
}