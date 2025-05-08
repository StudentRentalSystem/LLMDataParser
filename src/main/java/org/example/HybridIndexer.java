package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.example.Settings.extractedDataPath;

public class HybridIndexer {
    private LuceneBM25Indexer indexer;

    public void insert() throws IOException {
        indexer = new LuceneBM25Indexer();
        indexer.clearIndex();
        MilvusVectorStore.connect();
        MilvusVectorStore.dropCollection();
        MilvusVectorStore.createCollectionIfNotExist();
        insertData();
        indexer.close();
        MilvusVectorStore.createIndexIfNotExist();
        MilvusVectorStore.close();
    }

    public void insertData() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(extractedDataPath);
        if (in == null) {
            throw new RuntimeException("找不到 " + extractedDataPath);
        }
        String jsonText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        JSONArray extractedData = new JSONArray(jsonText);

        for (int i = 0; i < extractedData.length(); i++) {
            JSONObject rentalInfo = extractedData.getJSONObject(i);
            System.out.println("🔵 正在處理第 " + (i + 1) + " 筆租屋資料...");

            StringBuilder textBuilder = new StringBuilder();
            for (String key : rentalInfo.keySet()) {
                Object obj = rentalInfo.get(key);
                textBuilder.append(key).append(": ");

                if (obj instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) obj;
                    textBuilder.append("[");
                    for (int j = 0; j < jsonArray.length(); j++) {
                        Object item = jsonArray.get(j);
                        if (item instanceof JSONObject) {
                            textBuilder.append(((JSONObject) item).toString()).append(", ");
                        } else {
                            textBuilder.append(item.toString()).append(", ");
                        }
                    }
                    textBuilder.append("], ");
                } else if (obj instanceof JSONObject) {
                    textBuilder.append(((JSONObject) obj).toString()).append(", ");
                } else {
                    textBuilder.append(obj.toString()).append(", ");
                }
            }

            String rentalText = textBuilder.toString();

            System.out.println(rentalText);

            // 產生 embedding
            float[] embedding = EmbeddingClient.getEmbedding(rentalText);

            // 插入到 Milvus
            MilvusVectorStore.insert(embedding, rentalText);

            indexer.insert(rentalText);

        }

    }
}
