package org.example;

import org.openjdk.jmh.annotations.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class IndexBenchmark {

    private String text;
    private Map<String, List<Integer>> invertedIndex;
    private List<Map<String, Object>> mockSearchResults;
    private final String MOCK_TERM = "pride";

    @Setup(Level.Trial)
    public void setup() throws IOException {
        text = Files.readString(Path.of("datalake/sample.txt"));
        if (text.isEmpty()) {
            text = "Sample text for benchmark simulation repeated ".repeat(2000);
        }

        invertedIndex = new HashMap<>();
        invertedIndex.put(MOCK_TERM, Arrays.asList(1342, 5, 17, 42, 100, 201));

        mockSearchResults = List.of(
                Map.of("id", 1342, "author", "Jane Austen", "year", 1813, "title", "Pride and Prejudice"),
                Map.of("id", 5, "author", "Daniel Defoe", "year", 1719, "title", "Robinson Crusoe"),
                Map.of("id", 17, "author", "Jane Austen", "year", 1815, "title", "Sense and Sensibility"),
                Map.of("id", 42, "author", "Unknown", "year", 1900, "title", "A Mystery")
        );
    }

    @Benchmark
    public int testTokenization() {
        String[] tokens = text.split("\\W+");
        return tokens.length;
    }

    @Benchmark
    public long testCharacterCount() {
        return text.chars().filter(Character::isLetterOrDigit).count();
    }

    @Benchmark
    public void testWriteToDatamart() throws IOException {
        Path output = Path.of("datamart/tmp/result.txt");
        Files.createDirectories(output.getParent());
        Files.writeString(output, "benchmark completed");
    }

    @Benchmark
    public void testMetadataInsertion() {
        int bookId = 1342;
        String title = "Pride and Prejudice";
        String author = "Jane Austen";
        int year = 1813;
    }

    @Benchmark
    public int testIndexLookup() {
        List<Integer> postingList = invertedIndex.get(MOCK_TERM);
        return postingList != null ? postingList.size() : 0;
    }

    @Benchmark
    public List<Map<String, Object>> testQueryFilteringAndRanking() {
        List<Map<String, Object>> filteredAndRanked = new ArrayList<>();
        for (Map<String, Object> result : mockSearchResults) {
            String author = (String) result.get("author");
            Integer year = (Integer) result.get("year");
            if ("Jane Austen".equals(author) && year != null && year == 1813) {
                filteredAndRanked.add(result);
            }
        }
        return filteredAndRanked;
    }
}
