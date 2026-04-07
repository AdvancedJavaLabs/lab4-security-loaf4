package ru.itmo.testing.lab4.controller;

import io.javalin.Javalin;
import ru.itmo.testing.lab4.service.UserAnalyticsService;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * REST-контроллер для работы с аналитикой пользовательской активности.
 * Все параметры передаются через query-параметры.
 * <p>
 * Эндпоинты:
 * <ul>
 *   <li>POST /register           — регистрация пользователя (userId, userName)</li>
 *   <li>POST /recordSession       — запись сессии (userId, loginTime, logoutTime в ISO формате)</li>
 *   <li>GET  /totalActivity       — суммарное время активности в минутах (userId)</li>
 *   <li>GET  /inactiveUsers       — список неактивных пользователей (days — порог в днях)</li>
 *   <li>GET  /monthlyActivity     — активность по дням за месяц (userId, month в формате yyyy-MM)</li>
 *   <li>GET  /userProfile         — HTML-профиль пользователя (userId)</li>
 *   <li>GET  /exportReport        — экспорт отчёта в файл (userId, filename)</li>
 *   <li>POST /notify              — уведомление по вебхуку (userId, callbackUrl)</li>
 * </ul>
 * <p>
 * При отсутствии обязательных параметров возвращает 400.
 * При некорректном формате данных возвращает 400.
 */
public class UserAnalyticsController {

    // Базовая директория для экспорта отчётов
    private static final String REPORTS_BASE_DIR = "/tmp/reports/";

    public static Javalin createApp() {
        UserAnalyticsService service = new UserAnalyticsService();
        Javalin app = Javalin.create();

        app.post("/register", ctx -> {
            String userId = ctx.queryParam("userId");
            String userName = ctx.queryParam("userName");
            if (userId == null || userName == null) {
                ctx.status(400).result("Missing parameters");
                return;
            }
            boolean success = service.registerUser(userId, userName);
            ctx.result("User registered: " + success);
        });

        app.post("/recordSession", ctx -> {
            String userId = ctx.queryParam("userId");
            String loginTime = ctx.queryParam("loginTime");
            String logoutTime = ctx.queryParam("logoutTime");
            if (userId == null || loginTime == null || logoutTime == null) {
                ctx.status(400).result("Missing parameters");
                return;
            }
            try {
                LocalDateTime login = LocalDateTime.parse(loginTime);
                LocalDateTime logout = LocalDateTime.parse(logoutTime);
                boolean success = service.recordSession(userId, login, logout);
                if (success) {
                    ctx.result("Session recorded");
                } else {
                    ctx.status(404).result("User not found");
                }
            } catch (Exception e) {
                ctx.status(400).result("Invalid data: " + e.getMessage());
            }
        });

        app.get("/totalActivity", ctx -> {
            String userId = ctx.queryParam("userId");
            if (userId == null) {
                ctx.status(400).result("Missing userId");
                return;
            }
            long minutes = service.getTotalActivityTime(userId);
            ctx.result("Total activity: " + minutes + " minutes");
        });

        app.get("/inactiveUsers", ctx -> {
            String daysParam = ctx.queryParam("days");
            if (daysParam == null) {
                ctx.status(400).result("Missing days parameter");
                return;
            }
            try {
                int days = Integer.parseInt(daysParam);
                List<String> inactiveUsers = service.findInactiveUsers(days);
                ctx.json(inactiveUsers);
            } catch (NumberFormatException e) {
                ctx.status(400).result("Invalid number format for days");
            }
        });

        app.get("/monthlyActivity", ctx -> {
            String userId = ctx.queryParam("userId");
            String monthParam = ctx.queryParam("month");
            if (userId == null || monthParam == null) {
                ctx.status(400).result("Missing parameters");
                return;
            }
            try {
                YearMonth month = YearMonth.parse(monthParam);
                Map<String, Long> activity = service.getMonthlyActivityMetric(userId, month);
                ctx.json(activity);
            } catch (Exception e) {
                ctx.status(400).result("Invalid data: " + e.getMessage());
            }
        });

        // --- [EXAMPLE: CWE-79 Reflected XSS] ---
        // Эндпоинт возвращает HTML-страницу профиля пользователя.
        // Данные из хранилища подставляются в разметку напрямую.
        app.get("/userProfile", ctx -> {
            String userId = ctx.queryParam("userId");
            if (userId == null) {
                ctx.status(400).result("Missing userId");
                return;
            }
            UserAnalyticsService.User user = service.getUser(userId);
            if (user == null) {
                ctx.status(404).result("User not found");
                return;
            }
            long totalMinutes = service.getTotalActivityTime(userId);
            String html = "<html><body>"
                    + "<h1>Profile: " + user.getUserName() + "</h1>"
                    + "<p>ID: " + user.getUserId() + "</p>"
                    + "<p>Total activity: " + totalMinutes + " min</p>"
                    + "</body></html>";
            ctx.contentType("text/html").result(html);
        });

        app.get("/exportReport", ctx -> {
            String userId = ctx.queryParam("userId");
            String filename = ctx.queryParam("filename");
            if (userId == null || filename == null) {
                ctx.status(400).result("Missing parameters");
                return;
            }
            UserAnalyticsService.User user = service.getUser(userId);
            if (user == null) {
                ctx.status(404).result("User not found");
                return;
            }
            File reportFile = new File(REPORTS_BASE_DIR + filename);
            try {
                reportFile.getParentFile().mkdirs();
                try (FileWriter writer = new FileWriter(reportFile)) {
                    writer.write("Report for user: " + user.getUserName() + "\n");
                    writer.write("Total activity: " + service.getTotalActivityTime(userId) + " min\n");
                }
                ctx.result("Report saved to: " + reportFile.getPath());
            } catch (Exception e) {
                ctx.status(500).result("Failed to write report: " + e.getMessage());
            }
        });

        app.post("/notify", ctx -> {
            String userId = ctx.queryParam("userId");
            String callbackUrl = ctx.queryParam("callbackUrl");
            if (userId == null || callbackUrl == null) {
                ctx.status(400).result("Missing parameters");
                return;
            }
            UserAnalyticsService.User user = service.getUser(userId);
            if (user == null) {
                ctx.status(404).result("User not found");
                return;
            }
            try {
                URL url = new URL(callbackUrl);
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                try (InputStream in = connection.getInputStream()) {
                    String response = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    ctx.result("Notification sent. Response: " + response);
                }
            } catch (Exception e) {
                ctx.status(500).result("Notification failed: " + e.getMessage());
            }
        });

        return app;
    }
}
