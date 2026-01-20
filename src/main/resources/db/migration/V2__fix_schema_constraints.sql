-- 1) teachers: убрать логическое противоречие has_group + group_id NOT NULL
ALTER TABLE teachers
    ALTER COLUMN group_id DROP NOT NULL;

-- если хочешь, можно вообще удалить has_group и ориентироваться на group_id IS NULL/NOT NULL,
-- но пока оставим и сделаем строгую проверку:
ALTER TABLE teachers
    ADD CONSTRAINT chk_teacher_group_consistency
        CHECK (
            (has_group = FALSE AND group_id IS NULL)
                OR
            (has_group = TRUE  AND group_id IS NOT NULL)
            );

-- 2) attendance: запретить дубли (один ученик, один предмет, один день = одна запись)
ALTER TABLE attendance
    ADD CONSTRAINT uq_attendance_student_discipline_date
        UNIQUE (student_id, discipline_id, lesson_date);

-- 3) permissions: запретить повтор одного и того же permission для одной роли
ALTER TABLE permissions
    ADD CONSTRAINT uq_permissions_role_name
        UNIQUE (role_id, name);
