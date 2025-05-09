package io.github.studentrentalsystem;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static io.github.studentrentalsystem.Settings.*;
import static io.github.studentrentalsystem.Utils.getStringJSON;


public class MiniRagApp {
    private static String ragPromptTemplate;
    private static String queryPromptTemplate;
    private static String mongoDBQueryPromptTemplate;
    private static final int topK = 20;
    private final boolean stream = false;


    private void parseRagPrompt() throws IOException {
        InputStream in = MiniRagApp.class.getClassLoader().getResourceAsStream(ragPromptPath);
        if (in == null) {
            throw new FileNotFoundException("Cannot find rag_prompt.txt！");
        }
        ragPromptTemplate = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void parseQueryPrompt() throws IOException {
        InputStream in = MiniRagApp.class.getClassLoader().getResourceAsStream(queryPromptPath);
        if (in == null) {
            throw new FileNotFoundException("Cannot find query_prompt.txt!");
        }
        queryPromptTemplate = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void parseMongoDBQueryPrompt() throws IOException {
        InputStream in = MiniRagApp.class.getClassLoader().getResourceAsStream(mongoDBQueryPromptPath);
        if (in == null) {
            throw new FileNotFoundException("Cannot find mongo_query_prompt.txt!");
        }
        mongoDBQueryPromptTemplate = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    public MiniRagApp() {
        try {
            parseRagPrompt();
            parseQueryPrompt();
            parseMongoDBQueryPrompt();
        } catch (IOException e) {
            throw new RuntimeException("Read Rag prompt or Query prompt error: " + e.getMessage());
        }
    }

    public String formatQuery(String query) {
        String formattedQueryPrompt = queryPromptTemplate.replace("{query}", query);
//        System.out.println(formattedQueryPrompt);
        return LLMClient.callLocalModel(formattedQueryPrompt, llama3_8b, "http://localhost:11434/api/generate");
    }


    public String formatMongoDBQuery(String query) {
        String formattedMongoDBQueryPrompt = mongoDBQueryPromptTemplate.replace("{query}", query);

        return LLMClient.callLocalModel(formattedMongoDBQueryPrompt, llama3_8b, "http://localhost:11434/api/generate");
    }



    private void printSearchResults(String question) throws IOException {
        HybridSearcher searcher = new HybridSearcher();
        JSONArray jsonArray = searcher.hybridSearch(question, topK, 0.6f);
        System.out.println(jsonArray.toString());
    }




    public String getMongoDBSearchCmd(String query) {
        try {
            query = formatMongoDBQuery(query);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return query;
    }

    public String miniRag(String query) {
        try {
            query = formatQuery(query);

            JSONObject responseBody = new JSONObject(query);

            query = responseBody.getString("response");

            System.out.println(query);

            JSONObject jsonResponse = getStringJSON(query);

            query = jsonResponse.toString();

//            System.out.println(question);

            HybridSearcher searcher = new HybridSearcher();
            JSONArray relatedDocs = searcher.hybridSearch(query, topK, 0.6f);
            System.out.println(relatedDocs.toString());
            searcher.close();

            StringBuilder contextBuilder = new StringBuilder();
            for (int i = 0; i < relatedDocs.length(); i++) {
                JSONObject doc = relatedDocs.getJSONObject(i);
                contextBuilder.append(doc.getString("content")).append("\n");
            }

            String context = contextBuilder.toString();

            String finalPrompt = ragPromptTemplate
                    .replace("{context}", context)
                    .replace("{question}", query);

            return LLMClient.callLocalModel(finalPrompt, llama3_8b, "http://localhost:11434/api/generate", stream);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        MiniRagApp miniRag = new MiniRagApp();

        String response = "";

        try {
            MilvusVectorStore.connect();

            MilvusVectorStore.printCollectionCount();

            Scanner scanner = new Scanner(System.in);

            System.out.println("MiniRAG 啟動！請輸入租屋需求（輸入 exit 離開）：");

            while (true) {
                System.out.print("\n請輸入租屋需求：");
                String userQuery = scanner.nextLine();
                if (userQuery.equalsIgnoreCase("exit")) {
                    break;
                }

                // Get result according to the hybrid search

//                response = miniRag.miniRag(userQuery);

                // Get mongoDB result
                response = miniRag.getMongoDBSearchCmd(userQuery);

                if (miniRag.stream) continue;

                if (response == null || response.isEmpty()) continue;


                JSONObject responseBody = new JSONObject(response);

                response = responseBody.getString("response");

                response = getStringJSON(response).toString();

                System.out.println(response);

            }

            MilvusVectorStore.close();
            System.out.println("👋 再見！");
        } catch (Exception e) {
            if (response != null) System.out.println(response);
            e.printStackTrace();
        }
    }


}
