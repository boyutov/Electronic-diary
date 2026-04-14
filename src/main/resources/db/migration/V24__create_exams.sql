CREATE TABLE exams (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'EXAM',  -- EXAM, TEST, QUIZ
    description TEXT,
    exam_date DATE NOT NULL,
    exam_time TIME,
    discipline_id INTEGER REFERENCES disciplines(id) ON DELETE SET NULL,
    created_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE exam_groups (
    exam_id INTEGER NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (exam_id, group_id)
);
