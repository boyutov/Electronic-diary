package com.education.school.service;

import com.education.school.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Реализация UserDetailsService — Spring Security вызывает loadUserByUsername при каждой аутентификации
// Именно здесь происходит загрузка пользователя из БД по email
@Service
public class UserDeteilsImpl implements UserDetailsService {

    private UserRepository userRepository;

    public UserDeteilsImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Spring Security вызывает этот метод когда нужно проверить токен или аутентифицировать пользователя
    // username в нашем случае — это email
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Находим пользователя в БД по email — User реализует UserDetails, поэтому можно вернуть напрямую
        return userRepository.findByEmail(username);
    }
}
