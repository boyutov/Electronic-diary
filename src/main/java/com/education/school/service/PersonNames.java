package com.education.school.service;

import com.education.school.entity.User;

import java.util.regex.Pattern;

// Проверка ФИО: только буквы (любого алфавита, включая кыргызские ө ү ң), пробел, дефис, апостроф и точка.
// Имя выводится на страницах у других пользователей — HTML и скрипты в нём недопустимы (защита от XSS)
public final class PersonNames {

    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L}\\p{M} .'’-]{0,49}$");

    private PersonNames() {
    }

    public static void apply(User user, String firstName, String secondName, String thirdName) {
        user.setFirstName(require(firstName, "Имя"));
        user.setSecondName(require(secondName, "Фамилия"));
        user.setThirdName(thirdName == null || thirdName.isBlank() ? null : require(thirdName, "Отчество"));
    }

    private static String require(String value, String field) {
        String trimmed = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (!NAME.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(field + " может содержать только буквы, пробел, дефис и апостроф");
        }
        return trimmed;
    }
}
