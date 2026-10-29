-- При удалении ученика/учителя/родителя теперь удаляется и его аккаунт (users).
-- Чтобы удаление не падало, ссылки "кто создал / куратор / автор" обнуляются,
-- а голоса и посещаемость удаляются вместе с владельцем.
-- Оценки и расписание учителя (marks.given_by_teacher_id, schedule.teacher_id) остаются строгими —
-- учителя с выставленными оценками удалить нельзя, история оценок не теряется.
-- schools.director_user_id тоже не трогаем: основателя школы удалить нельзя.

CREATE FUNCTION pg_temp.set_fk_on_delete(p_table TEXT, p_column TEXT, p_ref TEXT, p_action TEXT) RETURNS void AS $$
DECLARE
    fk_name TEXT;
BEGIN
    SELECT c.conname INTO fk_name
    FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
    WHERE c.contype = 'f' AND c.conrelid = p_table::regclass AND a.attname = p_column;

    IF fk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', p_table, fk_name);
    END IF;
    EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I(id) ON DELETE %s',
                   p_table, p_table || '_' || p_column || '_fkey', p_column, p_ref, p_action);
END;
$$ LANGUAGE plpgsql;

SELECT pg_temp.set_fk_on_delete('users',    'created_by_admin_user_id', 'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('users',    'deleted_by_admin_user_id', 'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('groups',   'curator',                  'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('groups',   'created_by_admin_user_id', 'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('groups',   'deleted_by_admin_user_id', 'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('students', 'curator',                  'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('students', 'created_by_admin_user_id', 'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('students', 'deleted_by_admin_user_id', 'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('teachers', 'created_by_admin_user_id', 'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('teachers', 'deleted_by_admin_user_id', 'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('news',     'created_by_user_id',       'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('news',     'teacher_id',               'teachers', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('polls',    'created_by_user_id',       'users', 'SET NULL');
SELECT pg_temp.set_fk_on_delete('poll_votes', 'user_id',                'users', 'CASCADE');
SELECT pg_temp.set_fk_on_delete('attendance', 'student_id',             'students', 'CASCADE');
