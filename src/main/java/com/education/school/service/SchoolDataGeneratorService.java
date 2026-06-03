package com.education.school.service;

import com.education.school.dto.*;
import com.education.school.entity.*;
import com.education.school.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchoolDataGeneratorService {

    private final GroupRepository groupRepository;
    private final DisciplineRepository disciplineRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MarkRepository markRepository;
    private final NewsRepository newsRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    private final Faker faker = new Faker(new Locale("ru"));
    private final Random random = new Random();

    // GPT только парсит намерение — возвращает JSON с командами
    public String generateFromPrompt(String userPrompt, String schoolName) {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            return "❌ OPENAI_API_KEY не настроен.";
        }
        try {
            String json = parseIntentWithGPT(userPrompt);
            return executeGenerationPlan(json);
        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }

    private String parseIntentWithGPT(String userPrompt) {
        String system = """
            Ты парсер намерений. Извлеки параметры из запроса и верни ТОЛЬКО валидный JSON без markdown.
            
            Схема:
            {
              "disciplines": ["Математика", "Физика"],
              "groups": [{"name": "9А", "course": 9}, {"name": "10Б", "course": 10}],
              "teachers_count": 5,
              "students_per_group": 25,
              "create_parents": true,
              "marks_per_student": 5,
              "news_count": 3,
              "complaints_count": 2
            }
            
            Правила:
            - course = номер класса из названия (5А→5, 10Б→10, 11А→11)
            - Если не указано количество — используй разумные значения
            - Если не указаны предметы — используй стандартные школьные
            - Если не указаны группы — придумай реалистичные названия классов
            - Отвечай ТОЛЬКО JSON
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> body = Map.of(
            "model", "gpt-4o-mini",
            "messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", 0.1,
            "max_tokens", 600
        );

        ResponseEntity<Map> response = restTemplate.exchange(
            "https://api.openai.com/v1/chat/completions",
            HttpMethod.POST, new HttpEntity<>(body, headers), Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    @Transactional
    public String executeGenerationPlan(String json) {
        StringBuilder result = new StringBuilder("🚀 Генерация данных...\n\n");
        try {
            JsonNode plan = objectMapper.readTree(json);

            // 1. Предметы
            List<Integer> disciplineIds = new ArrayList<>();
            if (plan.has("disciplines")) {
                for (JsonNode d : plan.get("disciplines")) {
                    try {
                        Discipline disc = new Discipline();
                        disc.setName(d.asText());
                        disciplineIds.add(disciplineRepository.save(disc).getId());
                    } catch (Exception ignored) {}
                }
                if (disciplineIds.isEmpty()) {
                    disciplineRepository.findAll().forEach(d -> disciplineIds.add(d.getId()));
                }
                result.append("✅ Предметы: ").append(disciplineIds.size()).append("\n");
            }

            // 2. Группы
            List<Integer> groupIds = new ArrayList<>();
            if (plan.has("groups")) {
                for (JsonNode g : plan.get("groups")) {
                    GroupEntity group = new GroupEntity();
                    group.setName(g.get("name").asText());
                    int course = g.has("course") ? g.get("course").asInt() : 9;
                    group.setCourse(course);
                    group.setHasOffice(true);
                    group.setOffice(String.valueOf(100 + random.nextInt(300)));
                    group.setFundingType(random.nextBoolean() ? "BUDGET" : "CONTRACT");
                    groupIds.add(groupRepository.save(group).getId());
                }
                result.append("✅ Группы: ").append(groupIds.size()).append("\n");
            }

            // 3. Учителя
            List<Integer> teacherIds = new ArrayList<>();
            if (plan.has("teachers_count") && !disciplineIds.isEmpty()) {
                int count = plan.get("teachers_count").asInt();
                Role teacherRole = roleRepository.findByName("TEACHER")
                    .orElseThrow(() -> new IllegalStateException("Role TEACHER not found"));

                for (int i = 0; i < count; i++) {
                    boolean female = random.nextBoolean();
                    String firstName = russianFirstName(female);
                    String lastName = russianLastName(female);
                    String patronymic = russianPatronymic(female);
                    String email = uniqueEmail(firstName, lastName, i, "teacher");

                    User user = new User();
                    user.setFirstName(firstName);
                    user.setSecondName(lastName);
                    user.setThirdName(patronymic);
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode("Pass123!"));
                    user.setRole(teacherRole);
                    user = userRepository.save(user);

                    Teacher teacher = new Teacher();
                    teacher.setUser(user);
                    teacher.setPhone(russianPhone());
                    teacher.setBio("Преподаватель с опытом работы " + (5 + random.nextInt(20)) + " лет");
                    teacher.setHasOffice(true);
                    teacher.setOffice(String.valueOf(100 + random.nextInt(300)));

                    Set<Discipline> disciplines = new HashSet<>();
                    disciplines.add(disciplineRepository.findById(disciplineIds.get(i % disciplineIds.size())).orElseThrow());
                    if (disciplineIds.size() > 1 && random.nextBoolean()) {
                        disciplines.add(disciplineRepository.findById(disciplineIds.get((i + 1) % disciplineIds.size())).orElseThrow());
                    }
                    teacher.setDisciplines(disciplines);
                    teacher.setHasGroup(false);
                    teacherIds.add(teacherRepository.save(teacher).getId());
                }

                // Назначаем кураторов группам рандомно из созданных учителей
                List<Teacher> allTeachers = teacherRepository.findAllById(teacherIds);
                for (Integer groupId : groupIds) {
                    GroupEntity group = groupRepository.findById(groupId).orElseThrow();
                    Teacher curator = allTeachers.get(random.nextInt(allTeachers.size()));
                    group.setCurator(curator.getUser());
                    curator.setHasGroup(true);
                    curator.setGroup(group);
                    groupRepository.save(group);
                    teacherRepository.save(curator);
                }
                result.append("✅ Учителя: ").append(teacherIds.size()).append("\n");
            }

            // 4. Ученики
            List<Long> studentIds = new ArrayList<>();
            if (plan.has("students_per_group") && !groupIds.isEmpty()) {
                int perGroup = plan.get("students_per_group").asInt();
                Role studentRole = roleRepository.findByName("STUDENT")
                    .orElseThrow(() -> new IllegalStateException("Role STUDENT not found"));

                for (Integer groupId : groupIds) {
                    GroupEntity group = groupRepository.findById(groupId).orElseThrow();
                    for (int i = 0; i < perGroup; i++) {
                        boolean female = random.nextBoolean();
                        String firstName = russianFirstName(female);
                        String lastName = russianLastName(female);
                        String patronymic = russianPatronymic(female);
                        String email = uniqueEmail(firstName, lastName, studentIds.size(), "student");

                        User user = new User();
                        user.setFirstName(firstName);
                        user.setSecondName(lastName);
                        user.setThirdName(patronymic);
                        user.setEmail(email);
                        user.setPassword(passwordEncoder.encode("Pass123!"));
                        user.setRole(studentRole);
                        user = userRepository.save(user);

                        Student student = new Student();
                        student.setUser(user);
                        student.setAge(group.getCourse() != null ? 10 + group.getCourse() : 14);
                        student.setGroup(group);
                        student.setEmail(email);
                        studentIds.add(studentRepository.save(student).getId());
                    }
                }
                result.append("✅ Ученики: ").append(studentIds.size()).append("\n");
            }

            // 5. Родители
            if (plan.has("create_parents") && plan.get("create_parents").asBoolean() && !studentIds.isEmpty()) {
                int parentCount = createParents(studentIds);
                result.append("✅ Родители: ").append(parentCount).append("\n");
            }

            // 6. Оценки
            if (plan.has("marks_per_student") && !studentIds.isEmpty() && !disciplineIds.isEmpty()) {
                int perStudent = plan.get("marks_per_student").asInt();
                int markCount = createMarks(studentIds, disciplineIds, perStudent);
                result.append("✅ Оценки: ").append(markCount).append("\n");
            }

            // 7. Новости
            if (plan.has("news_count")) {
                int count = plan.get("news_count").asInt();
                createNews(count);
                result.append("✅ Новости: ").append(count).append("\n");
            }

            result.append("\n🎉 Готово!");
        } catch (Exception e) {
            result.append("❌ Ошибка: ").append(e.getMessage());
        }
        return result.toString();
    }

    // Создаём группы + студентов напрямую (без GPT)
    @Transactional
    public String createGroupsWithStudents(List<String> groupNames, int studentsPerGroup) {
        StringBuilder result = new StringBuilder();
        Role studentRole = roleRepository.findByName("STUDENT")
            .orElseThrow(() -> new IllegalStateException("Role STUDENT not found"));
        List<User> teachers = teacherRepository.findAll().stream()
            .map(Teacher::getUser).collect(Collectors.toList());

        for (String groupName : groupNames) {
            GroupEntity group = new GroupEntity();
            group.setName(groupName);
            group.setHasOffice(true);
            group.setOffice(String.valueOf(100 + random.nextInt(300)));
            group.setFundingType(random.nextBoolean() ? "BUDGET" : "CONTRACT");
            if (!teachers.isEmpty())
                group.setCurator(teachers.get(random.nextInt(teachers.size())));
            try {
                group.setCourse(Integer.parseInt(groupName.replaceAll("[^0-9]", "")));
            } catch (Exception ignored) {}
            group = groupRepository.save(group);

            int created = 0;
            for (int i = 0; i < studentsPerGroup; i++) {
                boolean female = random.nextBoolean();
                String firstName = russianFirstName(female);
                String lastName = russianLastName(female);
                String email = uniqueEmail(firstName, lastName, created, "student");

                User user = new User();
                user.setFirstName(firstName);
                user.setSecondName(lastName);
                user.setThirdName(russianPatronymic(female));
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode("Pass123!"));
                user.setRole(studentRole);
                user = userRepository.save(user);

                Student student = new Student();
                student.setUser(user);
                student.setAge(group.getCourse() != null ? 10 + group.getCourse() : 15);
                student.setGroup(group);
                student.setEmail(email);
                studentRepository.save(student);
                created++;
            }
            result.append("✅ ").append(groupName).append(": ").append(created).append(" студентов\n");
        }
        return result.toString();
    }

    // Создаём студентов в существующую группу
    @Transactional
    public String createStudentsInGroup(String groupName, int count) {
        GroupEntity group = groupRepository.findAll().stream()
            .filter(g -> g.getName().equalsIgnoreCase(groupName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Группа '" + groupName + "' не найдена"));

        // Если у группы нет куратора — назначаем рандомного учителя
        if (group.getCurator() == null) {
            List<Teacher> teachers = teacherRepository.findAll();
            if (!teachers.isEmpty()) {
                User curator = teachers.get(random.nextInt(teachers.size())).getUser();
                group.setCurator(curator);
                groupRepository.save(group);
            }
        }

        Role studentRole = roleRepository.findByName("STUDENT")
            .orElseThrow(() -> new IllegalStateException("Role STUDENT not found"));

        int created = 0;
        for (int i = 0; i < count; i++) {
            boolean female = random.nextBoolean();
            String firstName = russianFirstName(female);
            String lastName = russianLastName(female);
            String email = uniqueEmail(firstName, lastName, created, "student");

            User user = new User();
            user.setFirstName(firstName);
            user.setSecondName(lastName);
            user.setThirdName(russianPatronymic(female));
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("Pass123!"));
            user.setRole(studentRole);
            user = userRepository.save(user);

            Student student = new Student();
            student.setUser(user);
            student.setAge(group.getCourse() != null ? 10 + group.getCourse() : 15);
            student.setGroup(group);
            student.setEmail(email);
            studentRepository.save(student);
            created++;
        }
        return "Добавлено " + created + " студентов в группу " + groupName;
    }

    private int createParents(List<Long> studentIds) {
        Role parentRole = roleRepository.findByName("PARENT")
            .orElseThrow(() -> new IllegalStateException("Role PARENT not found"));
        int count = 0;
        for (int i = 0; i < studentIds.size(); i += 1 + random.nextInt(2)) {
            boolean female = random.nextBoolean();
            String firstName = russianFirstName(female);
            String lastName = russianLastName(female);
            String email = uniqueEmail(firstName, lastName, count, "parent");

            User user = new User();
            user.setFirstName(firstName);
            user.setSecondName(lastName);
            user.setThirdName(russianPatronymic(female));
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("Pass123!"));
            user.setRole(parentRole);
            user = userRepository.save(user);

            Parent parent = new Parent();
            parent.setUser(user);
            parent.setPhone(russianPhone());

            List<Long> childIds = new ArrayList<>();
            childIds.add(studentIds.get(i));
            if (i + 1 < studentIds.size() && random.nextBoolean())
                childIds.add(studentIds.get(i + 1));
            parent.getStudents().addAll(studentRepository.findAllById(childIds));
            parentRepository.save(parent);
            count++;
        }
        return count;
    }

    private int createMarks(List<Long> studentIds, List<Integer> disciplineIds, int perStudent) {
        int count = 0;
        List<Teacher> teachers = teacherRepository.findAll();
        if (teachers.isEmpty()) return 0;
        for (Long studentId : studentIds) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) continue;
            for (int i = 0; i < perStudent; i++) {
                Mark mark = new Mark();
                mark.setStudent(student);
                mark.setDiscipline(disciplineRepository.findById(
                    disciplineIds.get(random.nextInt(disciplineIds.size()))).orElse(null));
                mark.setValue(3 + random.nextInt(3));
                mark.setCreatedAt(java.time.OffsetDateTime.now().minusDays(random.nextInt(30)));
                mark.setGivenByTeacher(teachers.get(random.nextInt(teachers.size())));
                markRepository.save(mark);
                count++;
            }
        }
        return count;
    }

    private void createNews(int count) {
        String[] titles = {
            "Начало учебного года", "День открытых дверей", "Олимпиада по математике",
            "Родительское собрание", "Осенние каникулы", "Спортивный праздник",
            "Конкурс рисунков", "Экскурсия в музей", "Итоги четверти", "День учителя"
        };
        for (int i = 0; i < Math.min(count, titles.length); i++) {
            News news = new News();
            news.setTitle(titles[i]);
            news.setText(faker.lorem().paragraph(3));
            news.setCreatedAt(java.time.LocalDateTime.now().minusDays(random.nextInt(30)));
            newsRepository.save(news);
        }
    }

    // Русские имена через Faker
    private String russianFirstName(boolean female) {
        List<String> m = List.of("Александр","Дмитрий","Максим","Сергей","Андрей","Алексей","Артём","Илья","Кирилл","Михаил","Никита","Роман","Егор","Иван","Владимир","Павел","Тимур","Даниил");
        List<String> f = List.of("Анна","Мария","Елена","Ольга","Татьяна","Наталья","Екатерина","Юлия","Ирина","Светлана","Виктория","Алина","Дарья","Полина","Валерия","Ксения","Анастасия");
        return female ? f.get(random.nextInt(f.size())) : m.get(random.nextInt(m.size()));
    }

    private String russianLastName(boolean female) {
        List<String> m = List.of("Смирнов","Иванов","Кузнецов","Попов","Соколов","Лебедев","Козлов","Новиков","Морозов","Петров","Волков","Васильев","Зайцев","Павлов","Семёнов","Голубев","Виноградов","Богданов","Воробьёв","Фёдоров");
        List<String> f = List.of("Смирнова","Иванова","Кузнецова","Попова","Соколова","Лебедева","Козлова","Новикова","Морозова","Петрова","Волкова","Васильева","Зайцева","Павлова","Семёнова","Голубева","Виноградова","Богданова","Воробьёва","Фёдорова");
        return female ? f.get(random.nextInt(f.size())) : m.get(random.nextInt(m.size()));
    }

    private String russianPatronymic(boolean female) {
        List<String> m = List.of("Александрович","Дмитриевич","Сергеевич","Андреевич","Алексеевич","Михайлович","Владимирович","Николаевич","Игоревич","Олегович");
        List<String> f = List.of("Александровна","Дмитриевна","Сергеевна","Андреевна","Алексеевна","Михайловна","Владимировна","Николаевна","Игоревна","Олеговна");
        return female ? f.get(random.nextInt(f.size())) : m.get(random.nextInt(m.size()));
    }

    private String russianPhone() {
        return "+7" + (900 + random.nextInt(99)) + String.format("%07d", random.nextInt(10000000));
    }

    private final Set<String> usedEmails = new HashSet<>();

    private String uniqueEmail(String firstName, String lastName, int index, String domain) {
        Map<Character, String> tr = Map.ofEntries(
            Map.entry('а',"a"),Map.entry('б',"b"),Map.entry('в',"v"),Map.entry('г',"g"),
            Map.entry('д',"d"),Map.entry('е',"e"),Map.entry('ё',"yo"),Map.entry('ж',"zh"),
            Map.entry('з',"z"),Map.entry('и',"i"),Map.entry('й',"y"),Map.entry('к',"k"),
            Map.entry('л',"l"),Map.entry('м',"m"),Map.entry('н',"n"),Map.entry('о',"o"),
            Map.entry('п',"p"),Map.entry('р',"r"),Map.entry('с',"s"),Map.entry('т',"t"),
            Map.entry('у',"u"),Map.entry('ф',"f"),Map.entry('х',"kh"),Map.entry('ц',"ts"),
            Map.entry('ч',"ch"),Map.entry('ш',"sh"),Map.entry('щ',"sch"),Map.entry('ъ',""),
            Map.entry('ы',"y"),Map.entry('ь',""),Map.entry('э',"e"),Map.entry('ю',"yu"),
            Map.entry('я',"ya")
        );
        StringBuilder sb = new StringBuilder();
        for (char c : (firstName + "." + lastName).toLowerCase().toCharArray())
            sb.append(tr.getOrDefault(c, String.valueOf(c)));
        String base = sb.toString();
        String email = base + index + "@" + domain + ".school.ru";
        int attempt = 0;
        while (usedEmails.contains(email))
            email = base + index + (++attempt) + "@" + domain + ".school.ru";
        usedEmails.add(email);
        return email;
    }
}
