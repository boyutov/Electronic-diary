ALTER TABLE students
    ADD COLUMN email VARCHAR(255);

CREATE UNIQUE INDEX uq_students_email
    ON students (email)
    WHERE email IS NOT NULL;
