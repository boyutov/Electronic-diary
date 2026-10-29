package com.education.school.config;

// Текущая школа запроса (Multi-Tenancy)
// Заполняется в JwtAuthenticationFilter из токена, читается Hibernate через SchoolTenantResolver —
// так каждый SQL-запрос к школьным таблицам автоматически ограничивается "school_id = текущая школа"
public final class TenantContext {

    // "Школа не выбрана": school_id = -1 не совпадает ни с одной записью,
    // поэтому код, работающий без токена, не увидит данные ни одной школы
    public static final Long NO_SCHOOL = -1L;

    private static final ThreadLocal<Long> CURRENT_SCHOOL_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setSchoolId(Long schoolId) {
        CURRENT_SCHOOL_ID.set(schoolId);
    }

    public static Long getSchoolId() {
        return CURRENT_SCHOOL_ID.get();
    }

    // Для операций, которым школа обязательна (например, привязка нового пользователя)
    public static Long getRequiredSchoolId() {
        Long schoolId = CURRENT_SCHOOL_ID.get();
        if (schoolId == null) {
            throw new IllegalStateException("School is not selected for the current request");
        }
        return schoolId;
    }

    // Обязательно вызывать в конце запроса — потоки Tomcat переиспользуются
    public static void clear() {
        CURRENT_SCHOOL_ID.remove();
    }
}
