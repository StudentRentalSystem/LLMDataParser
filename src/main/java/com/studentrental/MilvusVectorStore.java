package com.studentrental;


import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.*;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MilvusVectorStore {

    private static final String COLLECTION_NAME = "rental_posts";
    private static MilvusServiceClient milvusClient;

    public static void connect() {
        if (milvusClient == null) {
            milvusClient = new MilvusServiceClient(
                    ConnectParam.newBuilder()
                            .withHost("localhost")
                            .withPort(19530)
                            .build()
            );
        }
    }

    public static void createCollectionIfNotExist() {
        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        var response = milvusClient.hasCollection(hasCollectionParam);

        if (response.getData() != null && !response.getData()) {
            milvusClient.createCollection(
                    CreateCollectionParam.newBuilder()
                            .withCollectionName(COLLECTION_NAME)
                            .withDescription("Rental Post Embeddings")
                            .withShardsNum(1)
                            .addFieldType(FieldType.newBuilder()
                                    .withName("id")
                                    .withDataType(DataType.Int64)
                                    .withPrimaryKey(true)
                                    .withAutoID(true)
                                    .build())
                            .addFieldType(FieldType.newBuilder()
                                    .withName("embedding")
                                    .withDataType(DataType.FloatVector)
                                    .withDimension(768)
                                    .build())
                            .addFieldType(FieldType.newBuilder()
                                    .withName("content")
                                    .withDataType(DataType.VarChar)
                                    .withMaxLength(1024) // 自訂 content 最大字數
                                    .build())
                            .build()
            );
        }
    }

    public static void insert(float[] embedding, String content) {
        List<InsertParam.Field> fields = new ArrayList<>();
        List<Float> listEmbedding = new ArrayList<>();
        for (float f : embedding) {
            listEmbedding.add(f);
        }
        fields.add(new InsertParam.Field("embedding", Collections.singletonList(listEmbedding)));
        fields.add(new InsertParam.Field("content", Collections.singletonList(content)));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFields(fields)
                .build();

        milvusClient.insert(insertParam);
        milvusClient.flush(FlushParam.newBuilder()
                .withCollectionNames(Collections.singletonList(COLLECTION_NAME))
                .withSyncFlush(true)
                .build());
    }

    public static List<SearchResultItem> searchReturn(List<Float> queryEmbedding, int topK) {
        loadCollectionIfNotLoaded(COLLECTION_NAME);

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withVectors(Collections.singletonList(queryEmbedding))
                .withVectorFieldName("embedding")
                .withTopK(topK)
                .withOutFields(Collections.singletonList("content"))
                .build();

        var response = milvusClient.search(searchParam);

//        System.out.println(response);

        if (response.getData() == null) {
            throw new RuntimeException("Search failed: response data is null.");
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());

        List<SearchResultItem> results = new ArrayList<>();
        List<String> contents = (List<String>) wrapper.getFieldWrapper("content").getFieldData();

        for (String obj : contents) {
            results.add(new SearchResultItem(obj));
        }
        return results;
    }


    public static void createIndexIfNotExist() {
        CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFieldName("embedding") // 針對 'embedding' 欄位建立索引
                .withIndexType(IndexType.IVF_FLAT) // 可以選擇其他索引類型：IVF_FLAT, IVF_SQ8, HNSW 等
                .withMetricType(MetricType.IP) // 使用內積（Inner Product），常用於向量搜尋
                .withExtraParam("{\"nlist\":128}") // 使用 .withExtraParam() 來傳遞額外的參數設定
                .build();

        milvusClient.createIndex(createIndexParam);
    }


    public static JSONArray searchMilvus(float[] queryEmbedding, int topK) {
        JSONArray resultArray = new JSONArray();
        List<Float> listEmbedding = new ArrayList<>();
        for (float f : queryEmbedding) {
            listEmbedding.add(f);
        }
        try {
            var searchResult = searchReturn(listEmbedding, topK);

            for (SearchResultItem item : searchResult) {
                JSONObject obj = new JSONObject();
                obj.put("content", item.getContent());
                resultArray.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultArray;
    }

    public static void printCollectionCount() {
        var stats = milvusClient.getCollectionStatistics(
                GetCollectionStatisticsParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .build()
        );
        System.out.println("Collection document count: " + stats.getData().getStats(0).getValue());
    }


    public static void loadCollectionIfNotLoaded(String collectionName) {
        var describeResp = milvusClient.describeCollection(
                DescribeCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );

        if (describeResp.getData() == null) {
            throw new RuntimeException("DescribeCollection failed: no data returned.");
        }

        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());
    }


    public static void dropCollection() {
        milvusClient.dropCollection(DropCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build());
    }

    public static void close() {
        if (milvusClient != null) {
            milvusClient.close();
            milvusClient = null;
        }
    }
}
