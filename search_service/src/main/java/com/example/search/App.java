// search_service/src/main/java/com/example/search/App.java
package com.example.search;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.*;

public class App {
    private static final Gson gson = new Gson();

    // Datos de ejemplo (en la versión real, esto vendría del datamart)
    private static final List<Map<String, Object>> MOCK_BOOKS = Arrays.asList(
            Map.of("book_id", 1342, "title", "Pride and Prejudice", "author", "Jane Austen", "language", "en", "year", 1813),
            Map.of("book_id", 5, "title", "The Communist Manifesto", "author", "Karl Marx", "language", "en", "year", 1848),
            Map.of("book_id", 11, "title", "Alice’s Adventures in Wonderland", "author", "Lewis Carroll", "language", "en", "year", 1865),
            Map.of("book_id", 12, "title", "De la Terre à la Lune", "author", "Jules Verne", "language", "fr", "year", 1865)
    );

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7003); // Puerto del Search Service

        app.get("/search", App::handleSearch);
    }

    private static void handleSearch(Context ctx) {
        String query = (ctx.queryParam("q") != null ? ctx.queryParam("q") : "").toLowerCase();
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
        for (Map<String, Object> book : MOCK_BOOKS) {
            // Aplicar filtros
            if (authorFilter != null && !book.get("author").equals(authorFilter)) continue;
            if (languageFilter != null && !book.get("language").equals(languageFilter)) continue;
            if (yearFilter != null && !book.get("year").equals(yearFilter)) continue;

            // Búsqueda por palabra clave (solo en título en este ejemplo simple)
            if (!query.isEmpty() && !book.get("title").toString().toLowerCase().contains(query)) continue;

            results.add(book);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("filters", buildFilters(authorFilter, languageFilter, yearFilter));
        response.put("count", results.size());
        response.put("results", results);

        ctx.result(gson.toJson(response));
    }

    private static Map<String, Object> buildFilters(String author, String lang, Integer year) {
        Map<String, Object> filters = new HashMap<>();
        if (author != null) filters.put("author", author);
        if (lang != null) filters.put("language", lang);
        if (year != null) filters.put("year", year);
        return filters;
    }
}