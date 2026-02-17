package com.education.school.repository;

import com.education.school.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
    
    List<User> findAllByRoleName(String roleName);
}
