-- Изоляция данных по школам (Multi-Tenancy).
-- Каждая школьная таблица получает school_id; Hibernate (@TenantId) фильтрует по нему все запросы.
-- Существующие строки распределяются по школам через связанные записи,
-- а то, что определить нельзя, достаётся первой школе.

ALTER TABLE groups        ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE students      ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE teachers      ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE parents       ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE courses       ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE disciplines   ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE marks         ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE schedule      ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE attendance    ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE news          ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE polls         ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE poll_options  ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE poll_votes    ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE complaints    ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE canteen_menus ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE exams         ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;
ALTER TABLE notifications ADD COLUMN school_id BIGINT REFERENCES schools(id) ON DELETE CASCADE;

-- Основная школа пользователя (если он в нескольких — берём первую)
CREATE FUNCTION pg_temp.user_school(p_user_id BIGINT) RETURNS BIGINT AS $$
    SELECT MIN(school_id) FROM user_schools WHERE user_id = p_user_id
$$ LANGUAGE sql STABLE;

-- 1. Люди: по их аккаунту
UPDATE students SET school_id = pg_temp.user_school(user_id);
UPDATE teachers SET school_id = pg_temp.user_school(user_id);
UPDATE parents  SET school_id = pg_temp.user_school(user_id);

-- 2. Группы: по ученикам, иначе по куратору или создателю
UPDATE groups g SET school_id = COALESCE(
    (SELECT MIN(s.school_id) FROM students s WHERE s.group_id = g.id),
    pg_temp.user_school(g.curator),
    pg_temp.user_school(g.created_by_admin_user_id));

-- Ученики/учителя без привязки аккаунта — по группе
UPDATE students s SET school_id = g.school_id FROM groups g WHERE s.school_id IS NULL AND s.group_id = g.id;
UPDATE teachers t SET school_id = g.school_id FROM groups g WHERE t.school_id IS NULL AND t.group_id = g.id;
UPDATE parents p SET school_id = (
    SELECT MIN(s.school_id) FROM parent_student ps JOIN students s ON s.id = ps.student_id WHERE ps.parent_id = p.id)
WHERE p.school_id IS NULL;

-- 3. Учебные данные
UPDATE courses c   SET school_id = t.school_id FROM teachers t WHERE c.teacher_id = t.id;
UPDATE marks m     SET school_id = s.school_id FROM students s WHERE m.student_id = s.id;
UPDATE schedule sc SET school_id = COALESCE(
    (SELECT g.school_id FROM groups g WHERE g.id = sc.group_id),
    (SELECT t.school_id FROM teachers t WHERE t.id = sc.teacher_id));
UPDATE attendance a SET school_id = COALESCE(
    (SELECT s.school_id FROM students s WHERE s.id = a.student_id),
    (SELECT sc.school_id FROM schedule sc WHERE sc.id = a.schedule_id));

-- 4. Публикации — по автору
UPDATE news n SET school_id = COALESCE(
    pg_temp.user_school(n.created_by_user_id),
    (SELECT t.school_id FROM teachers t WHERE t.id = n.teacher_id));
UPDATE polls p SET school_id = pg_temp.user_school(p.created_by_user_id);
UPDATE complaints c SET school_id = pg_temp.user_school(c.author_user_id);
UPDATE canteen_menus cm SET school_id = COALESCE(
    pg_temp.user_school(cm.created_by_user_id),
    (SELECT MIN(g.school_id) FROM canteen_menu_groups cmg JOIN groups g ON g.id = cmg.group_id WHERE cmg.menu_id = cm.id));
UPDATE exams e SET school_id = COALESCE(
    pg_temp.user_school(e.created_by_user_id),
    (SELECT MIN(g.school_id) FROM exam_groups eg JOIN groups g ON g.id = eg.group_id WHERE eg.exam_id = e.id));
UPDATE notifications n SET school_id = pg_temp.user_school(n.user_id);

-- 5. Всё, что не удалось определить, — первой школе
UPDATE groups        SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE students      SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE teachers      SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE parents       SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE courses c     SET school_id = COALESCE((SELECT t.school_id FROM teachers t WHERE t.id = c.teacher_id), (SELECT MIN(id) FROM schools)) WHERE school_id IS NULL;
UPDATE marks m       SET school_id = COALESCE((SELECT s.school_id FROM students s WHERE s.id = m.student_id), (SELECT MIN(id) FROM schools)) WHERE school_id IS NULL;
UPDATE schedule      SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE attendance    SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE news          SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE polls         SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE complaints    SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE canteen_menus SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE exams         SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;
UPDATE notifications SET school_id = (SELECT MIN(id) FROM schools) WHERE school_id IS NULL;

-- Варианты и голоса опроса принадлежат школе опроса
UPDATE poll_options o SET school_id = p.school_id FROM polls p WHERE o.poll_id = p.id;
UPDATE poll_votes v   SET school_id = p.school_id FROM polls p WHERE v.poll_id = p.id;

-- 6. Предметы раньше были общими для всех школ. Каждая школа получает свою копию предмета,
-- и её учителя, расписание, оценки, посещаемость и экзамены переводятся на эту копию.
DO $$
DECLARE
    r RECORD;
    new_id INTEGER;
BEGIN
    CREATE TEMP TABLE discipline_usage ON COMMIT DROP AS
        SELECT td.discipline_id, t.school_id FROM teacher_discipline td JOIN teachers t ON t.id = td.teacher_id
        UNION SELECT discipline_id, school_id FROM schedule
        UNION SELECT discipline_id, school_id FROM marks
        UNION SELECT discipline_id, school_id FROM attendance WHERE discipline_id IS NOT NULL
        UNION SELECT discipline_id, school_id FROM exams WHERE discipline_id IS NOT NULL;

    -- Исходная запись предмета остаётся первой школе, которая им пользуется
    UPDATE disciplines d SET school_id = COALESCE(
        (SELECT MIN(u.school_id) FROM discipline_usage u WHERE u.discipline_id = d.id),
        (SELECT MIN(id) FROM schools));

    FOR r IN
        SELECT DISTINCT u.discipline_id, u.school_id
        FROM discipline_usage u JOIN disciplines d ON d.id = u.discipline_id
        WHERE u.school_id IS NOT NULL AND u.school_id <> d.school_id
    LOOP
        INSERT INTO disciplines (name, school_id)
            SELECT name, r.school_id FROM disciplines WHERE id = r.discipline_id
            RETURNING id INTO new_id;

        UPDATE teacher_discipline td SET discipline_id = new_id
            FROM teachers t
            WHERE td.teacher_id = t.id AND t.school_id = r.school_id AND td.discipline_id = r.discipline_id;
        UPDATE schedule   SET discipline_id = new_id WHERE school_id = r.school_id AND discipline_id = r.discipline_id;
        UPDATE marks      SET discipline_id = new_id WHERE school_id = r.school_id AND discipline_id = r.discipline_id;
        UPDATE attendance SET discipline_id = new_id WHERE school_id = r.school_id AND discipline_id = r.discipline_id;
        UPDATE exams      SET discipline_id = new_id WHERE school_id = r.school_id AND discipline_id = r.discipline_id;
    END LOOP;
END $$;

-- 7. NOT NULL ставим, только если школы есть и все строки распределены
--    (на пустой базе без школ столбцы останутся NULL-допустимыми, но приложение всё равно всегда их заполняет)
DO $$
DECLARE
    tbl TEXT;
    has_nulls BOOLEAN;
BEGIN
    FOREACH tbl IN ARRAY ARRAY['groups','students','teachers','parents','courses','disciplines','marks','schedule',
                               'attendance','news','polls','poll_options','poll_votes','complaints',
                               'canteen_menus','exams','notifications']
    LOOP
        EXECUTE format('SELECT EXISTS (SELECT 1 FROM %I WHERE school_id IS NULL)', tbl) INTO has_nulls;
        IF NOT has_nulls THEN
            EXECUTE format('ALTER TABLE %I ALTER COLUMN school_id SET NOT NULL', tbl);
        END IF;
        EXECUTE format('CREATE INDEX idx_%s_school_id ON %I (school_id)', tbl, tbl);
    END LOOP;
END $$;

-- 8. Имя школы — это её адрес в URL и в токене, оно должно быть уникальным
DO $$
BEGIN
    IF NOT EXISTS (SELECT lower(name) FROM schools GROUP BY lower(name) HAVING COUNT(*) > 1) THEN
        CREATE UNIQUE INDEX uq_schools_name_lower ON schools (lower(name));
    END IF;
END $$;
