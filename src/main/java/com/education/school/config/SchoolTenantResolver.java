package com.education.school.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

// Сообщает Hibernate, какая школа текущая. Hibernate использует это значение для полей @TenantId:
// подставляет его при INSERT и добавляет фильтр "school_id = ?" во все SELECT, включая findById
@Component
public class SchoolTenantResolver implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long schoolId = TenantContext.getSchoolId();
        return schoolId != null ? schoolId : TenantContext.NO_SCHOOL;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    // Регистрируем резолвер в настройках Hibernate (Spring Boot сам его не подхватывает)
    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
