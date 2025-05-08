package com.studentrental;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.studentrental.Settings.*;
import static com.studentrental.Utils.getStringJSON;


public class RentalExtractor {
    private static String promptTemplate;

    public RentalExtractor() throws IOException{
        parseResData();
    }

    private void parseResData() throws IOException {
        InputStream in = RentalExtractor.class.getClassLoader().getResourceAsStream(promptPath);
        promptTemplate = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        try {
            RentalExtractor rentalExtractor = new RentalExtractor();
            System.out.println(rentalExtractor.getJSONPost("育樂街185號/透天    \n【 格  局 】：兩間雅房，2F-1，2F-2，共用衛浴，一樓為黑工嫩仙草，二樓為玄關及兩間雅房一衛浴\n             雅房，2F-1，5.5坪(限男生)\n             雅房，2F-2，3.5坪(限男生)\n【 地  址 】：台南市東區育樂街185號…… 查看更多\n兩間雅房出租\nNT$1  · 701006\n育樂街185號/透天\n【 格 局 】：兩間雅房，2F-1，2F-2，共用衛浴，一樓為黑工嫩仙草，二樓為玄關及兩間雅房一衛浴\n雅房，2F-1，5.5坪(限男生)\n雅房，2F-2，3.5坪(限男生)\n【 地 址 】：台南市東區育樂街185號\n台南一中正門附近，黑工嫩仙草樓上\n【 設 備 】：雅房、套房內附有無線數位電視、變頻冷氣、衣櫃、電腦桌、床、電視櫃、網路，玄關附有監視攝影機、磁卡門鎖、飲水機，頂樓曬衣場附有投幣式洗衣機、免投幣脫水機、手洗衣物台\n【 費 用 】：雅房，2F-1，3500元/月(限男生)\n雅房，2F-2，3000元/月(限男生)\n無管理費，包水，不包電，\n一度5元，免網路費\n一次繳清半年，垃圾需自行清理\n【 押 金 】：4000\n【限制性別】：男\n【目前住戶】：目前整棟住戶包含雅房、套房，共男生 7人\n【 限 制 】：不可開伙，不可養寵物，若有特別在意的可以詢問房東\n【聯絡方式】：0932895832 張太太或0972013922張文慶\n【聯絡時間】：正常上班時間聯絡即可\n【房東概述】：房東人很好，偶爾還會請大家喝飲料，約3星期偶爾會過來打掃公共區域， 如果有什麼東西壞掉需要修理找她也會馬上幫同學解決，\n【備註說明】：(1)無電梯。",
                    llama3_8b));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getJSONPost(String post, String modelName) {
        String response = callLocalModel(post, modelName);
        JSONObject jsonObject = getStringJSON(response);
        jsonObject.put("坪數", correctRentalSize(jsonObject.getJSONArray("坪數")));
        return jsonObject;
    }


    private JSONArray correctRentalSize(JSONArray rentalsSize) {
        JSONArray revisedRental = new JSONArray();
        for (int j = 0; j < rentalsSize.length(); j++) {
            if (rentalsSize.getFloat(j) >= 100) {
                revisedRental.put(-1);
            } else {
                revisedRental.put(rentalsSize.getFloat(j));
            }
        }
        return revisedRental;
    }

    public void processPosts(String inputPath, String outputPath, String modelName) {
        try {
            // Read all posts from input JSON file
            InputStream in = RentalExtractor.class.getClassLoader().getResourceAsStream(inputPath);
            String inputJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JSONArray posts = new JSONArray(inputJson);
            List<JSONObject> results = new ArrayList<>();

            for (int i = 0; i < posts.length(); i++) {
                String post = posts.getString(i);
                System.out.println("⏳ 分析第 " + (i + 1) + " 筆貼文...");
                String response = callLocalModel(post, modelName);

                JSONObject parsed;
                try {
                    int start = response.indexOf("{");
                    int end = response.lastIndexOf("}");
                    String jsonStr = response.substring(start, end + 1);
                    parsed = new JSONObject(jsonStr);

                    parsed.put("坪數", correctRentalSize(parsed.getJSONArray("坪數")));
                    results.add(parsed);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("⚠️ 第 " + (i + 1) + " 筆無法解析 JSON，原始內容：" + response);

                    System.out.println("Reparsing Data");
                    i--;
                }


            }

            // Write results to output JSON file
            JSONArray outputJson = new JSONArray(results);
            Files.write(Paths.get(outputPath), outputJson.toString(2).getBytes(StandardCharsets.UTF_8));
            System.out.println("✅ 資料已輸出至 " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String callLocalModel(String text, String model) {
        String prompt = promptTemplate.replace("{text}", text);

        String response = LLMClient.callLocalModel(prompt, model, "http://localhost:11434/api/generate");

        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("response");
    }

    public JSONObject getDefaultUnknownJson() {
        JSONObject obj = new JSONObject();

        obj.put("地點", "未知");
        obj.put("租金", "未知");
        obj.put("坪數", "未知");
        obj.put("格局", "未知");
        obj.put("是否可養寵物", "未知");
        obj.put("可否開伙", "未知");
        obj.put("性別限制", "未知");
        obj.put("是否有電梯", "未知");
        obj.put("聯絡方式", "未知");
        obj.put("照片", "未知");
        return obj;
    }
}
