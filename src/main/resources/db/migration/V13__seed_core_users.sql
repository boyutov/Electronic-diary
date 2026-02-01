-- Seed core roles/users for demo
INSERT INTO users (first_name, second_name, third_name, password, email, role_id)
SELECT 'Системный', 'Администратор', NULL, '$2a$10$7EqJtq98hPqEX7fNZaFWoO0jS0DqQfQd9K5xD2YWS9Vyuk3F7S3y', 'admin@school.kz', id
FROM roles WHERE name = 'ADMIN';

INSERT INTO users (first_name, second_name, third_name, password, email, role_id)
SELECT 'Алия', 'Кудайберген', 'Сериковна', '$2a$10$7EqJtq98hPqEX7fNZaFWoO0jS0DqQfQd9K5xD2YWS9Vyuk3F7S3y', 'director@school.kz', id
FROM roles WHERE name = 'DIRECTOR';

INSERT INTO users (first_name, second_name, third_name, password, email, role_id)
SELECT 'Мирас', 'Садыков', 'Айбекович', '$2a$10$7EqJtq98hPqEX7fNZaFWoO0jS0DqQfQd9K5xD2YWS9Vyuk3F7S3y', 'teacher@school.kz', id
FROM roles WHERE name = 'TEACHER';

INSERT INTO users (first_name, second_name, third_name, password, email, role_id)
SELECT 'Айгуль', 'Нуртаева', 'Ильясовна', '$2a$10$7EqJtq98hPqEX7fNZaFWoO0jS0DqQfQd9K5xD2YWS9Vyuk3F7S3y', 'parent@school.kz', id
FROM roles WHERE name = 'PARENT';

INSERT INTO users (first_name, second_name, third_name, password, email, role_id)
SELECT 'Азамат', 'Касымов', 'Русланович', '$2a$10$7EqJtq98hPqEX7fNZaFWoO0jS0DqQfQd9K5xD2YWS9Vyuk3F7S3y', 'student@school.kz', id
FROM roles WHERE name = 'STUDENT';

INSERT INTO groups (name, curator, has_office, office, course)
SELECT '10А', (SELECT id FROM users WHERE email = 'teacher@school.kz'), true, '203', 1;

INSERT INTO teachers (user_id, bio, has_office, office, has_group, group_id, phone)
VALUES ((SELECT id FROM users WHERE email = 'teacher@school.kz'), 'Учитель математики', true, '203', true,
        (SELECT id FROM groups WHERE name = '10А'), '+7 700 000 00 00');

INSERT INTO parents (user_id, phone)
VALUES ((SELECT id FROM users WHERE email = 'parent@school.kz'), '+7 700 111 11 11');

INSERT INTO students (user_id, curator, age, group_id)
VALUES ((SELECT id FROM users WHERE email = 'student@school.kz'),
        (SELECT id FROM users WHERE email = 'teacher@school.kz'),
        15,
        (SELECT id FROM groups WHERE name = '10А'));

INSERT INTO parent_student (parent_id, student_id)
VALUES (
    (SELECT id FROM parents WHERE user_id = (SELECT id FROM users WHERE email = 'parent@school.kz')),
    (SELECT id FROM students WHERE user_id = (SELECT id FROM users WHERE email = 'student@school.kz'))
);
