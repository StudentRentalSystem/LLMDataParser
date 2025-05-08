# 📦 租屋資訊問答系統（RAG 機制）

本專案是一個基於 **RAG（Retrieval-Augmented Generation）機制** 的租屋問答系統，透過大語言模型與語意檢索技術，讓使用者可使用自然語言查詢租屋資訊，並取得 AI 理解與篩選後的回答。

---

## 🧠 系統機制說明

整體系統機制包含以下流程：

### 1️⃣ 資料萊描與結構化

- 貼文來源：Facebook 租屋社團
- 使用 `LLaMA3-8B-Instruct` 模型 將原始貼文轉為結構化 `JSON` 格式
- 統一格式範例如下：

```json
{
  "地點": "台南市北區勝利路206巷",
  "租金": ["5500元/月"],
  "坪數": ["4.5坪"],
  "格局": {"房": 1, "廳": 0, "衛": 0},
  "可養寵物": "否",
  "可養魚": "未知",
  "可開伙": "未知",
  "有電梯": "未知",
  "聯絡方式": [{"聯絡人": "張先生", "手機": ["0912345678"], "lineID": ["zline123"]}],
  "照片": ["..."]
}
```

---

### 2️⃣ 向量化與儲存

- 使用 `nomic-embed-text` 對每筆資料進行語意向量生成 (embedding)
- 會先把每筆 JSON 轉換成可閱讀的文字故事描述後進行 embedding
- 使用 `Milvus` 向量資料庫 儲存向量與原始描述文字

---

### 3️⃣ 問答查詢流程（RAG）

1. 使用者輸入自然語言問題
    - 例如：「我要找租金 10000 以下可以開伙的房子」

2. 使用 `LLaMA3-8B-Instruct` 模型轉換為結構化 JSON 條件
    - 例：
      ```json
      {
        "租金": ["$10000元/月"],
        "可開伙": "是"
      }
      ```

3. 使用使用者問題產生向量
    - 透過 `nomic-embed-text` 產生問題向量

4. 在 Milvus 中做 topK 相似度查詢（語意搜尋）
    - 目前設為 topK = 20 筆

5. 由 LLM 根據查詢到的 20 筆房源，結合使用者需求產生自然語言回應

---

## 🗂️ 專案檔案結構

```
FacebookCrawlingDataFormatter/
├── .gradle/
├── .idea/
├── build/
├── gradle/
├── src/
│   └── main/
│       ├── java/org/example/
│       │   ├── EmbeddingClient.java
│       │   ├── LLMClient.java
│       │   ├── MilvusInserter.java
│       │   ├── MilvusVectorStore.java
│       │   ├── MiniRagApp.java
│       │   ├── RentalExtractor.java
│       │   ├── SearchResultItem.java
│       │   └── Settings.java
│       └── resources/
│           ├── extract_prompt.txt
│           ├── extracted_data.json
│           ├── query_prompt.txt
│           ├── rag_prompt.txt
│           └── rental_posts.json
├── text/
├── volumes/
├── build.gradle.kts
├── docker-compose.yml
├── embedtxtd.yaml
├── gradlew
├── gradlew.bat
├── README.md
├── settings.gradle.kts
├── standalone.bat
└── user.yaml
```
---

## 🚀 使用說明

### Step 0: 工具下載與環境建置

專案開始前，請先安裝並執行以下工具與步驟：

1. **安裝 Docker Desktop**：
    - 前往 https://www.docker.com/products/docker-desktop 下載並安裝 Docker。
    - 安裝後請確保 Docker 服務正常執行。

2. **Milvus 環境建置**： 
    - 在管理員模式下開啟 Docker Desktop, 方法是按滑鼠右鍵並選擇以管理員身分執行。 
   下載安裝指令碼並儲存為standalone.bat。
      ```bash
      C:\>Invoke-WebRequest https://raw.githubusercontent.com/milvus-io/milvus/refs/heads/master/scripts/standalone_embed.bat -OutFile standalone.bat
      ```

    - 使用專案中的 `standalone.bat` 自動啟動 Milvus docker 環境：
      ```bash
      ./standalone.bat start
      ```
    - 在 docker desktop 尋找剛剛配置的 milvus 容器, 尋找 Files 選項, 找到 milvus/config/milvus.yaml, 將 enables 欄位設置為 false
      ```yaml
      quotaAndLimits:
      enabled: false # `true` to enable quota and limits, `false` to disable.
      # quotaCenterCollectInterval is the time interval that quotaCenter
      # collects metrics from Proxies, Query cluster and Data cluster.
      # seconds, (0 ~ 65536)
      quotaCenterCollectInterval: 3
      ```

3. **Ollama 安裝與模型下載**：
    - 前往 https://ollama.com/ 下載安裝 Ollama 工具。
    - 執行下列指令下載模型：
      ```bash
      ollama pull llama3:8b
      ollama pull nomic-embed-text
      ```
    - `llama3:8b` 用於自然語言轉換成結構化資料。
    - `nomic-embed-text` 用於轉成向量後存入 Milvus。


### Step 1：準備資料
- 使用 `LLaMA3-8B-Instruct` 模型將 `rental_posts.json` 的 Facebook 貼文轉換成 `extracted_data.json`
- 每筆資料皆為標準格式的 JSON

### Step 2：插入資料進 Milvus

執行：
```bash
java org.example.HybridInserter
```
或自定義執行腳本，將結構化資料轉向量並儲存

### Step 3：查詢流程

- 使用者輸入問題（如：「有無 10000 元以下能養貓的房子？」）
- 使用 `LLaMA3-8B-Instruct` 模型產生 JSON 條件
- 使用問題向量進行 Milvus 查詢
- 將查詢結果與條件整合後使用 `Mistral` 模型回答

---

## 🔍 使用技術與工具

- 語言模型：Mistral、LLaMA3-8B-Instruct
- 嵌入模型：`nomic-embed-text`
- 向量資料庫：`Milvus`
- 開發語言：Java (資料處理、插入)
- 查詢邏輯：RAG + Top-K 検索

---

## 📌 注意事項

- Milvus 中的資料是使用完整文字描述進行 embedding，而非結構化欄位查詢，因此無法使用條件篩選（如金額 < 10000）
- 若需要更進一步的條件比對，可搭配結構化資料做 post-filtering (未實作)

---

## 👨‍💼 作者資訊

作者：hding4915
版本：v1.0

