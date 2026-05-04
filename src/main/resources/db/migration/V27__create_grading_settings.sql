CREATE TABLE grading_settings (
    id SERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    period_type VARCHAR(20) NOT NULL DEFAULT 'QUARTER', -- QUARTER, SEMESTER, YEAR
    academic_year_start INTEGER NOT NULL DEFAULT 9,     -- месяц начала учебного года (9 = сентябрь)
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE (school_id)
);
