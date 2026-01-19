CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       first_name VARCHAR(50) NOT NULL,
                       second_name VARCHAR(50) NOT NULL,
                       third_name VARCHAR(50),
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(100) NOT NULL,
                       created_by_admin_user_id INTEGER REFERENCES users(id),
                       deleted_by_admin_user_id INTEGER REFERENCES users(id),
                       role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE RESTRICT
);

INSERT INTO roles (name) VALUES
                             ('STUDENT'),
                             ('TEACHER'),
                             ('DIRECTOR'),
                             ('ADMIN'),
                             ('PARENT'),
                             ('MINISTRY');

CREATE TABLE complaints (
                            id SERIAL PRIMARY KEY,
                            content TEXT NOT NULL,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
                            deleted_at TIMESTAMP WITH TIME ZONE,
    -- если жалоба подана анонимно — true. Автор всё равно сохраняется, но не показывается.
                            is_anonymous BOOLEAN DEFAULT TRUE NOT NULL,
                            author_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE groups (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(50) NOT NULL,
                        curator INTEGER NOT NULL REFERENCES users(id),
                        has_office BOOLEAN DEFAULT FALSE,
                        office VARCHAR(100),
                        course INTEGER,
                        deleted_by_admin_user_id INTEGER REFERENCES users(id),
                        created_by_admin_user_id INTEGER REFERENCES users(id)
);

CREATE TABLE students (
                          id SERIAL PRIMARY KEY,
                          user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                          curator INTEGER NOT NULL REFERENCES users(id),
                          age INTEGER NOT NULL,
                          group_id INTEGER REFERENCES groups(id) ON DELETE RESTRICT,
                          deleted_by_admin_user_id INTEGER REFERENCES users(id),
                          created_by_admin_user_id INTEGER REFERENCES users(id)
);

CREATE TABLE teachers (
                          id SERIAL PRIMARY KEY,
                          user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                          bio TEXT,
                          has_office BOOLEAN DEFAULT FALSE,
                          office VARCHAR(100),
                          has_group BOOLEAN DEFAULT FALSE,
                          group_id INTEGER NOT NULL REFERENCES groups(id),
                          phone VARCHAR(50),
                          deleted_by_admin_user_id INTEGER REFERENCES users(id),
                          created_by_admin_user_id INTEGER REFERENCES users(id)
);

CREATE TABLE parents (
                         id SERIAL PRIMARY KEY,
                         user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                         phone VARCHAR(50)
);

CREATE TABLE parent_student (
                                parent_id INTEGER REFERENCES parents(id) ON DELETE CASCADE,
                                student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
                                PRIMARY KEY (parent_id, student_id)
);

CREATE TABLE courses (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    teacher_id INTEGER NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    description TEXT
);

CREATE TABLE student_course (
    student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    course_id INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    PRIMARY KEY (student_id, course_id)
);

CREATE TABLE disciplines (
                             id SERIAL PRIMARY KEY,
                             name VARCHAR(100) NOT NULL
);

CREATE TABLE teacher_discipline (
                                    teacher_id INTEGER NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
                                    discipline_id INTEGER NOT NULL REFERENCES disciplines(id) ON DELETE CASCADE,
                                    PRIMARY KEY (teacher_id, discipline_id)
);

CREATE TABLE marks (
                       id SERIAL PRIMARY KEY,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
                       deleted_at TIMESTAMP WITH TIME ZONE,
                       student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE,
                       discipline_id INTEGER NOT NULL REFERENCES disciplines(id),
                       value INTEGER CHECK (value BETWEEN 1 AND 100) NOT NULL,
                       given_by_teacher_id INTEGER REFERENCES teachers(id) NOT NULL,
                       comment TEXT
);

CREATE TABLE schedule (
                          id SERIAL PRIMARY KEY,
                          discipline_id INTEGER NOT NULL REFERENCES disciplines(id),
                          teacher_id INTEGER NOT NULL REFERENCES teachers(id),
                          group_id INTEGER NOT NULL REFERENCES groups(id),  -- или grade + letter
                          day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1=Пн, 7=Вс
                          lesson_number INTEGER NOT NULL CHECK (lesson_number BETWEEN 1 AND 8), -- номер урока в дне
                          classroom VARCHAR(20),

                          UNIQUE (day_of_week, lesson_number, group_id) -- один урок в одно время
);

CREATE TABLE attendance (
                            id SERIAL PRIMARY KEY,
                            student_id INTEGER REFERENCES students(id),
                            discipline_id INTEGER REFERENCES disciplines(id),
                            lesson_date DATE NOT NULL,
                            status VARCHAR(20) CHECK (status IN ('present','absent','late','reasonable')),
                            late_for_in_minutes INTEGER,
                            comment TEXT
                        );

CREATE TABLE news (
                      id SERIAL PRIMARY KEY,
                      title VARCHAR(200) NOT NULL,
                      text TEXT NOT NULL,
                      created_at TIMESTAMP DEFAULT now(),
                      teacher_id INTEGER REFERENCES teachers(id),
                      created_by_user_id INTEGER REFERENCES users(id)
);

CREATE TABLE polls (
                       id SERIAL PRIMARY KEY,
                       title VARCHAR(300) NOT NULL,
                       description TEXT,
                       created_by_user_id INTEGER REFERENCES users(id),
                       created_at TIMESTAMP DEFAULT now(),
                       active BOOLEAN DEFAULT TRUE
);

CREATE TABLE poll_options (
                              id SERIAL PRIMARY KEY,
                              poll_id INTEGER NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
                              option_text VARCHAR(300) NOT NULL
);

CREATE TABLE poll_votes (
                            id SERIAL PRIMARY KEY,
                            poll_id INTEGER NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
                            option_id INTEGER NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
                            user_id INTEGER NOT NULL REFERENCES users(id),
                            voted_at TIMESTAMP DEFAULT now(),
                            UNIQUE (poll_id, user_id)
);

CREATE TABLE permissions (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(50) NOT NULL,
                            role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE RESTRICT

);

INSERT INTO permissions (name, role_id) VALUES
    ('Can grade students', 2),
    ('Can mark student attendance', 2),
    ('Can operate on users', 4),
    ('Can operate on polls', 4),
    ('Can operate on news', 4),
    ('Can operate on schedule', 4),
    ('Can watch complaints', 3),
    ('Can watch complaints', 6),
    ('Can operate on courses', 4),
    ('Can operate on groups', 4),
    ('Can operate on disciplines', 4);