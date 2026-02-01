-- Align parents.user_id with users.id (BIGINT)
ALTER TABLE parents
    ALTER COLUMN user_id TYPE BIGINT USING user_id::BIGINT;
