package io.github.studentrentalsystem;

import org.json.JSONArray;
import org.json.JSONObject;

import static io.github.studentrentalsystem.Settings.nomic_embed_text;

public class EmbeddingClient {

    public static float[] getEmbedding(String query) {
        String response = LLMClient.callLocalModel(query, nomic_embed_text, "http://localhost:11434/api/embeddings");

        JSONObject parsed;
        try {
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            String jsonStr = response.substring(start, end + 1);
            parsed = new JSONObject(jsonStr);
        } catch (Exception e) {
            System.out.println("⚠️ 無法解析 JSON，原始內容：" + response);
            parsed = getDefaultUnknownJson();
        }


        JSONArray jsonArray = parsed.getJSONArray("embedding");
        float[] embedding = new float[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            embedding[i] = jsonArray.getFloat(i);
        }
        return embedding;
    }

    public static JSONObject getDefaultUnknownJson() {
        JSONObject obj = new JSONObject();
        obj.put("embedding", new float[]{});
        return obj;
    }

}
