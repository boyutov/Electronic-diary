package com.education.school.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// Сервис для работы с JWT: создание токенов, парсинг, проверка подписи
@Service
public class JwtService {

    // Секретный ключ подписи берётся из переменной окружения JWT_SECRET (минимум 32 байта для HS256)
    private final String secretKey;

    // Время жизни токена в миллисекундах (по умолчанию 24 часа)
    private final long expirationTime;

    public JwtService(@Value("${jwt.secret}") String secretKey,
                      @Value("${jwt.expiration:86400000}") long expirationTime) {
        if (secretKey == null || secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and be at least 32 bytes long");
        }
        this.secretKey = secretKey;
        this.expirationTime = expirationTime;
    }

    // Извлекаем email из токена — он хранится в стандартном поле "subject"
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Извлекаем schoolName — кастомное поле, которое мы сами добавили при создании токена
    public String extractSchoolName(String token) {
        return extractClaim(token, claims -> claims.get("schoolName", String.class));
    }

    // Универсальный метод: принимает функцию-маппер и возвращает нужное поле из payload
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Создаём токен с дополнительным полем schoolName в payload
    public String generateToken(UserDetails userDetails, String schoolName) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("schoolName", schoolName);
        return generateToken(extraClaims, userDetails);
    }

    // Строим JWT: задаём payload, subject (email), даты, подписываем ключом → compact() даёт строку
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)                                         // кастомные поля (schoolName)
                .subject(userDetails.getUsername())                          // email пользователя
                .issuedAt(new Date(System.currentTimeMillis()))              // время выдачи
                .expiration(new Date(System.currentTimeMillis() + expirationTime)) // время истечения
                .signWith(getSignInKey(), Jwts.SIG.HS256)                   // подписываем HMAC-SHA256
                .compact();                                                   // сериализуем в строку xxx.yyy.zzz
    }

    // Токен валиден если: email совпадает с пользователем И токен не истёк
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // Токен истёк если дата expiration раньше текущего времени
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Парсим токен и получаем все Claims — библиотека JJWT проверяет подпись автоматически
    // Если подпись не совпадает — выбрасывает исключение
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey()) // указываем ключ для верификации подписи
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Преобразуем строку-секрет в объект SecretKey для алгоритма HMAC
    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
