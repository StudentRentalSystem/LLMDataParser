package com.studentrental;


import org.apache.lucene.queryparser.classic.QueryParser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class HybridSearcher {

    private final LuceneBM25Indexer indexer;

    public HybridSearcher() throws IOException {
        this.indexer = new LuceneBM25Indexer(); // 預設從同一索引路徑開啟
        MilvusVectorStore.connect();
    }

    public JSONObject getStringJSON(String str) {
        int start = str.indexOf("{");
        int end = str.indexOf("}");
        if (start == -1 || end == -1) {
            return null;
        }
        str = str.substring(start, end + 1);
        return new JSONObject(str);
    }

    public JSONArray hybridSearch(String query, int topK, float alpha) {
        // alpha ∈ [0, 1] 控制向量與關鍵字比重：alpha 越大越偏語意向量

        // 1. 向量嵌入查詢（語意）
        float[] embedding = EmbeddingClient.getEmbedding(query);
        JSONArray milvusResults = MilvusVectorStore.searchMilvus(embedding, topK);
        Map<String, Float> vectorScores = new HashMap<>();
        for (int i = 0; i < milvusResults.length(); i++) {
            String content = milvusResults.getJSONObject(i).getString("content");
            vectorScores.put(content, (float)(topK - i)); // 模擬分數（越前面越高）
        }

        // 2. 從 JSON 查詢中構造 Lucene 查詢語句
        JSONObject jsonStr = new JSONObject(query);
        StringBuilder luceneQueryBuilder = new StringBuilder();

        if (!jsonStr.getString("地址").isEmpty()) {
            luceneQueryBuilder.append("地址:" + QueryParser.escape(jsonStr.getString("地址")) + " ");
        }

        JSONArray rentArr = jsonStr.getJSONArray("租金");
        for (int i = 0; i < rentArr.length(); i++) {
            JSONObject rentObj = rentArr.getJSONObject(i);

            String rent = "maxRental: " + rentObj.getString("maxRental") + ", minRental: " + rentObj.getString("minRental");
            if (!rent.isEmpty()) {
                luceneQueryBuilder.append("租金:" + QueryParser.escape(rent) + " ");
            }
        }

        JSONArray areaArr = jsonStr.getJSONArray("坪數");
        for (int i = 0; i < areaArr.length(); i++) {
            String area = areaArr.getString(i);
            if (!area.isEmpty()) {
                luceneQueryBuilder.append("坪數:" + QueryParser.escape(area) + " ");
            }
        }

        JSONObject layout = jsonStr.getJSONObject("格局");
        for (String key : layout.keySet()) {
            int count = layout.getInt(key);
            if (count > 0) {
                luceneQueryBuilder.append("格局:" + key + count + " ");
            }
        }

        String[] otherKeys = {"可養寵物", "可養魚", "可開伙", "有電梯"};
        for (String key : otherKeys) {
            if (jsonStr.has(key)) {
                String value = jsonStr.optString(key, "");
                if (!value.isEmpty() && !value.equals("未知")) {
                    luceneQueryBuilder.append(key + ":" + QueryParser.escape(value) + " ");
                }
            }
        }


        // 其他要求可以當成全文關鍵詞加入查詢
        String extra = jsonStr.getString("其他要求");
        if (!extra.isEmpty()) {
            luceneQueryBuilder.append(QueryParser.escape(extra));
        }

        String luceneQuery = luceneQueryBuilder.toString().trim();

        // 3. 關鍵字查詢（BM25）
        List<LuceneBM25Indexer.ScoredDocument> bm25Results = indexer.search(luceneQuery, topK);
        Map<String, Float> bm25Scores = new HashMap<>();
        for (LuceneBM25Indexer.ScoredDocument doc : bm25Results) {
            bm25Scores.put(doc.content(), doc.score());
        }

        // 4. 合併兩者
        Set<String> allContents = new HashSet<>();
        allContents.addAll(vectorScores.keySet());
        allContents.addAll(bm25Scores.keySet());

        PriorityQueue<JSONObject> resultQueue = new PriorityQueue<>(Comparator.comparingDouble(o -> -o.getDouble("score")));

        for (String content : allContents) {
            float vectorScore = vectorScores.getOrDefault(content, 0f);
            float bm25Score = bm25Scores.getOrDefault(content, 0f);
            float hybridScore = alpha * vectorScore + (1 - alpha) * bm25Score;

            JSONObject obj = new JSONObject();
            obj.put("content", content);
            obj.put("score", hybridScore);
            resultQueue.add(obj);
        }

        // 5. 回傳前 topK 筆
        JSONArray results = new JSONArray();
        int count = 0;
        while (!resultQueue.isEmpty() && count < topK) {
            results.put(resultQueue.poll());
            count++;
        }

        return results;
    }


    public void close() throws IOException {
        indexer.close();
        MilvusVectorStore.close();
    }
}
