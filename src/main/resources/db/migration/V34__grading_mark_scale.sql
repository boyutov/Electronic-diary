-- Шкала оценок школы: FIVE (2–5) или HUNDRED (1–100). Существующие школы остаются на 100-балльной
ALTER TABLE grading_settings ADD COLUMN mark_scale VARCHAR(10) NOT NULL DEFAULT 'HUNDRED';
