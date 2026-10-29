-- 1. Расписание: старое правило "один урок на день недели" (из времён до появления дат) не давало
--    создать тот же урок на следующей неделе. Уникальность теперь по конкретной дате
ALTER TABLE schedule DROP CONSTRAINT IF EXISTS schedule_day_of_week_lesson_number_group_id_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_schedule_group_date_lesson ON schedule (group_id, date, lesson_number) WHERE date IS NOT NULL;

-- 2. Посещаемость: старое правило "одна отметка на предмет в день" ломало сохранение,
--    если у класса два урока одного предмета в день. Отметка теперь уникальна в рамках урока
ALTER TABLE attendance DROP CONSTRAINT IF EXISTS uq_attendance_student_discipline_date;
CREATE UNIQUE INDEX IF NOT EXISTS uq_attendance_student_schedule ON attendance (student_id, schedule_id) WHERE schedule_id IS NOT NULL;

-- 3. Момент последней смены пароля: токены, выданные раньше, перестают действовать
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP WITH TIME ZONE;
