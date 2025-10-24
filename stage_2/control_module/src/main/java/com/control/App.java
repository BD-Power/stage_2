package com.control;

import io.javalin.Javalin;
import kong.unirest.Unirest;

public class App {
    public static void main(String[] args) {

        // Servidor principal del control module
        Javalin app = Javalin.create().start(7004);

        // Endpoint normal
        app.get("/status", ctx -> ctx.result("Control module running"));

        // Nuevo endpoint: pregunta al data module si está activo
        app.get("/check-data", ctx -> {
            try {
                String response = Unirest.get("http://localhost:7005/status").asString().getBody();
                ctx.result("Respuesta del Data Module: " + response);
            } catch (Exception e) {
                ctx.result("Error al conectar con Data Module: " + e.getMessage());
            }
        });
    }
}
