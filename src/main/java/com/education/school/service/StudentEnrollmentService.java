package com.education.school.service;

import com.education.school.dto.StudentRequest;
import com.education.school.entity.GroupEntity;
import com.education.school.entity.Student;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// Зачисление ученика из списка (Excel или ИИ-ассистент):
// - есть почта ученика → она и есть логин, пароль приходит ученику;
// - почты нет, есть почта родителя (младшие классы) → логин генерируется, пароль приходит родителю,
//   а родитель получает свой аккаунт, привязанный к ребёнку
@Service
@RequiredArgsConstructor
public class StudentEnrollmentService {

    private final StudentService studentService;
    private final ParentService parentService;
    private final UserRepository userRepository;
    private final CurrentSchoolService currentSchoolService;

    public record Enrollment(String secondName, String firstName, String thirdName,
                             String studentEmail, String parentEmail,
                             String parentSecondName, String parentFirstName,
                             Integer age, GroupEntity group) {}

    // Одна строка — одна транзакция: при ошибке не создаётся ни ученик, ни родитель, и письма не уходят
    @Transactional
    public String enroll(Enrollment e) {
        boolean ownEmail = notBlank(e.studentEmail());
        boolean parentEmail = notBlank(e.parentEmail());
        if (!notBlank(e.firstName()) || !notBlank(e.secondName())) {
            throw new IllegalArgumentException("нет фамилии или имени");
        }
        if (!ownEmail && !parentEmail) {
            throw new IllegalArgumentException("нет ни почты ученика, ни почты родителя — пароль некуда отправить");
        }

        String login = ownEmail ? e.studentEmail().trim() : generateLogin(e.firstName(), e.secondName());
        int age = e.age() != null ? e.age() : (e.group().getCourse() != null ? e.group().getCourse() + 6 : 15);
        StudentRequest request = new StudentRequest(e.firstName().trim(), e.secondName().trim(),
                e.thirdName() != null ? e.thirdName().trim() : null, login, null, age, e.group().getId());

        Student student = studentService.create(request, ownEmail ? null : e.parentEmail().trim());
        if (parentEmail) {
            parentService.linkToChild(e.parentEmail().trim(), e.parentFirstName(), e.parentSecondName(), student);
        }

        String name = request.secondName() + " " + request.firstName();
        return ownEmail ? name : name + " (логин " + login + ", пароль у родителя)";
    }

    // Логин для ребёнка без почты: имя.фамилия@школа.diary (почтового ящика нет — это только логин)
    private String generateLogin(String firstName, String secondName) {
        String school = transliterate(currentSchoolService.get().getName());
        String base = transliterate(firstName) + "." + transliterate(secondName);
        String login = base + "@" + school + ".diary";
        for (int n = 2; userRepository.existsByEmailIgnoreCase(login); n++) {
            login = base + n + "@" + school + ".diary";
        }
        return login;
    }

    private static final Map<Character, String> TRANSLIT = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"), Map.entry('д', "d"),
            Map.entry('е', "e"), Map.entry('ё', "yo"), Map.entry('ж', "zh"), Map.entry('з', "z"), Map.entry('и', "i"),
            Map.entry('й', "y"), Map.entry('к', "k"), Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"),
            Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "kh"), Map.entry('ц', "ts"), Map.entry('ч', "ch"),
            Map.entry('ш', "sh"), Map.entry('щ', "sch"), Map.entry('ъ', ""), Map.entry('ы', "y"), Map.entry('ь', ""),
            Map.entry('э', "e"), Map.entry('ю', "yu"), Map.entry('я', "ya"),
            // кыргызские буквы
            Map.entry('ө', "o"), Map.entry('ү', "u"), Map.entry('ң', "ng")
    );

    static String transliterate(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.trim().toLowerCase().toCharArray()) {
            sb.append(TRANSLIT.getOrDefault(c, String.valueOf(c)));
        }
        // в логине оставляем только латиницу, цифры, точку и дефис
        return sb.toString().replaceAll("[^a-z0-9.-]", "");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
