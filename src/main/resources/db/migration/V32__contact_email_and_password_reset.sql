-- 1. Куда слать письма, если у пользователя нет своей почты:
--    у ребёнка без почты логин сгенерирован, а пароль и ссылки на восстановление получает родитель
ALTER TABLE users ADD COLUMN contact_email VARCHAR(255);

-- 2. Логин (email) должен быть уникальным: иначе повторный импорт создаст дубликаты, и они не смогут войти
DO $$
BEGIN
    IF NOT EXISTS (SELECT lower(email) FROM users GROUP BY lower(email) HAVING COUNT(*) > 1) THEN
        CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
    END IF;
END $$;

-- 3. Одноразовые ссылки "Забыли пароль?": храним только хэш токена, срок жизни — 1 час
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
