-- Пользователь, пароль которого задал кто-то другой (админ, импорт, ИИ),
-- обязан сменить его при первом входе. Существующие аккаунты не трогаем.
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
