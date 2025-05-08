package com.studentrental;

import java.io.IOException;

public class HybridInserter {

    public static void main(String[] args) {
        try {
            HybridIndexer indexer = new HybridIndexer();
            indexer.insert();
        } catch (IOException e) {
            System.out.println("Insert data failed: " + e.getMessage());
        }
    }

}
