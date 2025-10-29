package com.example.ingestion;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class App {
    private static final Gson gson = new Gson();
    private static final String DATALAKE_ROOT = "datalake"; // Carpeta raíz del datalake
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7001); // Puerto del Ingestion Service

        app.get("/status", ctx -> {
            JsonObject status = new JsonObject();
            status.addProperty("service", "ingestion-service");
            status.addProperty("status", "running");
            ctx.result(gson.toJson(status));
        });

        app.post("/ingest/{book_id}", App::handleIngest);
        app.get("/ingest/status/{book_id}", App::handleStatus);
        app.get("/ingest/list", App::handleList);
    }

    private static void handleIngest(Context ctx) {
        String bookIdStr = ctx.pathParam("book_id");
        int bookId;
        try {
            bookId = Integer.parseInt(bookIdStr);
        } catch (NumberFormatException e) {
            ctx.status(400).result(gson.toJson(Map.of("error", "Invalid book_id")));
            return;
        }

        String gutenbergUrl = String.format("https://www.gutenberg.org/files/%d/%d-0.txt", bookId, bookId);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd/HH"));
        String bookPath = DATALAKE_ROOT + "/" + timestamp + "/" + bookId;

        try {
            // Descargar el libro
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(gutenbergUrl))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                ctx.status(404).result(gson.toJson(Map.of("book_id", bookId, "status", "not_found")));
                return;
            }

            // Crear directorio
            Path dir = Paths.get(bookPath);
            Files.createDirectories(dir);

            // Guardar contenido
            Files.write(dir.resolve("raw.txt"), response.body().getBytes());

            // Responder con éxito
            JsonObject result = new JsonObject();
            result.addProperty("book_id", bookId);
            result.addProperty("status", "downloaded");
            result.addProperty("path", bookPath);
            ctx.result(gson.toJson(result));

        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result(gson.toJson(Map.of("error", "Download failed: " + e.getMessage())));
        }
    }

    private static void handleStatus(Context ctx) {
        String bookIdStr = ctx.pathParam("book_id");
        try {
            int bookId = Integer.parseInt(bookIdStr);
            boolean exists = findBookInDatalake(bookId);
            String status = exists ? "available" : "not found";
            ctx.result(gson.toJson(Map.of("book_id", bookId, "status", status)));
        } catch (NumberFormatException e) {
            ctx.status(400).result(gson.toJson(Map.of("error", "Invalid book_id")));
        }
    }

    private static void handleList(Context ctx) {
        Set<Integer> books = new HashSet<>();
        try (Stream<Path> paths = Files.walk(Paths.get(DATALAKE_ROOT))) {
            paths.filter(Files::isDirectory)
                    .map(path -> {
                        String dirName = path.getFileName().toString();
                        try {
                            return Integer.parseInt(dirName);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .forEach(books::add);
        } catch (IOException e) {
            ctx.status(500).result(gson.toJson(Map.of("error", "Failed to scan datalake")));
            return;
        }

        List<Integer> sortedBooks = new ArrayList<>(books);
        Collections.sort(sortedBooks);

        JsonObject result = new JsonObject();
        result.addProperty("count", sortedBooks.size());
        result.add("books", gson.toJsonTree(sortedBooks));
        ctx.result(gson.toJson(result));
    }

    private static boolean findBookInDatalake(int bookId) {
        try (Stream<Path> paths = Files.walk(Paths.get(DATALAKE_ROOT))) {
            return paths.anyMatch(path ->
                    path.getFileName() != null &&
                            path.getFileName().toString().equals(String.valueOf(bookId)) &&
                            Files.isDirectory(path)
            );
        } catch (IOException e) {
            return false;
        }
    }
}