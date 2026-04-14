CREATE TABLE canteen_menus (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    image_data TEXT,
    image_type VARCHAR(50),
    menu_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT now(),
    created_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE canteen_menu_groups (
    menu_id INTEGER NOT NULL REFERENCES canteen_menus(id) ON DELETE CASCADE,
    group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (menu_id, group_id)
);
