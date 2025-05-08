package com.studentrental;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LuceneBM25Indexer {
    private static final String INDEX_DIR = "lucene_index";

    private final IndexWriter writer;

    public static class ScoredDocument {
        private final String content;
        private final float score;

        public ScoredDocument(String content, float score) {
            this.content = content;
            this.score = score;
        }

        public String content() {
            return content;
        }

        public float score() {
            return score;
        }
    }

    public LuceneBM25Indexer() throws IOException {
        Path path = Paths.get(INDEX_DIR);
        Directory directory = FSDirectory.open(path);
        Analyzer analyzer = new StandardAnalyzer();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        this.writer = new IndexWriter(directory, config);
    }

    public void insert(String content) throws IOException {
        Document doc = new Document();

        doc.add(new StringField("id", java.util.UUID.randomUUID().toString(), Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.YES));

        writer.addDocument(doc);
    }



    public List<ScoredDocument> search(String queryString, int topK) {
        List<ScoredDocument> results = new ArrayList<>();

        try (DirectoryReader reader = DirectoryReader.open(writer)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new StandardAnalyzer();
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(queryString);

            TopDocs topDocs = searcher.search(query, topK);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String content = doc.get("content");
                float score = scoreDoc.score;
                results.add(new ScoredDocument(content, score));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return results;
    }


    public void clearIndex() {
        try {
            writer.deleteAll();
            writer.commit(); // 確保刪除立即生效
            System.out.println("🔄 Lucene 索引已清空！");
        } catch (IOException e) {
            System.err.println("❌ 清空 Lucene 索引失敗！");
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        writer.close();
    }
}
