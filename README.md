## 🤖 LLM Data Parser

一套整合 **Ollama 本地推論服務** 的 Java API，提供快速呼叫本地 LLM 模型並處理自然語言資料的能力，設計用於租屋資料結構化等應用。

專案名稱：**LLMDataParser**

---

### 📦 功能簡介

* 📡 呼叫本機 Ollama 推論 API（支援模型如：`llama3:8b`, `mistral`）
* ⚙️ 支援多模型切換與配置
* 🧠 搭配租屋社團貼文格式進行 LLM 回應解析
* ✅ 模組化設計，易於整合至爬蟲或分析系統中

---

### 📁 專案結構

```
LLMDataParser/
├── .github/               # GitHub Actions 或 CI 設定
├── .gradle/               # Gradle 快取資料夾
├── .idea/                 # IntelliJ 專案資料
├── build/                 # 編譯後的產出檔案
├── gradle/                # Gradle Wrapper 設定
├── src/
│   └── main/
│       └── java/io/github/studentrentalsystem/
│           ├── LLMAPIRules.java       # 回應格式規則定義
│           ├── LLMClient.java         # 呼叫 Ollama 推論 API
│           ├── LLMConfig.java         # 設定檔與模型參數
│           └── ModelRegistry.java     # 模型管理
├── .gitignore
├── build.gradle.kts        # Gradle 設定檔
├── gradle.properties       # Gradle 屬性檔
├── gradlew / gradlew.bat   # Gradle 執行腳本
├── settings.gradle.kts     # 專案設定
├── README.md               # 說明文件（本檔案）
```

---

### ⚙️ 設定方式

1. 確保安裝 [Ollama](https://ollama.com/) 並執行：

```bash
ollama run llama3:8b
```

2. 設定模型與推論參數於 `LLMConfig.java`
```java
public LLMConfig(LLMMode mode, 
                 String serverAddress, 
                 int serverPort,
                 String modelType, 
                 boolean stream, 
                 BlockingQueue<LLMClient.StreamData> queue
) { ... }
```
- mode: CHAT, GENERATE, EMBEDDINGS
- serverAddress: your server address
- serverPort: your server port
- modelType: like "llama3:8b", "mistral", ...
- stream: used to specify whether to show LLM response instantly or not
- queue: used to receive response when stream mode is true


```java
LLMConfig config = new LLMConfig(
        LLMConfig.LLMMode.CHAT,
        "Your server address",
        "Your server port",
        "Your model type",
        false,
        null
);
```

---

### 🚀 使用方式

在 `LLMClient.java` 中可使用：

```java
LLMClient client = new LLMClient(config);
String prompt = "請將以下租屋貼文轉為 JSON 結構...";
String result = client.callLocalModel(prompt);
```

搭配 `LLMAPIRules` 對輸出進行正規化或過濾。

---

### 🧠 應用場景

* 租屋社團自然語言貼文結構化（配合 Facebook Crawler 使用）
* 聊天記錄解析
* 文字分類與摘要

---

### 📄 授權

MIT License

---

### ✨ 貢獻者

開發者：[@hding4915](https://github.com/hding4915)
