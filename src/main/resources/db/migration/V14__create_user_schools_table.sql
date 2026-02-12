CREATE TABLE user_schools (
    user_id BIGINT NOT NULL,
    school_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, school_id),
    CONSTRAINT fk_user_schools_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_schools_school FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE CASCADE
);
