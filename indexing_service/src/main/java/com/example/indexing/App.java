// indexing_service/src/main/java/com/example/indexing/App.java
package com.example.indexing;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {

    private static final Gson gson = new Gson();
    private static final String DATALAKE_ROOT = "datalake";
    private static final String DATAMART_ROOT = "datamart";
    private static int booksIndexed = 0;
    private static String lastUpdate = "";

    public static void main(String[] args) {
        Javalin app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = "application/json";
        }).start(7002);

        app.get("/status", ctx -> {
            JsonObject st = new JsonObject();
            st.addProperty("books_indexed", booksIndexed);
            st.addProperty("last_update", lastUpdate.isEmpty() ? "never" : lastUpdate);
            ctx.result(gson.toJson(st));
        });

        app.post("/index/update/{book_id}", App::handleUpdateBook);
        app.post("/index/rebuild", App::handleRebuild);
    }

    private static void handleUpdateBook(Context ctx) {
        String idStr = ctx.pathParam("book_id");
        try {
            int bookId = Integer.parseInt(idStr);
            Optional<Path> rawPath = findRawFile(bookId);
            if (rawPath.isEmpty()) {
                ctx.status(404).result(gson.toJson(Map.of("error", "Book not found in datalake")));
                return;
            }
            processBook(bookId, rawPath.get());
            ctx.result(gson.toJson(Map.of("book_id", bookId, "index", "updated")));
        } catch (NumberFormatException e) {
            ctx.status(400).result(gson.toJson(Map.of("error", "Invalid book_id")));
        } catch (IOException e) {
            ctx.status(500).result(gson.toJson(Map.of("error", e.getMessage())));
        }
    }

    private static void handleRebuild(Context ctx) {
        Set<Path> raws = findAllRawFiles();
        int count = 0;
        long start = System.currentTimeMillis();
        for (Path p : raws) {
            try {
                String name = p.getParent().getFileName().toString();
                int id = Integer.parseInt(name);
                processBook(id, p);
                count++;
            } catch (Exception ignored) {}
        }
        booksIndexed = count;
        lastUpdate = Instant.now().toString();
        double elapsed = (System.currentTimeMillis() - start) / 1000.0;
        ctx.result(gson.toJson(Map.of("books_processed", count, "elapsed_time", String.format("%.2fs", elapsed))));
    }

    private static Optional<Path> findRawFile(int bookId) {
        try (Stream<Path> s = Files.walk(Paths.get(DATALAKE_ROOT))) {
            return s.filter(p -> p.getFileName().toString().equals("raw.txt")
                            && p.getParent().getFileName().toString().equals(String.valueOf(bookId)))
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Set<Path> findAllRawFiles() {
        try (Stream<Path> s = Files.walk(Paths.get(DATALAKE_ROOT))) {
            return s.filter(p -> p.getFileName().toString().equals("raw.txt")).collect(Collectors.toSet());
        } catch (IOException e) {
            return Set.of();
        }
    }

    private static void processBook(int bookId, Path rawPath) throws IOException {
        String text = Files.readString(rawPath, StandardCharsets.UTF_8);
        // Extraer metadatos básicos
        Map<String, String> meta = extractMetadata(text);

        // Tokenizar y contar
        Map<String, Long> freq = Arrays.stream(text.toLowerCase().split("[^a-záéíóúüñ]+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        // Top 20 palabras
        LinkedHashMap<String, Long> top = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new
                ));

        Path indexDir = Paths.get(DATAMART_ROOT, "indexes");
        Files.createDirectories(indexDir);
        Path outFile = indexDir.resolve(bookId + ".json");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("book_id", bookId);
        out.put("metadata", meta);
        out.put("top_terms", top);
        out.put("last_indexed", Instant.now().toString());

        Files.writeString(outFile, gson.toJson(out), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static Map<String, String> extractMetadata(String text) {
        Map<String, String> m = new HashMap<>();
        String[] lines = text.split("\n");
        for (String l : lines) {
            if (l.toLowerCase().startsWith("title:")) m.put("title", l.substring(6).trim());
            if (l.toLowerCase().startsWith("author:")) m.put("author", l.substring(7).trim());
            if (m.size() >= 2) break;
        }
        if (!m.containsKey("title")) m.put("title", "Unknown");
        if (!m.containsKey("author")) m.put("author", "Unknown");
        return m;
    }
}
