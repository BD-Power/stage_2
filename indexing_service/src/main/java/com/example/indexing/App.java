// indexing_service/src/main/java/com/example/indexing/App.java
package com.example.indexing;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

public class App {
    private static final Gson gson = new Gson();
    private static final String DATALAKE_ROOT = "datalake";
    private static final String DATAMART_ROOT = "datamart";
    private static int booksIndexed = 0;
    private static String lastUpdate = "";

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7002); // Puerto del Indexing Service

        app.get("/status", ctx -> {
            JsonObject status = new JsonObject();
            status.addProperty("books_indexed", booksIndexed);
            status.addProperty("last_update", lastUpdate.isEmpty() ? "never" : lastUpdate);
            status.addProperty("index_size_MB", 0.1); // Placeholder
            ctx.result(gson.toJson(status));
        });

        app.post("/index/update/{book_id}", App::handleUpdateBook);
        app.post("/index/rebuild", App::handleRebuild);
    }

    private static void handleUpdateBook(Context ctx) {
        String bookIdStr = ctx.pathParam("book_id");
        try {
            int bookId = Integer.parseInt(bookIdStr);
            if (!bookExistsInDatalake(bookId)) {
                ctx.status(404).result(gson.toJson(Map.of("error", "Book not found in datalake")));
                return;
            }

            // Aquí iría la lógica real:
            // 1. Leer raw.txt del datalake
            // 2. Extraer título, autor, año, idioma, etc.
            // 3. Tokenizar el cuerpo y construir/actualizar el índice invertido en el datamart
            // 4. Guardar metadatos en datamart/metadata/

            // Simulamos el procesamiento
            simulateIndexing(bookId);

            booksIndexed++;
            lastUpdate = Instant.now().toString();

            ctx.result(gson.toJson(Map.of("book_id", bookId, "index", "updated")));
        } catch (NumberFormatException e) {
            ctx.status(400).result(gson.toJson(Map.of("error", "Invalid book_id")));
        }
    }

    private static void handleRebuild(Context ctx) {
        // Escanear todo el datalake y reindexar todos los libros
        Set<Integer> allBooks = findAllBooksInDatalake();
        int count = 0;
        long start = System.currentTimeMillis();

        for (int bookId : allBooks) {
            simulateIndexing(bookId);
            count++;
        }

        booksIndexed = count;
        lastUpdate = Instant.now().toString();
        double elapsed = (System.currentTimeMillis() - start) / 1000.0;

        ctx.result(gson.toJson(Map.of(
                "books_processed", count,
                "elapsed_time", String.format("%.1fs", elapsed)
        )));
    }

    private static boolean bookExistsInDatalake(int bookId) {
        try (var paths = Files.walk(Paths.get(DATALAKE_ROOT))) {
            return paths.anyMatch(p ->
                    p.getFileName() != null &&
                            p.getFileName().toString().equals(String.valueOf(bookId)) &&
                            Files.isDirectory(p)
            );
        } catch (IOException e) {
            return false;
        }
    }

    private static Set<Integer> findAllBooksInDatalake() {
        Set<Integer> books = new HashSet<>();
        try (var paths = Files.walk(Paths.get(DATALAKE_ROOT))) {
            paths.filter(Files::isDirectory)
                    .map(p -> {
                        try {
                            return Integer.parseInt(p.getFileName().toString());
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .forEach(books::add);
        } catch (IOException ignored) {}
        return books;
    }

    private static void simulateIndexing(int bookId) {
        try {
            Path datamartDir = Paths.get(DATAMART_ROOT, "indexes");
            Files.createDirectories(datamartDir);
            // Ej: guardar entrada en índice invertido
        } catch (IOException ignored) {}
    }
}