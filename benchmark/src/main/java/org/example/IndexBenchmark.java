package org.example;

import org.openjdk.jmh.annotations.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class IndexBenchmark {

    private String text;
    private Map<String, List<Integer>> invertedIndex;
    private List<Map<String, Object>> mockSearchResults;
    private final String MOCK_TERM = "pride";

    @Setup(Level.Trial)
    public void setup() throws Exception {


        try (Stream<Path> paths = Files.walk(Path.of("datalake"))) {
            Optional<Path> raw = paths
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase("raw.txt"))
                    .findFirst();

            if (raw.isPresent()) {
                System.out.println("Using real text from: " + raw.get());
                text = Files.readString(raw.get());
            } else {
                System.out.println("Not found raw.txt — using synthetic text.");
                text = "Sample benchmark text ".repeat(4000);
            }
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
            if ("Jane Austen".equals(author) && year == 1813) {
                filteredAndRanked.add(result);
            }
        }
        return filteredAndRanked;
    }
}