CREATE TABLE schools (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    contact_email VARCHAR(150) NOT NULL,
    contact_phone VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    director_user_id BIGINT REFERENCES users(id)
);
