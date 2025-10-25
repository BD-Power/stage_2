package com.data;

import io.javalin.Javalin;

public class DataApp {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7005); // Puerto distinto al control_module

        app.get("/status", ctx -> ctx.result("Data module running"));
    }
}
