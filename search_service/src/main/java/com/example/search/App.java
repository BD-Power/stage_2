// search_service/src/main/java/com/example/search/App.java
package com.example.search;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {

    private static final Gson gson = new Gson();
    private static final String DATAMART_ROOT = "datamart/indexes";

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7003);

        app.get("/search", App::handleSearch);
    }

    private static void handleSearch(Context ctx) {
        String query = Optional.ofNullable(ctx.queryParam("q")).orElse("").toLowerCase();
        String authorFilter = ctx.queryParam("author");
        String languageFilter = ctx.queryParam("language");
        String yearStr = ctx.queryParam("year");

        Integer yearFilter = null;
        if (yearStr != null) {
            try {
                yearFilter = Integer.parseInt(yearStr);
            } catch (NumberFormatException ignored) {}
        }

        List<Map<String, Object>> results = new ArrayList<>();
        List<Path> jsonFiles = listJsonFiles(Paths.get(DATAMART_ROOT));

        for (Path jsonFile : jsonFiles) {
            try {
                String content = Files.readString(jsonFile, StandardCharsets.UTF_8);
                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> book = gson.fromJson(content, type);

                Optional<Map<String, Object>> filtered = buildResult(book, query, authorFilter, yearFilter);
                filtered.ifPresent(results::add);

            } catch (IOException e) {
                System.err.println("Error reading file " + jsonFile + ": " + e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("filters", buildFilters(authorFilter, languageFilter, yearFilter));
        response.put("count", results.size());
        response.put("results", results);

        ctx.result(gson.toJson(response));
    }

    private static Optional<Map<String, Object>> buildResult(Map<String, Object> book, String query, String authorFilter, Integer yearFilter) {
        if (book == null || !book.containsKey("metadata") || !book.containsKey("top_terms"))
            return Optional.empty();

        Map<String, Object> meta = (Map<String, Object>) book.get("metadata");
        Map<String, Double> topTerms = (Map<String, Double>) book.get("top_terms");

        // Filtros de autor y año
        if (authorFilter != null && meta.containsKey("author") && !meta.get("author").toString().equalsIgnoreCase(authorFilter))
            return Optional.empty();

        if (yearFilter != null && meta.containsKey("year") && !meta.get("year").toString().equals(String.valueOf(yearFilter)))
            return Optional.empty();

        // Buscar coincidencias (query puede tener varias palabras)
        String[] searchTerms = query.split("\\s+");
        Map<String, Double> matches = new LinkedHashMap<>();

        for (String term : searchTerms) {
            topTerms.forEach((word, freq) -> {
                if (word.toLowerCase().contains(term)) {
                    matches.put(word, freq);
                }
            });
        }

        if (!query.isEmpty() && matches.isEmpty()) return Optional.empty();

        // Armar resultado limpio
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("book_id", book.get("book_id"));
        result.put("title", meta.getOrDefault("title", "Unknown"));
        result.put("author", meta.getOrDefault("author", "Unknown"));
        result.put("matches", matches);

        return Optional.of(result);
    }

    private static List<Path> listJsonFiles(Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(p -> p.toString().endsWith(".json")).collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private static Map<String, Object> buildFilters(String author, String lang, Integer year) {
        Map<String, Object> filters = new HashMap<>();
        if (author != null) filters.put("author", author);
        if (lang != null) filters.put("language", lang);
        if (year != null) filters.put("year", year);
        return filters;
    }
}
