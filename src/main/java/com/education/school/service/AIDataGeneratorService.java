package com.education.school.service;

import com.education.school.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AIDataGeneratorService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DisciplineService disciplineService;
    private final GroupService groupService;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final ParentService parentService;
    private final MarkService markService;
    private final NewsService newsService;
    private final ComplaintService complaintService;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    private final Random random = new Random();

    // Данные для генерации
    private static final List<String> LAST_NAMES_M = List.of(
        "Смирнов","Иванов","Кузнецов","Попов","Соколов","Лебедев","Козлов","Новиков",
        "Морозов","Петров","Волков","Соловьёв","Васильев","Зайцев","Павлов","Семёнов",
        "Голубев","Виноградов","Богданов","Воробьёв","Фёдоров","Михайлов","Беляев","Тарасов"
    );
    private static final List<String> LAST_NAMES_F = List.of(
        "Смирнова","Иванова","Кузнецова","Попова","Соколова","Лебедева","Козлова","Новикова",
        "Морозова","Петрова","Волкова","Соловьёва","Васильева","Зайцева","Павлова","Семёнова",
        "Голубева","Виноградова","Богданова","Воробьёва","Фёдорова","Михайлова","Беляева","Тарасова"
    );
    private static final List<String> FIRST_NAMES_M = List.of(
        "Александр","Дмитрий","Максим","Сергей","Андрей","Алексей","Артём","Илья",
        "Кирилл","Михаил","Никита","Роман","Егор","Иван","Владимир","Павел","Тимур","Даниил"
    );
    private static final List<String> FIRST_NAMES_F = List.of(
        "Анна","Мария","Елена","Ольга","Татьяна","Наталья","Екатерина","Юлия",
        "Ирина","Светлана","Виктория","Алина","Дарья","Полина","Валерия","Ксения","Анастасия"
    );
    private static final List<String> PATRONYMICS_M = List.of(
        "Александрович","Дмитриевич","Сергеевич","Андреевич","Алексеевич","Михайлович","Владимирович","Николаевич"
    );
    private static final List<String> PATRONYMICS_F = List.of(
        "Александровна","Дмитриевна","Сергеевна","Андреевна","Алексеевна","Михайловна","Владимировна","Николаевна"
    );
    private static final List<String> NEWS_TITLES = List.of(
        "Начало учебного года","День открытых дверей","Школьная олимпиада по математике",
        "Родительское собрание","Осенние каникулы","Спортивный праздник",
        "Конкурс рисунков","Экскурсия в музей","Выпускной вечер","Итоги четверти",
        "Новогодний утренник","День учителя","Субботник","Школьный театр","Научная конференция"
    );
    private static final List<String> NEWS_TEXTS = List.of(
        "Поздравляем всех учеников и учителей с началом нового учебного года! Желаем успехов в учёбе.",
        "Приглашаем всех желающих на день открытых дверей. Вы сможете познакомиться с нашей школой.",
        "Объявляется школьная олимпиада. Победители получат грамоты и ценные призы.",
        "Уважаемые родители, приглашаем вас на родительское собрание в актовый зал.",
        "Расписание осенних каникул опубликовано на сайте школы.",
        "В эту пятницу состоится спортивный праздник. Приходите поддержать наших спортсменов!",
        "Объявляется конкурс рисунков на тему родного города. Принимаются работы до конца месяца.",
        "Учащиеся 7-х классов отправятся на экскурсию в краеведческий музей.",
        "Поздравляем выпускников! Желаем успехов в дальнейшей жизни.",
        "Подведены итоги первой четверти. Лучшие ученики награждены грамотами."
    );

    public String generateWithAI(String userPrompt, String schoolName) {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            return "❌ AI недоступен. Настройте OPENAI_API_KEY.";
        }

        try {
            String jsonPlan = getCommandsFromAI(userPrompt);
            return executeCommands(jsonPlan, schoolName);
        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }

    // GPT возвращает ТОЛЬКО JSON с командами — никаких данных, только числа и названия
    private String getCommandsFromAI(String userPrompt) {
        String systemPrompt = """
            Ты парсер команд для школьной системы. Твоя задача — извлечь из запроса пользователя параметры и вернуть ТОЛЬКО валидный JSON.
            
            Формат ответа (строго JSON, без markdown, без пояснений):
            {
              "disciplines": ["Математика", "Физика"],
              "groups": [
                {"name": "9А", "course": 9},
                {"name": "10Б", "course": 10}
              ],
              "teachers_count": 5,
              "students_per_group": 25,
              "parents": true,
              "marks_per_student": 5,
              "news_count": 3,
              "complaints_count": 2
            }
            
            Правила:
            - Если пользователь не указал количество — используй разумные значения по умолчанию
            - course для группы — это номер класса (5А → 5, 10Б → 10, 11А → 11)
            - Если предметы не указаны — придумай стандартные школьные предметы
            - Отвечай ТОЛЬКО JSON, ничего больше
            """;

        return callOpenAI(systemPrompt, userPrompt);
    }

    private String executeCommands(String jsonPlan, String schoolName) {
        StringBuilder result = new StringBuilder();
        result.append("🚀 Начинаю генерацию данных...\n\n");

        try {
            JsonNode plan = objectMapper.readTree(jsonPlan);

            // 1. Предметы
            List<Integer> disciplineIds = new ArrayList<>();
            if (plan.has("disciplines")) {
                for (JsonNode d : plan.get("disciplines")) {
                    try {
                        var disc = disciplineService.create(d.asText());
                        disciplineIds.add(disc.getId());
                    } catch (Exception ignored) {
                        // предмет уже существует — найдём его
                    }
                }
                // подгружаем все существующие
                disciplineIds.clear();
                disciplineService.findAll().forEach(d -> disciplineIds.add(d.getId()));
                result.append("✅ Предметы: ").append(disciplineIds.size()).append("\n");
            }

            // 2. Классы
            List<Integer> groupIds = new ArrayList<>();
            if (plan.has("groups")) {
                for (JsonNode g : plan.get("groups")) {
                    try {
                        int course = g.has("course") ? g.get("course").asInt() : 9;
                        var group = groupService.create(new GroupRequest(
                            g.get("name").asText(), false, null, course, "budget"
                        ));
                        groupIds.add(group.getId());
                    } catch (Exception ignored) {}
                }
                groupIds.clear();
                groupService.findAll().forEach(g -> groupIds.add(g.getId()));
                result.append("✅ Классы: ").append(groupIds.size()).append("\n");
            }

            // 3. Учителя
            List<Integer> teacherIds = new ArrayList<>();
            if (plan.has("teachers_count") && !disciplineIds.isEmpty()) {
                int count = plan.get("teachers_count").asInt();
                Set<String> usedEmails = new HashSet<>();
                for (int i = 0; i < count; i++) {
                    boolean isFemale = random.nextBoolean();
                    String firstName = isFemale
                        ? FIRST_NAMES_F.get(random.nextInt(FIRST_NAMES_F.size()))
                        : FIRST_NAMES_M.get(random.nextInt(FIRST_NAMES_M.size()));
                    String lastName = isFemale
                        ? LAST_NAMES_F.get(random.nextInt(LAST_NAMES_F.size()))
                        : LAST_NAMES_M.get(random.nextInt(LAST_NAMES_M.size()));
                    String patronymic = isFemale
                        ? PATRONYMICS_F.get(random.nextInt(PATRONYMICS_F.size()))
                        : PATRONYMICS_M.get(random.nextInt(PATRONYMICS_M.size()));

                    String email = generateUniqueEmail(firstName, lastName, i, "teacher.school", usedEmails);
                    int discId = disciplineIds.get(i % disciplineIds.size());

                    try {
                        var teacher = teacherService.create(new TeacherRequest(
                            firstName, lastName, patronymic,
                            email, "Pass123!",
                            "+7" + (9000000000L + random.nextInt(999999999)),
                            "Опытный преподаватель с высшей категорией",
                            false, null, false, null,
                            List.of(discId)
                        ));
                        teacherIds.add(teacher.getId());
                    } catch (Exception ignored) {}
                }
                result.append("✅ Учителя: ").append(teacherIds.size()).append("\n");
            }

            // 4. Ученики
            List<Long> studentIds = new ArrayList<>();
            if (plan.has("students_per_group") && !groupIds.isEmpty()) {
                int perGroup = plan.get("students_per_group").asInt();
                Set<String> usedEmails = new HashSet<>();
                for (int groupId : groupIds) {
                    for (int i = 0; i < perGroup; i++) {
                        boolean isFemale = random.nextBoolean();
                        String firstName = isFemale
                            ? FIRST_NAMES_F.get(random.nextInt(FIRST_NAMES_F.size()))
                            : FIRST_NAMES_M.get(random.nextInt(FIRST_NAMES_M.size()));
                        String lastName = isFemale
                            ? LAST_NAMES_F.get(random.nextInt(LAST_NAMES_F.size()))
                            : LAST_NAMES_M.get(random.nextInt(LAST_NAMES_M.size()));
                        String patronymic = isFemale
                            ? PATRONYMICS_F.get(random.nextInt(PATRONYMICS_F.size()))
                            : PATRONYMICS_M.get(random.nextInt(PATRONYMICS_M.size()));

                        String email = generateUniqueEmail(firstName, lastName,
                            studentIds.size(), "student.school", usedEmails);

                        try {
                            var student = studentService.create(new StudentRequest(
                                firstName, lastName, patronymic,
                                email, "Pass123!",
                                11 + random.nextInt(7),
                                groupId
                            ));
                            studentIds.add(student.getId());
                        } catch (Exception ignored) {}
                    }
                }
                result.append("✅ Ученики: ").append(studentIds.size()).append("\n");
            }

            // 5. Родители
            if (plan.has("parents") && plan.get("parents").asBoolean() && !studentIds.isEmpty()) {
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
                int newsCount = createNews(count, teacherIds);
                result.append("✅ Новости: ").append(newsCount).append("\n");
            }

            // 8. Жалобы
            if (plan.has("complaints_count")) {
                int count = plan.get("complaints_count").asInt();
                createComplaints(count);
                result.append("✅ Жалобы: ").append(count).append("\n");
            }

            result.append("\n🎉 Готово!");

        } catch (Exception e) {
            result.append("❌ Ошибка выполнения: ").append(e.getMessage());
        }

        return result.toString();
    }

    private int createParents(List<Long> studentIds) {
        Set<String> usedEmails = new HashSet<>();
        int count = 0;
        // Один родитель на 1-2 ребёнка
        for (int i = 0; i < studentIds.size(); i += 1 + random.nextInt(2)) {
            boolean isFemale = random.nextBoolean();
            String firstName = isFemale
                ? FIRST_NAMES_F.get(random.nextInt(FIRST_NAMES_F.size()))
                : FIRST_NAMES_M.get(random.nextInt(FIRST_NAMES_M.size()));
            String lastName = isFemale
                ? LAST_NAMES_F.get(random.nextInt(LAST_NAMES_F.size()))
                : LAST_NAMES_M.get(random.nextInt(LAST_NAMES_M.size()));
            String patronymic = isFemale
                ? PATRONYMICS_F.get(random.nextInt(PATRONYMICS_F.size()))
                : PATRONYMICS_M.get(random.nextInt(PATRONYMICS_M.size()));

            String email = generateUniqueEmail(firstName, lastName, count, "parent.school", usedEmails);

            List<Long> children = new ArrayList<>();
            children.add(studentIds.get(i));
            if (i + 1 < studentIds.size() && random.nextBoolean()) {
                children.add(studentIds.get(i + 1));
            }

            try {
                parentService.create(new ParentRequest(
                    firstName, lastName, patronymic,
                    email, "Pass123!",
                    "+7" + (9000000000L + random.nextInt(999999999)),
                    children
                ));
                count++;
            } catch (Exception ignored) {}
        }
        return count;
    }

    private int createMarks(List<Long> studentIds, List<Integer> disciplineIds, int perStudent) {
        int count = 0;
        LocalDate today = LocalDate.now();
        for (Long studentId : studentIds) {
            for (int i = 0; i < perStudent; i++) {
                int discId = disciplineIds.get(random.nextInt(disciplineIds.size()));
                int value = 3 + random.nextInt(3); // 3, 4 или 5
                LocalDate markDate = today.minusDays(random.nextInt(30));
                try {
                    markService.create(new MarkRequest(studentId, discId, value, null, markDate));
                    count++;
                } catch (Exception ignored) {}
            }
        }
        return count;
    }

    private int createNews(int count, List<Integer> teacherIds) {
        int created = 0;
        for (int i = 0; i < Math.min(count, NEWS_TITLES.size()); i++) {
            Integer teacherId = teacherIds.isEmpty() ? null : teacherIds.get(i % teacherIds.size());
            try {
                newsService.create(new NewsRequest(NEWS_TITLES.get(i), NEWS_TEXTS.get(i % NEWS_TEXTS.size()), teacherId));
                created++;
            } catch (Exception ignored) {}
        }
        return created;
    }

    private void createComplaints(int count) {
        List<String> texts = List.of(
            "Прошу рассмотреть вопрос об улучшении питания в столовой",
            "Предлагаю добавить дополнительные занятия по математике",
            "Вопрос по расписанию на следующую четверть",
            "Просьба рассмотреть возможность проведения экскурсий",
            "Предложение по организации внеклассных мероприятий"
        );
        for (int i = 0; i < Math.min(count, texts.size()); i++) {
            try {
                complaintService.create(new ComplaintRequest(texts.get(i), random.nextBoolean()));
            } catch (Exception ignored) {}
        }
    }

    private String generateUniqueEmail(String firstName, String lastName, int index, String domain, Set<String> used) {
        String base = transliterate(firstName).toLowerCase() + "."
            + transliterate(lastName).toLowerCase() + index;
        String email = base + "@" + domain + ".ru";
        int attempt = 0;
        while (used.contains(email)) {
            email = base + (++attempt) + "@" + domain + ".ru";
        }
        used.add(email);
        return email;
    }

    private String transliterate(String text) {
        Map<Character, String> map = Map.ofEntries(
            Map.entry('а',"a"), Map.entry('б',"b"), Map.entry('в',"v"), Map.entry('г',"g"),
            Map.entry('д',"d"), Map.entry('е',"e"), Map.entry('ё',"yo"), Map.entry('ж',"zh"),
            Map.entry('з',"z"), Map.entry('и',"i"), Map.entry('й',"y"), Map.entry('к',"k"),
            Map.entry('л',"l"), Map.entry('м',"m"), Map.entry('н',"n"), Map.entry('о',"o"),
            Map.entry('п',"p"), Map.entry('р',"r"), Map.entry('с',"s"), Map.entry('т',"t"),
            Map.entry('у',"u"), Map.entry('ф',"f"), Map.entry('х',"kh"), Map.entry('ц',"ts"),
            Map.entry('ч',"ch"), Map.entry('ш',"sh"), Map.entry('щ',"sch"), Map.entry('ъ',""),
            Map.entry('ы',"y"), Map.entry('ь',""), Map.entry('э',"e"), Map.entry('ю',"yu"),
            Map.entry('я',"ya")
        );
        StringBuilder sb = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            sb.append(map.getOrDefault(c, String.valueOf(c)));
        }
        return sb.toString();
    }

    private String callOpenAI(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> body = Map.of(
            "model", "gpt-4o-mini",
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", 0.1,
            "max_tokens", 500
        );

        ResponseEntity<Map> response = restTemplate.exchange(
            "https://api.openai.com/v1/chat/completions",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
