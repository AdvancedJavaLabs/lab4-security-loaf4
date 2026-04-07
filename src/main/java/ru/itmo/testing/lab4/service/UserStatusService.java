package ru.itmo.testing.lab4.service;

import java.util.Optional;

/**
 * Сервис определения статуса пользователя.
 * Зависит от {@link UserAnalyticsService} для получения данных об активности.
 */
public class UserStatusService {

    private final UserAnalyticsService userAnalyticsService;

    public UserStatusService(UserAnalyticsService userAnalyticsService) {
        this.userAnalyticsService = userAnalyticsService;
    }

    /**
     * Определяет статус пользователя по суммарному времени активности.
     * <ul>
     *   <li>менее 60 минут — "Inactive"</li>
     *   <li>от 60 до 119 минут — "Active"</li>
     *   <li>120 минут и более — "Highly active"</li>
     * </ul>
     *
     * @param userId идентификатор пользователя
     * @return строка со статусом: "Inactive", "Active" или "Highly active"
     */
    public String getUserStatus(String userId) {

        long totalActivityTime = userAnalyticsService.getTotalActivityTime(userId);

        if (totalActivityTime < 60) {
            return "Inactive";
        } else if (totalActivityTime < 120) {
            return "Active";
        } else {
            return "Highly active";
        }
    }

    /**
     * Возвращает дату последней сессии пользователя.
     * Берёт последнюю сессию из списка и возвращает дату её окончания.
     *
     * @param userId идентификатор пользователя
     * @return Optional с датой в формате "yyyy-MM-dd";
     *         должен возвращать Optional.empty(), если у пользователя нет сессий
     */
    public Optional<String> getUserLastSessionDate(String userId) {
        UserAnalyticsService.Session lastSession = userAnalyticsService.getUserSessions(userId).getLast();
        return Optional.of(lastSession.getLogoutTime().toLocalDate().toString());
    }
}
