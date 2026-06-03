package com.education.school.service;

import com.education.school.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class TestDataGeneratorService {

    private final DisciplineService disciplineService;
    private final GroupService groupService;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final ParentService parentService;
    private final MarkService markService;
    private final NewsService newsService;
    private final ComplaintService complaintService;

    private final Random random = new Random();

    public String generateTestData(String schoolName) {
        StringBuilder result = new StringBuilder();
        result.append("Генерирую тестовые данные для школы: ").append(schoolName).append("\n\n");

        try {
            // 1. Создаем предметы
            result.append("1. Создаю предметы...\n");
            List<String> subjects = List.of("Математика", "Русский язык", "Физика", "Химия", "История", "Английский язык");
            for (String subject : subjects) {
                disciplineService.create(subject);
                result.append("   ✓ ").append(subject).append("\n");
            }

            // 2. Создаем классы
            result.append("\n2. Создаю классы...\n");
            List<String> classNames = List.of("9А", "10Б", "11В");
            for (String className : classNames) {
                groupService.create(new GroupRequest(className, false, null, 9, "budget"));
                result.append("   ✓ ").append(className).append("\n");
            }

            // 3. Создаем учителей
            result.append("\n3. Создаю учителей...\n");
            List<TeacherData> teachers = List.of(
                new TeacherData("Иванова", "Мария", "Петровна", "ivanova@school.com", "Математика"),
                new TeacherData("Петров", "Сергей", "Иванович", "petrov@school.com", "Физика"),
                new TeacherData("Сидорова", "Анна", "Викторовна", "sidorova@school.com", "Русский язык"),
                new TeacherData("Козлов", "Дмитрий", "Александрович", "kozlov@school.com", "Химия"),
                new TeacherData("Морозова", "Елена", "Сергеевна", "morozova@school.com", "История"),
                new TeacherData("Волкова", "Ольга", "Николаевна", "volkova@school.com", "Английский язык")
            );

            var disciplines = disciplineService.findAll();
            var groups = groupService.findAll();

            for (TeacherData teacher : teachers) {
                var discipline = disciplines.stream()
                    .filter(d -> d.getName().equals(teacher.subject))
                    .findFirst()
                    .orElse(disciplines.get(0));

                teacherService.create(new TeacherRequest(
                    teacher.firstName, teacher.lastName, teacher.middleName,
                    teacher.email, "password123", 
                    "+7900" + (1000000 + random.nextInt(9000000)),
                    "Опытный преподаватель",
                    false, null, false, null,
                    List.of(discipline.getId())
                ));
                result.append("   ✓ ").append(teacher.lastName).append(" ").append(teacher.firstName).append(" - ").append(teacher.subject).append("\n");
            }

            // 4. Создаем учеников
            result.append("\n4. Создаю учеников...\n");
            List<String> lastNames = List.of("Смирнов", "Иванов", "Кузнецов", "Попов", "Соколов", "Лебедев", "Козлов", "Новиков", "Морозов", "Петров", "Волков", "Соловьев", "Васильев", "Зайцев", "Павлов");
            List<String> firstNamesBoys = List.of("Александр", "Дмитрий", "Максим", "Сергей", "Андрей", "Алексей", "Артем", "Илья", "Кирилл", "Михаил");
            List<String> firstNamesGirls = List.of("Анна", "Мария", "Елена", "Наталья", "Ирина", "Татьяна", "Ольга", "Юлия", "Екатерина", "Светлана");
            List<String> middleNamesBoys = List.of("Александрович", "Дмитриевич", "Максимович", "Сергеевич", "Андреевич", "Алексеевич", "Артемович", "Ильич", "Кириллович", "Михайлович");
            List<String> middleNamesGirls = List.of("Александровна", "Дмитриевна", "Максимовна", "Сергеевна", "Андреевна", "Алексеевна", "Артемовна", "Ильинична", "Кирилловна", "Михайловна");

            int studentCount = 0;
            for (var group : groups) {
                int studentsInClass = group.getName().equals("9А") ? 15 : group.getName().equals("10Б") ? 12 : 10;
                
                for (int i = 0; i < studentsInClass; i++) {
                    boolean isBoy = random.nextBoolean();
                    String lastName = lastNames.get(random.nextInt(lastNames.size()));
                    String firstName = isBoy ? firstNamesBoys.get(random.nextInt(firstNamesBoys.size())) : firstNamesGirls.get(random.nextInt(firstNamesGirls.size()));
                    String middleName = isBoy ? middleNamesBoys.get(random.nextInt(middleNamesBoys.size())) : middleNamesGirls.get(random.nextInt(middleNamesGirls.size()));
                    String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + studentCount + "@student.com";

                    studentService.create(new StudentRequest(
                        firstName, lastName, middleName, email, "password123", 
                        group.getName().equals("9А") ? 15 : group.getName().equals("10Б") ? 16 : 17,
                        group.getId()
                    ));
                    studentCount++;
                }
                result.append("   ✓ ").append(group.getName()).append(" - ").append(studentsInClass).append(" учеников\n");
            }

            // 5. Создаем родителей
            result.append("\n5. Создаю родителей...\n");
            var students = studentService.findAll().stream()
                .map(StudentDto::from)
                .toList();
            int parentCount = 0;
            for (var student : students) {
                // Создаем одного родителя для каждого ученика
                String parentFirstName = random.nextBoolean() ? "Сергей" : "Елена";
                String parentEmail = "parent" + parentCount + "@parent.com";
                
                parentService.create(new ParentRequest(
                    parentFirstName, student.getSecondName(), "Владимирович",
                    parentEmail, "password123", String.valueOf(40 + random.nextInt(10)),
                    List.of(student.getId())
                ));
                parentCount++;
            }
            result.append("   ✓ Создано ").append(parentCount).append(" родителей\n");

            // 6. Создаем оценки
            result.append("\n6. Создаю оценки...\n");
            var allTeachers = teacherService.findAll();
            int markCount = 0;
            for (var student : students) {
                // Создаем 3-5 оценок для каждого ученика
                int marksPerStudent = 3 + random.nextInt(3);
                for (int i = 0; i < marksPerStudent; i++) {
                    var teacher = allTeachers.get(random.nextInt(allTeachers.size()));
                    int mark = 3 + random.nextInt(3); // оценки от 3 до 5
                    
                    markService.create(new MarkRequest(
                        student.getId(), teacher.getDisciplineIds().get(0), mark, "Текущая оценка", java.time.LocalDate.now()
                    ));
                    markCount++;
                }
            }
            result.append("   ✓ Создано ").append(markCount).append(" оценок\n");

            // 7. Создаем новости
            result.append("\n7. Создаю новости...\n");
            List<String> newsData = List.of(
                "Начало нового учебного года|Дорогие ученики и родители! Поздравляем с началом нового учебного года. Желаем успехов в учебе!",
                "День открытых дверей|15 октября в нашей школе пройдет день открытых дверей. Приглашаем всех желающих!",
                "Школьная олимпиада по математике|Объявляется школьная олимпиада по математике для учеников 9-11 классов. Регистрация до 20 октября."
            );
            
            for (String news : newsData) {
                String[] parts = news.split("\\|");
                newsService.create(new NewsRequest(parts[0], parts[1], null));
                result.append("   ✓ ").append(parts[0]).append("\n");
            }

            // 8. Создаем жалобы
            result.append("\n8. Создаю тестовые жалобы...\n");
            if (!students.isEmpty()) {
                complaintService.create(new ComplaintRequest(
                    "Проблема с расписанием: Не могу найти расписание на следующую неделю", false
                ));
                complaintService.create(new ComplaintRequest(
                    "Вопрос по оценкам: Хотел бы уточнить оценку по математике", false
                ));
                result.append("   ✓ Создано 2 тестовые жалобы\n");
            }

            result.append("\n✅ Все тестовые данные успешно созданы!\n");
            result.append("Теперь можно тестировать все функции системы.");

        } catch (Exception e) {
            result.append("\n❌ Ошибка при создании данных: ").append(e.getMessage());
        }

        return result.toString();
    }

    private record TeacherData(String lastName, String firstName, String middleName, String email, String subject) {}
}