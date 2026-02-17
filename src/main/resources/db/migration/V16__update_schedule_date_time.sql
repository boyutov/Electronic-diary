ALTER TABLE schedule ADD COLUMN date DATE;
ALTER TABLE schedule ADD COLUMN start_time TIME;
ALTER TABLE schedule ADD COLUMN end_time TIME;

-- Можно удалить day_of_week, так как дата содержит эту информацию,
-- но для совместимости пока оставим или сделаем необязательным.
ALTER TABLE schedule ALTER COLUMN day_of_week DROP NOT NULL;
