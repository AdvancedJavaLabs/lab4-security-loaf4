package ru.itmo.testing.lab4.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис аналитики пользовательской активности.
 * Хранит зарегистрированных пользователей и их сессии,
 * предоставляет методы для расчёта метрик активности.
 */
public class UserAnalyticsService {

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, List<Session>> userSessions = new HashMap<>();

    /**
     * Регистрирует нового пользователя.
     *
     * @param userId   уникальный идентификатор пользователя (не должен быть null или пустым)
     * @param userName имя пользователя (не должно быть null или пустым)
     * @return true, если пользователь успешно зарегистрирован;
     *         false, если пользователь с таким userId уже существует
     */
    public boolean registerUser(String userId, String userName) {
        if (users.containsKey(userId)) {
            return false;
        }
        users.put(userId, new User(userId, userName));
        return true;
    }

    /**
     * Записывает сессию активности пользователя.
     *
     * @param userId    идентификатор зарегистрированного пользователя
     * @param loginTime время начала сессии (должно быть раньше logoutTime)
     * @param logoutTime время окончания сессии (должно быть позже loginTime)
     * @return true, если сессия успешно записана;
     *         false, если пользователь не найден
     */
    public boolean recordSession(String userId, LocalDateTime loginTime, LocalDateTime logoutTime) {
        if (!users.containsKey(userId)) {
            return false;
        }
        Session session = new Session(loginTime, logoutTime);
        userSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(session);
        return true;
    }

    /**
     * Возвращает суммарное время активности пользователя в минутах.
     * Считается как сумма длительностей всех сессий пользователя.
     *
     * @param userId идентификатор пользователя
     * @return суммарное время активности в минутах; 0, если у пользователя нет сессий
     */
    public long getTotalActivityTime(String userId) {
        if (!userSessions.containsKey(userId)) {
            return 0;
        }
        return userSessions.get(userId).stream()
                .mapToLong(session -> ChronoUnit.MINUTES.between(session.getLoginTime(), session.getLogoutTime()))
                .sum();
    }

    /**
     * Находит пользователей, неактивных более заданного количества дней.
     * Пользователь считается неактивным, если с момента окончания его последней сессии
     * прошло строго больше {@code days} дней.
     * <p>
     * Должен учитывать всех зарегистрированных пользователей,
     * включая тех, у кого нет ни одной сессии.
     *
     * @param days порог неактивности в днях (должен быть >= 0)
     * @return список userId неактивных пользователей
     */
    public List<String> findInactiveUsers(int days) {
        List<String> inactiveUsers = new ArrayList<>();
        for (Map.Entry<String, List<Session>> entry : userSessions.entrySet()) {
            String userId = entry.getKey();
            List<Session> sessions = entry.getValue();
            if (sessions.isEmpty()) continue;
            LocalDateTime lastSessionTime = sessions.get(sessions.size() - 1).getLogoutTime();
            long daysInactive = ChronoUnit.DAYS.between(lastSessionTime, LocalDateTime.now());
            if (daysInactive > days) {
                inactiveUsers.add(userId);
            }
        }
        return inactiveUsers;
    }

    /**
     * Возвращает метрики активности пользователя за указанный месяц.
     * Результат — карта, где ключ — дата (в формате "yyyy-MM-dd"),
     * значение — суммарная активность за этот день в минутах.
     *
     * @param userId идентификатор пользователя
     * @param month  месяц для расчёта (например, 2025-01)
     * @return карта активности по дням; пустая карта, если сессий в этом месяце нет
     * @throws IllegalArgumentException если у пользователя нет ни одной сессии
     */
    public Map<String, Long> getMonthlyActivityMetric(String userId, YearMonth month) {
        if (!userSessions.containsKey(userId)) {
            throw new IllegalArgumentException("No sessions found for user");
        }
        Map<String, Long> activityByDay = new HashMap<>();
        userSessions.get(userId).stream()
                .filter(session -> isSessionInMonth(session, month))
                .forEach(session -> {
                    String dayKey = session.getLoginTime().toLocalDate().toString();
                    long minutes = ChronoUnit.MINUTES.between(session.getLoginTime(), session.getLogoutTime());
                    activityByDay.put(dayKey, activityByDay.getOrDefault(dayKey, 0L) + minutes);
                });
        return activityByDay;
    }

    private boolean isSessionInMonth(Session session, YearMonth month) {
        LocalDateTime start = session.getLoginTime();
        return start.getYear() == month.getYear() && start.getMonth() == month.getMonth();
    }

    /**
     * Возвращает объект пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     * @return объект User или null, если пользователь не найден
     */
    public User getUser(String userId) {
        return users.get(userId);
    }

    /**
     * Возвращает список сессий пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список сессий или null, если у пользователя нет записанных сессий
     */
    public List<Session> getUserSessions(String userId) {
        return userSessions.get(userId);
    }

    public static class User {
        private final String userId;
        private final String userName;

        public User(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        public String getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }
    }

    public static class Session {
        private final LocalDateTime loginTime;
        private final LocalDateTime logoutTime;

        public Session(LocalDateTime loginTime, LocalDateTime logoutTime) {
            this.loginTime = loginTime;
            this.logoutTime = logoutTime;
        }

        public LocalDateTime getLoginTime() {
            return loginTime;
        }

        public LocalDateTime getLogoutTime() {
            return logoutTime;
        }
    }
}
