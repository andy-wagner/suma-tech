package de.suma.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class Searcher {

    // Top-k-Ranking berechnen
    public static final int k = 10;

    private IndexSearcher indexSearcher;

    private IndexReader indexReader;

    public Searcher() throws IOException {
        indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(Indexer.INDEX_DIR)));
        indexSearcher = new IndexSearcher(indexReader);
    }

    public void search() throws IOException {

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        while (true) {
            System.out.println("Suchanfrage eingeben (oder ENTER zum Beenden): ");
            String queryString = in.readLine();
            if ("".equals(queryString)) {
                break;
            }

            // Angabe des Indexfeld, das für die Suche verwendet werden soll
            QueryParser queryParser = new QueryParser(Indexer.FIELD_CONTENT, new StandardAnalyzer());
            Query q;
            try {
                q = queryParser.parse(queryString);
                System.out.println("ausgeführte Suchanfrage: " + q.toString());
            } catch (ParseException e) {
                System.err.println("Unerwarteter Fehler beim Parsen der Suchanfrage: " + e.getMessage());
                continue;
            }

            TopDocs results = indexSearcher.search(q, k);
            System.out.println("Es wurden " + results.totalHits + " Treffer gefunden");


            // die einzelnen Suchtreffer im Top-k-Ranking ausgeben
            ScoreDoc[] hits = results.scoreDocs;

            if (hits.length > 0) {

                System.out.println("Ausgabe des Top-" + k + "-Rankings");
                int rank = 1;

                for (ScoreDoc hit : hits) {

                    System.out.println("Treffer Nr. " + rank++);

                    Document document = indexSearcher.doc(hit.doc);

                    String id = document.get("id");
                    System.out.println("\tDoc ID: " + id);

                    float score = hit.score;
                    System.out.println("\tScore: " + score);

                    String fileName = document.get("filename");
                    System.out.println("\tFile Name: " + fileName);
                }
            }
        }
    }

    public void printStats() throws IOException {
        System.out.println("Anzahl Dokument im Index: " + indexReader.numDocs());

        for (String fieldName : new String[] {Indexer.FIELD_ID, Indexer.FIELD_FILENAME, Indexer.FIELD_CONTENT}) {

            System.out.println("Indexfeld " + fieldName);
            CollectionStatistics collectionStatistics = indexSearcher.collectionStatistics(fieldName);
            System.out.println("Summe aller df-Werte: " + collectionStatistics.sumDocFreq());
            System.out.println("Summe aller tf-Werte: " + collectionStatistics.sumTotalTermFreq());
        }

        System.out.println("Ausgabe einiger df-Werte für das Indexfeld " + Indexer.FIELD_CONTENT);
        for (String termStr : new String[] {"caesar", "calpurnia", "brutus"}) {
            Term term = new Term(Indexer.FIELD_CONTENT, termStr);
            System.out.println("df('" + termStr + "') = " + indexReader.docFreq(term));
            System.out.println("total-tf('" + termStr + "') = " + indexReader.totalTermFreq(term));
        }
    }

    public static void main(String[] args) throws IOException {

        System.out.println("Suche auf dem zuvor generierten Lucene-Index im Verzeichnis " + Indexer.INDEX_DIR);

        Searcher searcher = new Searcher();

        // Ausgabe einiger statistischer Informationen über den verwendeten Lucene-Index
        searcher.printStats();

        searcher.search();

        searcher.indexReader.close();
    }

}
