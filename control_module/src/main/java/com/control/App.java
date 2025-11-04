package com.control;

import io.javalin.Javalin;
import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class App {

    private static final Gson gson = new Gson();
    private static final String DATAMART_INDEX_DIR = "datamart/indexes";

    public static void main(String[] args) {

        Unirest.config().connectTimeout(5000).socketTimeout(20000);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7004);

        app.get("/status", ctx -> {
            JsonObject status = new JsonObject();
            status.addProperty("service", "control-module");
            status.addProperty("status", "running");
            ctx.result(gson.toJson(status));
        });

        app.post("/pipeline/{book_id}", ctx -> {
            String bookIdStr = ctx.pathParam("book_id");

            int bookId;
            try {
                bookId = Integer.parseInt(bookIdStr);
            } catch (NumberFormatException e) {
                ctx.status(400).result(gson.toJson(Map.of("error", "Invalid book_id")));
                return;
            }

            try {
                if (isAlreadyIndexed(bookId)) {
                    ctx.result(gson.toJson(Map.of(
                            "book_id", bookId,
                            "pipeline", "skipped",
                            "reason", "Book already indexed"
                    )));
                    return;
                }

                // Ingestion
                HttpResponse<JsonNode> ingestResp = Unirest
                        .post("http://localhost:7001/ingest/" + bookId)
                        .asJson();

                if (ingestResp.getStatus() != 200) {
                    ctx.status(500).result(gson.toJson(Map.of("error", "Ingestion failed")));
                    return;
                }

                String statusIngest = ingestResp.getBody().getObject().getString("status");
                if (!"downloaded".equals(statusIngest)) {
                    ctx.status(404).result(gson.toJson(Map.of("error", "Book not found in Project Gutenberg")));
                    return;
                }

                // Indexing
                HttpResponse<JsonNode> indexResp = Unirest
                        .post("http://localhost:7002/index/update/" + bookId)
                        .asJson();

                if (indexResp.getStatus() != 200) {
                    ctx.status(500).result(gson.toJson(Map.of("error", "Indexing failed")));
                    return;
                }


                ctx.result(gson.toJson(Map.of(
                        "book_id", bookId,
                        "pipeline", "completed",
                        "step_ingestion", "success",
                        "step_indexing", "success"
                )));

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result(gson.toJson(Map.of("error",
                        "Pipeline execution failed: " + e.getMessage())));
            }
        });
    }

    private static boolean isAlreadyIndexed(int bookId) {
        Path indexPath = Paths.get(DATAMART_INDEX_DIR, bookId + ".json");
        return Files.exists(indexPath);
    }
}
