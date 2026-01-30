-- Align foreign keys to users.id (BIGINT)
ALTER TABLE users
    ALTER COLUMN created_by_admin_user_id TYPE BIGINT USING created_by_admin_user_id::BIGINT,
    ALTER COLUMN deleted_by_admin_user_id TYPE BIGINT USING deleted_by_admin_user_id::BIGINT;

ALTER TABLE complaints
    ALTER COLUMN author_user_id TYPE BIGINT USING author_user_id::BIGINT;

ALTER TABLE groups
    ALTER COLUMN curator TYPE BIGINT USING curator::BIGINT,
    ALTER COLUMN deleted_by_admin_user_id TYPE BIGINT USING deleted_by_admin_user_id::BIGINT,
    ALTER COLUMN created_by_admin_user_id TYPE BIGINT USING created_by_admin_user_id::BIGINT;

ALTER TABLE students
    ALTER COLUMN curator TYPE BIGINT USING curator::BIGINT,
    ALTER COLUMN deleted_by_admin_user_id TYPE BIGINT USING deleted_by_admin_user_id::BIGINT,
    ALTER COLUMN created_by_admin_user_id TYPE BIGINT USING created_by_admin_user_id::BIGINT;

ALTER TABLE teachers
    ALTER COLUMN user_id TYPE BIGINT USING user_id::BIGINT,
    ALTER COLUMN deleted_by_admin_user_id TYPE BIGINT USING deleted_by_admin_user_id::BIGINT,
    ALTER COLUMN created_by_admin_user_id TYPE BIGINT USING created_by_admin_user_id::BIGINT;

ALTER TABLE news
    ALTER COLUMN created_by_user_id TYPE BIGINT USING created_by_user_id::BIGINT;

ALTER TABLE polls
    ALTER COLUMN created_by_user_id TYPE BIGINT USING created_by_user_id::BIGINT;

ALTER TABLE poll_votes
    ALTER COLUMN user_id TYPE BIGINT USING user_id::BIGINT;

-- Align foreign keys to students.id (BIGINT)
ALTER TABLE attendance
    ALTER COLUMN student_id TYPE BIGINT USING student_id::BIGINT;

ALTER TABLE marks
    ALTER COLUMN student_id TYPE BIGINT USING student_id::BIGINT;

ALTER TABLE parent_student
    ALTER COLUMN student_id TYPE BIGINT USING student_id::BIGINT;

ALTER TABLE student_course
    ALTER COLUMN student_id TYPE BIGINT USING student_id::BIGINT;
