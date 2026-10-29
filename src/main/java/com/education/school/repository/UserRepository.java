package com.education.school.repository;

import com.education.school.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// Пользователи общие для всех школ (один человек может работать в нескольких),
// поэтому таблица users не фильтруется Hibernate автоматически — школу передаём явно через user_schools
public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    List<User> findAllBySchoolsIdAndRoleName(Long schoolId, String roleName);

    List<User> findAllBySchoolsId(Long schoolId);

    Optional<User> findByIdAndSchoolsId(Long id, Long schoolId);

    Optional<User> findByIdAndSchoolsIdAndRoleName(Long id, Long schoolId, String roleName);

    boolean existsByIdAndSchoolsId(Long id, Long schoolId);

    boolean existsByEmailIgnoreCase(String email);
}
