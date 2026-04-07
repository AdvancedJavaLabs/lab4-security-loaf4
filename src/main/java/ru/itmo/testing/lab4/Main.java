package ru.itmo.testing.lab4;

import io.javalin.Javalin;
import ru.itmo.testing.lab4.controller.UserAnalyticsController;

public class Main {

    private static final int DEFAULT_PORT = 7000;

    public static void main(String[] args) {
        Javalin app = UserAnalyticsController.createApp();
        app.start(DEFAULT_PORT);
        System.out.println("Server started on http://localhost:" + DEFAULT_PORT);
    }
}
