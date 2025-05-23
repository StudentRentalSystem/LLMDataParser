package io.github.studentrentalsystem;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static io.github.studentrentalsystem.Settings.*;
import static io.github.studentrentalsystem.Utils.getStringJSON;


/**
 * <p>
 * This class is used to parse rental posts to structural json data <br>
 * @author hding4915
 */
public class RentalExtractor {
    private final int maxErrorTimes;
    private static String promptTemplate;


    /**
     * <p>
     * Default maxErrorTimes will be set to 5
     * @throws IOException if it gets error when reading the prompt file
     */
    public RentalExtractor() throws IOException{
        this(5);
    }


    /**
     *
     * @param maxErrorTimes set the maxErrorTimes for parsing the rental posts
     * @throws IOException if it gets error when reading the prompt file
     */
    public RentalExtractor(int maxErrorTimes) throws IOException{
        this.maxErrorTimes = maxErrorTimes;
        parseResData();
    }

    private void parseResData() throws IOException {
        InputStream in = RentalExtractor.class.getClassLoader().getResourceAsStream(promptPath);
        promptTemplate = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        String test_post = "育樂街185號/透天    \n【 格  局 】：兩間雅房，2F-1，2F-2，共用衛浴，一樓為黑工嫩仙草，二樓為玄關及兩間雅房一衛浴\n             雅房，2F-1，5.5坪(限男生)\n             雅房，2F-2，3.5坪(限男生)\n【 地  址 】：台南市東區育樂街185號…… 查看更多\n兩間雅房出租\nNT$1  · 701006\n育樂街185號/透天\n【 格 局 】：兩間雅房，2F-1，2F-2，共用衛浴，一樓為黑工嫩仙草，二樓為玄關及兩間雅房一衛浴\n雅房，2F-1，5.5坪(限男生)\n雅房，2F-2，3.5坪(限男生)\n【 地 址 】：台南市東區育樂街185號\n台南一中正門附近，黑工嫩仙草樓上\n【 設 備 】：雅房、套房內附有無線數位電視、變頻冷氣、衣櫃、電腦桌、床、電視櫃、網路，玄關附有監視攝影機、磁卡門鎖、飲水機，頂樓曬衣場附有投幣式洗衣機、免投幣脫水機、手洗衣物台\n【 費 用 】：雅房，2F-1，3500元/月(限男生)\n雅房，2F-2，3000元/月(限男生)\n無管理費，包水，不包電，\n一度5元，免網路費\n一次繳清半年，垃圾需自行清理\n【 押 金 】：4000\n【限制性別】：男\n【目前住戶】：目前整棟住戶包含雅房、套房，共男生 7人\n【 限 制 】：不可開伙，不可養寵物，若有特別在意的可以詢問房東\n【聯絡方式】：0932895832 張太太或0972013922張文慶\n【聯絡時間】：正常上班時間聯絡即可\n【房東概述】：房東人很好，偶爾還會請大家喝飲料，約3星期偶爾會過來打掃公共區域， 如果有什麼東西壞掉需要修理找她也會馬上幫同學解決，\n【備註說明】：(1)無電梯。";
        try {
            RentalExtractor rentalExtractor = new RentalExtractor();
//            System.out.println(rentalExtractor.getJSONPostNoError(test_post,
//                    LLMClient.ModelType.LLAMA3_8B));
            rentalExtractor.processPosts(rentalPostsPath, outputPath, LLMClient.ModelType.LLAMA3_8B);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>
     * This function will call local LLM model to generate structural json data
     * @param post the rental post
     * @param model the LLM model type to use
     * @see LLMClient.ModelType
     * @return type of {@link JSONObject} structural post
     */
    public JSONObject getJSONPost(String post, LLMClient.ModelType model) throws JSONException{
        String response = callLocalModel(post, model);
        JSONObject jsonObject = getStringJSON(response);
        jsonObject.put("坪數", correctRentalSize(jsonObject.getJSONArray("坪數")));
        return jsonObject;
    }

    private JSONObject noErrorJSONPost(String post, LLMClient.ModelType model) {
        String response  = callLocalModel(post, model);
        try {
            JSONObject jsonObject = getStringJSON(response);
            jsonObject.put("坪數", correctRentalSize(jsonObject.getJSONArray("坪數")));
            return jsonObject;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean checkFirstObject(JSONArray jsonArray) {
        return (jsonArray.length() == 1 && (jsonArray.getString(0).isEmpty() || jsonArray.getString(0).equals("未知")));
    }

    private void writeDefaultEmpty(JSONObject jsonObject, String key, Object value) {
        if (!jsonObject.has(key)) jsonObject.put(key, value);
    }

    /**
     * <p>
     * This function is used to adjust the structural json data to a fixed format
     * @param structural_post one rental post json object
     */
    public void formSameJSON(JSONObject structural_post) {
        JSONArray contact = structural_post.getJSONArray("聯絡方式");
        JSONArray result = new JSONArray();
        for (int i = 0; i < contact.length(); i++) {
            JSONObject personal_contact = contact.getJSONObject(i);
            writeDefaultEmpty(personal_contact, "聯絡人", "");
            writeDefaultEmpty(personal_contact, "手機", new JSONArray());
            writeDefaultEmpty(personal_contact, "lineID", new JSONArray());
            writeDefaultEmpty(personal_contact, "lineLink", new JSONArray());
            writeDefaultEmpty(personal_contact, "others", new JSONArray());

            String name = personal_contact.getString("聯絡人");
            JSONArray phone_nums = personal_contact.getJSONArray("手機");
            JSONArray lineID = personal_contact.getJSONArray("lineID");
            JSONArray lineLink = personal_contact.getJSONArray("lineLink");
            JSONArray others = personal_contact.getJSONArray("others");

            if (name.equals("未知")) name = "";
            if (checkFirstObject(phone_nums)) {
                phone_nums = new JSONArray();
            } else {
                JSONArray modified = new JSONArray();
                for (int j = 0; j < phone_nums.length(); j++) {
                    String phone = phone_nums.getString(j);
                    if (phone.contains("-")) modified.put(phone.replace("-", ""));
                    else modified.put(phone);
                }
                phone_nums = modified;
            }
            if (checkFirstObject(lineID)) lineID = new JSONArray();
            if (checkFirstObject(lineLink)) lineLink = new JSONArray();
            if (checkFirstObject(others)) others = new JSONArray();
            personal_contact = new JSONObject();
            personal_contact.put("聯絡人", name);
            personal_contact.put("手機", phone_nums);
            personal_contact.put("lineID", lineID);
            personal_contact.put("lineLink", lineLink);
            personal_contact.put("others", others);
            result.put(personal_contact);
        }
        structural_post.put("聯絡方式", result);
    }


    /**
     * <p>
     * This function is used to parse single rental post to structural json data. <br>
     * When getting errors, it will parse again until the error times >= maxErrorTimes.
     * @param post single rental post string
     * @param model the LLM model type to use
     * @see LLMClient.ModelType
     * @return structural json post, type of {@link JSONObject}
     */
    public JSONObject getJSONPostNoError(String post , LLMClient.ModelType model) {
        JSONObject jsonPost;
        int errorTimes = 0;
        while ((jsonPost = noErrorJSONPost(post, model)) == null && errorTimes < maxErrorTimes) {
            errorTimes++;
        }
        if (jsonPost != null) formSameJSON(jsonPost);
        return jsonPost;
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


    /**
     * <p>
     * This function is used to parse multiple rental posts to structural json data
     * @param inputPath input file path of the original rental posts, is a .json file that contains ["post1", "post2", ...]
     * @param outputPath output file path of the structural json data, is a .json file
     * @param model the LLM model type to use
     * @see LLMClient.ModelType
     */
    public void processPosts(String inputPath, String outputPath, LLMClient.ModelType model) {
        try {
            // Read all posts from input JSON file
            InputStream in = RentalExtractor.class.getClassLoader().getResourceAsStream(inputPath);
            String inputJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JSONArray posts = new JSONArray(inputJson);
            List<JSONObject> results = new ArrayList<>();

            for (int i = 0; i < posts.length(); i++) {
                String post = posts.getString(i);
                System.out.println("⏳ 分析第 " + (i + 1) + " 筆貼文...");

                JSONObject parsed = getJSONPostNoError(post, model);

                formSameJSON(parsed);

                results.add(parsed);
            }

            // Write results to output JSON file
            JSONArray outputJson = new JSONArray(results);
            Files.write(Paths.get(outputPath), outputJson.toString(2).getBytes(StandardCharsets.UTF_8));
            System.out.println("✅ 資料已輸出至 " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>
     * This function is used to get the LLM response when converting the rental post to structural json data.
     * @param text the rental post
     * @param model the LLM model type to use
     * @see LLMClient.ModelType
     * @return type of {@link String} response
     */
    public String callLocalModel(String text, LLMClient.ModelType model) {
        String prompt = promptTemplate.replace("{text}", text);

        String response = LLMClient.callLocalModel(prompt, model, "http://localhost:11434/api/generate");
        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("response");
    }
}
