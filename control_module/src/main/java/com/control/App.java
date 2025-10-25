package com.control;

import io.javalin.Javalin;
import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;

public class App {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        // Configurar Unirest con timeouts razonables
        Unirest.config().connectTimeout(5000).socketTimeout(20000);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7004);

        // Endpoint de estado
        app.get("/status", ctx -> {
            JsonObject status = new JsonObject();
            status.addProperty("service", "control-module");
            status.addProperty("status", "running");
            ctx.result(gson.toJson(status));
        });

        // Endpoint para orquestar el pipeline completo
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
                // 1. Ingestar el libro
                HttpResponse<JsonNode> ingestResp = Unirest
                        .post("http://localhost:7001/ingest/" + bookId)
                        .asJson();

                if (ingestResp.getStatus() != 200) {
                    ctx.status(500).result(gson.toJson(Map.of("error", "Ingestion failed")));
                    return;
                }

                String statusIngest = ingestResp.getBody().getObject().getString("status");
                if (!"downloaded".equals(statusIngest)) {
                    ctx.status(404).result(gson.toJson(Map.of("error", "Book not found on Project Gutenberg")));
                    return;
                }

                // 2. Indexar el libro
                HttpResponse<JsonNode> indexResp = Unirest
                        .post("http://localhost:7002/index/update/" + bookId)
                        .asJson();

                if (indexResp.getStatus() != 200) {
                    ctx.status(500).result(gson.toJson(Map.of("error", "Indexing failed")));
                    return;
                }

                // Éxito
                JsonObject result = new JsonObject();
                result.addProperty("book_id", bookId);
                result.addProperty("pipeline", "completed");
                result.addProperty("step_ingestion", "success");
                result.addProperty("step_indexing", "success");
                ctx.result(gson.toJson(result));

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result(gson.toJson(Map.of("error", "Pipeline execution failed: " + e.getMessage())));
            }
        });
    }
}