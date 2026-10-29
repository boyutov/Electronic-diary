-- Даты периодов, заданные школой: JSON {"QUARTER":[["09-01","10-31"],...],"SEMESTER":[...],"YEAR":[...]}.
-- Пусто — используется обычный школьный календарь
ALTER TABLE grading_settings ADD COLUMN custom_periods TEXT;
