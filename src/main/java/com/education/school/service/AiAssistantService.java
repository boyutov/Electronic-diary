package com.education.school.service;

import com.education.school.dto.StudentRequest;
import com.education.school.entity.*;
import com.education.school.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final StudentService studentService;
    private final GroupRepository groupRepository;
    private final DisciplineRepository disciplineRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final MarkRepository markRepository;
    private final ScheduleRepository scheduleRepository;
    private final SchoolDataGeneratorService generatorService;
    private final CourseRepository courseRepository;

    private final Map<Long, List<Map<String, String>>> conversationHistory = new HashMap<>();
    private final Random random = new Random();

    private static final List<String> LAST_NAMES_M = List.of(
        "Смирнов","Иванов","Кузнецов","Попов","Соколов","Лебедев","Козлов","Новиков",
        "Морозов","Петров","Волков","Васильев","Зайцев","Павлов","Семёнов","Голубев",
        "Виноградов","Богданов","Воробьёв","Фёдоров","Михайлов","Беляев","Тарасов","Белов"
    );
    private static final List<String> LAST_NAMES_F = List.of(
        "Смирнова","Иванова","Кузнецова","Попова","Соколова","Лебедева","Козлова","Новикова",
        "Морозова","Петрова","Волкова","Васильева","Зайцева","Павлова","Семёнова","Голубева",
        "Виноградова","Богданова","Воробьёва","Фёдорова","Михайлова","Беляева","Тарасова","Белова"
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
    private static final List<String> OFFICES = List.of(
        "101","102","103","104","105","201","202","203","204","205","301","302","303"
    );

    public Map<String, Object> chat(String schoolName, String userMessage) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        Long userId = user.getId();
        String role = user.getRole().getName();

        List<Map<String, String>> history = conversationHistory.computeIfAbsent(userId, k -> new ArrayList<>());
        history.add(Map.of("role", "user", "content", userMessage));
        if (history.size() > 20) history = history.subList(history.size() - 20, history.size());

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", buildSystemPrompt(schoolName, role));

            for (Map<String, String> msg : history) {
                ObjectNode m = messages.addObject();
                m.put("role", msg.get("role"));
                m.put("content", msg.get("content"));
            }

            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 2000);

            ArrayNode tools = buildTools(role);
            if (tools.size() > 0) {
                requestBody.set("tools", tools);
                requestBody.put("tool_choice", "auto");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST,
                new HttpEntity<>(requestBody.toString(), headers),
                String.class
            );

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode choice = responseJson.path("choices").get(0).path("message");

            if (choice.has("tool_calls") && !choice.path("tool_calls").isEmpty()) {
                StringBuilder replyBuilder = new StringBuilder();
                boolean allSuccess = true;
                for (JsonNode toolCall : choice.path("tool_calls")) {
                    String functionName = toolCall.path("function").path("name").asText();
                    JsonNode args = objectMapper.readTree(toolCall.path("function").path("arguments").asText());
                    Map<String, Object> actionResult = executeAction(functionName, args, schoolName, user);
                    String resultMsg = (String) actionResult.get("message");
                    boolean success = Boolean.TRUE.equals(actionResult.get("success"));
                    if (!success) allSuccess = false;
                    replyBuilder.append(success ? "✅ " : "❌ ").append(resultMsg).append("\n");
                }
                String reply = replyBuilder.toString().trim();
                history.add(Map.of("role", "assistant", "content", reply));
                conversationHistory.put(userId, history);
                return Map.of("reply", reply, "success", allSuccess);
            }

            String assistantMessage = choice.path("content").asText();
            history.add(Map.of("role", "assistant", "content", assistantMessage));
            conversationHistory.put(userId, history);
            return Map.of("reply", assistantMessage, "success", true);

        } catch (Exception e) {
            return Map.of("reply", "Ошибка при обращении к AI: " + e.getMessage(), "success", false);
        }
    }

    public void clearHistory(Long userId) {
        conversationHistory.remove(userId);
    }

    private String buildSystemPrompt(String schoolName, String role) {
        String groups = groupRepository.findAll().stream().map(GroupEntity::getName).collect(Collectors.joining(", "));
        String disciplines = disciplineRepository.findAll().stream().map(Discipline::getName).collect(Collectors.joining(", "));
        long students = studentRepository.count();
        long teachers = teacherRepository.count();

        return """
            Ты — AI-ассистент системы управления школой "Electronic Diary".
            Школа: %s. Роль пользователя: %s.
            
            ПРАВИЛА ИСПОЛЬЗОВАНИЯ ИНСТРУМЕНТОВ — СТРОГО СОБЛЮДАЙ:
            1. Если нужно создать 2+ групп — ВСЕГДА используй add_multiple_groups с полным списком names[]. НИКОГДА не вызывай add_group несколько раз.
            2. Если пользователь говорит "рандомно", "сам", "любые" или не указывает данные студентов — ИСПОЛЬЗУЙ add_students_random, НИКОГДА не проси данные.
            3. Если нужно добавить студентов в несколько групп — вызывай add_students_random для каждой группы последовательно.
            4. Если группы ещё не существуют — сначала создай их, потом добавляй студентов.
            5. Для создания расписания — используй generate_schedule, он автоматически распределит учителей и предметы.
            6. Для анализа успеваемости — используй analyze_performance.
            7. Отвечай ТОЛЬКО на вопросы связанные со школой. Отвечай кратко на русском языке.
            8. Не выполняй массовое удаление без подтверждения.
            
            Текущее состояние БД:
            - Группы: %s
            - Предметы: %s
            - Учеников: %d, Учителей: %d
            """.formatted(schoolName, role, groups, disciplines, students, teachers);
    }

    private ArrayNode buildTools(String role) {
        ArrayNode tools = objectMapper.createArrayNode();

        // ── ЧТЕНИЕ ──

        // Получить группы
        addTool(tools, "get_groups", "Получить список всех групп/классов школы",
            Map.of(), List.of());

        // Получить учеников
        addTool(tools, "get_students",
            "Получить список учеников. Можно фильтровать по группе.",
            Map.of("groupName", strProp("Название группы (необязательно)")), List.of());

        // Получить учителей
        addTool(tools, "get_teachers", "Получить список учителей школы",
            Map.of(), List.of());

        // Получить предметы
        addTool(tools, "get_disciplines", "Получить список учебных предметов",
            Map.of(), List.of());

        // Получить оценки ученика
        addTool(tools, "get_student_marks",
            "Получить оценки конкретного ученика по всем предметам",
            Map.of("studentName", strProp("ФИО ученика или часть имени")), List.of("studentName"));

        // Статистика
        addTool(tools, "get_stats",
            "Получить общую статистику школы: количество учеников, учителей, групп, предметов",
            Map.of(), List.of());

        // ── ЗАПИСЬ (только для ADMIN/DIRECTOR/TEACHER) ──
        if (List.of("ADMIN", "DIRECTOR", "TEACHER").contains(role)) {

            // Добавить одного ученика
            addTool(tools, "add_student", "Добавить одного нового ученика",
                Map.of(
                    "firstName",  strProp("Имя"),
                    "secondName", strProp("Фамилия"),
                    "thirdName",  strProp("Отчество"),
                    "email",      strProp("Email"),
                    "password",   strProp("Пароль"),
                    "age",        intProp("Возраст"),
                    "groupName",  strProp("Название группы")
                ),
                List.of("firstName", "secondName", "email", "password", "age", "groupName"));

            // Добавить нескольких учеников
            ObjectNode addMany = tools.addObject();
            addMany.put("type", "function");
            ObjectNode fn = addMany.putObject("function");
            fn.put("name", "add_multiple_students");
            fn.put("description", "Добавить сразу несколько учеников одним вызовом");
            ObjectNode params = fn.putObject("parameters");
            params.put("type", "object");
            ObjectNode props = params.putObject("properties");
            props.putObject("groupName").put("type", "string").put("description", "Название группы");
            ObjectNode arr = props.putObject("students");
            arr.put("type", "array");
            ObjectNode item = arr.putObject("items");
            item.put("type", "object");
            ObjectNode ip = item.putObject("properties");
            ip.putObject("firstName").put("type", "string");
            ip.putObject("secondName").put("type", "string");
            ip.putObject("thirdName").put("type", "string");
            ip.putObject("email").put("type", "string");
            ip.putObject("password").put("type", "string");
            ip.putObject("age").put("type", "integer");
            params.putArray("required").add("groupName").add("students");
        }

        if (List.of("ADMIN", "DIRECTOR").contains(role)) {
            // Создать одну группу
            addTool(tools, "add_group", "Создать одну новую группу/класс",
                Map.of("name", strProp("Название"), "course", intProp("Номер курса")),
                List.of("name"));

            // Создать несколько групп сразу
            ObjectNode addGroups = tools.addObject();
            addGroups.put("type", "function");
            ObjectNode fnGroups = addGroups.putObject("function");
            fnGroups.put("name", "add_multiple_groups");
            fnGroups.put("description", "Создать несколько групп/классов сразу с полным заполнением всех полей. Используй когда нужно создать 2+ групп. Можно сразу указать students_per_group чтобы добавить студентов.");
            ObjectNode pgParams = fnGroups.putObject("parameters");
            pgParams.put("type", "object");
            ObjectNode pgProps = pgParams.putObject("properties");
            pgProps.putObject("count").put("type", "integer").put("description", "Количество групп");
            ObjectNode namesArr = pgProps.putObject("names");
            namesArr.put("type", "array");
            namesArr.putObject("items").put("type", "string");
            namesArr.put("description", "Список названий групп");
            pgProps.putObject("course").put("type", "integer").put("description", "Номер класса (если одинаковый для всех)");
            pgProps.putObject("students_per_group").put("type", "integer").put("description", "Количество студентов в каждой группе (если нужно сразу создать)");
            pgParams.putArray("required").add("names");

            // Создать студентов с рандомными данными
            addTool(tools, "add_students_random",
                "Создать указанное количество студентов с рандомными ФИО, email, паролями в указанную группу. Используй когда пользователь говорит 'рандомно' или не указывает данные студентов.",
                Map.of(
                    "groupName", strProp("Название группы"),
                    "count", intProp("Количество студентов")
                ),
                List.of("groupName", "count"));

            // Добавить предмет
            addTool(tools, "add_discipline", "Добавить новый учебный предмет",
                Map.of("name", strProp("Название предмета")),
                List.of("name"));

            // Удалить ученика
            addTool(tools, "delete_student", "Удалить ученика по имени",
                Map.of("studentName", strProp("ФИО ученика")),
                List.of("studentName"));

            // Сгенерировать расписание для группы
            addTool(tools, "generate_schedule",
                "Создать расписание для группы на неделю. Учителя и предметы назначаются автоматически.",
                Map.of(
                    "groupName", strProp("Название группы"),
                    "startDate", strProp("Дата начала в формате yyyy-MM-dd (необязательно)"),
                    "lessonsPerDay", intProp("Количество уроков в день (по умолчанию 5)")
                ),
                List.of("groupName"));

            // Получить расписание группы
            addTool(tools, "get_schedule",
                "Получить расписание группы",
                Map.of("groupName", strProp("Название группы")),
                List.of("groupName"));

            // Анализ успеваемости
            addTool(tools, "analyze_performance",
                "Анализ успеваемости: средние оценки по группам/предметам, лучшие и худшие ученики",
                Map.of("groupName", strProp("Название группы (необязательно)")),
                List.of());

            // Создать курс
            addTool(tools, "add_course",
                "Создать новый курс и назначить учителя",
                Map.of(
                    "name", strProp("Название курса"),
                    "teacherName", strProp("ФИО учителя"),
                    "description", strProp("Описание курса")
                ),
                List.of("name", "teacherName"));

            // Получить список курсов
            addTool(tools, "get_courses", "Получить список всех курсов",
                Map.of(), List.of());
        }

        return tools;
    }

    private String generateEmail(String firstName, String lastName, int index, Set<String> used) {
        Map<Character, String> tr = Map.ofEntries(
            Map.entry('\u0430',"a"),Map.entry('\u0431',"b"),Map.entry('\u0432',"v"),Map.entry('\u0433',"g"),
            Map.entry('\u0434',"d"),Map.entry('\u0435',"e"),Map.entry('\u0451',"yo"),Map.entry('\u0436',"zh"),
            Map.entry('\u0437',"z"),Map.entry('\u0438',"i"),Map.entry('\u0439',"y"),Map.entry('\u043a',"k"),
            Map.entry('\u043b',"l"),Map.entry('\u043c',"m"),Map.entry('\u043d',"n"),Map.entry('\u043e',"o"),
            Map.entry('\u043f',"p"),Map.entry('\u0440',"r"),Map.entry('\u0441',"s"),Map.entry('\u0442',"t"),
            Map.entry('\u0443',"u"),Map.entry('\u0444',"f"),Map.entry('\u0445',"kh"),Map.entry('\u0446',"ts"),
            Map.entry('\u0447',"ch"),Map.entry('\u0448',"sh"),Map.entry('\u0449',"sch"),Map.entry('\u044a',""),
            Map.entry('\u044b',"y"),Map.entry('\u044c',""),Map.entry('\u044d',"e"),Map.entry('\u044e',"yu"),
            Map.entry('\u044f',"ya")
        );
        StringBuilder sb = new StringBuilder();
        for (char c : (firstName + "." + lastName + index).toLowerCase().toCharArray())
            sb.append(tr.getOrDefault(c, String.valueOf(c)));
        String base = sb.toString();
        String email = base + "@student.school.ru";
        int attempt = 0;
        while (used.contains(email)) email = base + (++attempt) + "@student.school.ru";
        used.add(email);
        return email;
    }

    // Хелперы для построения tools
    private void addTool(ArrayNode tools, String name, String description,
                          Map<String, ObjectNode> properties, List<String> required) {
        ObjectNode tool = tools.addObject();
        tool.put("type", "function");
        ObjectNode fn = tool.putObject("function");
        fn.put("name", name);
        fn.put("description", description);
        ObjectNode params = fn.putObject("parameters");
        params.put("type", "object");
        ObjectNode props = params.putObject("properties");
        properties.forEach(props::set);
        ArrayNode req = params.putArray("required");
        required.forEach(req::add);
    }

    private ObjectNode strProp(String description) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("type", "string");
        n.put("description", description);
        return n;
    }

    private ObjectNode intProp(String description) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("type", "integer");
        n.put("description", description);
        return n;
    }

    private Map<String, Object> executeAction(String functionName, JsonNode args, String schoolName, User executor) {
        try {
            return switch (functionName) {

                case "get_groups" -> {
                    List<String> list = groupRepository.findAll().stream()
                        .map(g -> g.getName() + (g.getCourse() != null ? " (" + g.getCourse() + " кл.)" : "")
                            + " — " + g.getStudents().size() + " уч.")
                        .collect(Collectors.toList());
                    yield Map.of("success", true, "message",
                        list.isEmpty() ? "Групп нет" : "Группы (" + list.size() + "): " + String.join(", ", list));
                }

                case "get_students" -> {
                    String groupName = args.path("groupName").asText("");
                    List<Student> all = studentRepository.findAll();
                    if (!groupName.isBlank()) {
                        all = all.stream()
                            .filter(s -> s.getGroup() != null && s.getGroup().getName().equalsIgnoreCase(groupName))
                            .collect(Collectors.toList());
                    }
                    List<String> names = all.stream()
                        .map(s -> s.getUser().getSecondName() + " " + s.getUser().getFirstName()
                            + (s.getGroup() != null ? " [" + s.getGroup().getName() + "]" : ""))
                        .collect(Collectors.toList());
                    yield Map.of("success", true, "message",
                        names.isEmpty() ? "Учеников нет" : "Ученики (" + names.size() + "): " + String.join(", ", names));
                }

                case "get_teachers" -> {
                    List<String> list = teacherRepository.findAll().stream()
                        .map(t -> t.getUser().getSecondName() + " " + t.getUser().getFirstName()
                            + " — " + t.getDisciplines().stream().map(Discipline::getName).collect(Collectors.joining(", ")))
                        .collect(Collectors.toList());
                    yield Map.of("success", true, "message",
                        list.isEmpty() ? "Учителей нет" : "Учителя (" + list.size() + "): " + String.join("; ", list));
                }

                case "get_disciplines" -> {
                    List<String> list = disciplineRepository.findAll().stream()
                        .map(Discipline::getName).collect(Collectors.toList());
                    yield Map.of("success", true, "message",
                        list.isEmpty() ? "Предметов нет" : "Предметы: " + String.join(", ", list));
                }

                case "get_student_marks" -> {
                    String name = args.path("studentName").asText("").toLowerCase();
                    List<Student> found = studentRepository.findAll().stream()
                        .filter(s -> (s.getUser().getSecondName() + " " + s.getUser().getFirstName()).toLowerCase().contains(name))
                        .collect(Collectors.toList());
                    if (found.isEmpty()) yield Map.of("success", false, "message", "Ученик не найден: " + name);
                    Student s = found.get(0);
                    Map<String, List<Integer>> byDisc = new LinkedHashMap<>();
                    s.getMarks().stream()
                        .filter(m -> m.getDeletedAt() == null)
                        .forEach(m -> byDisc.computeIfAbsent(m.getDiscipline().getName(), k -> new ArrayList<>()).add(m.getValue()));
                    if (byDisc.isEmpty()) yield Map.of("success", true, "message",
                        s.getUser().getSecondName() + " " + s.getUser().getFirstName() + ": оценок нет");
                    String result = byDisc.entrySet().stream()
                        .map(e -> {
                            double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
                            return e.getKey() + ": " + e.getValue() + " (ср. " + String.format("%.1f", avg) + ")";
                        }).collect(Collectors.joining("; "));
                    yield Map.of("success", true, "message",
                        s.getUser().getSecondName() + " " + s.getUser().getFirstName() + " — " + result);
                }

                case "get_stats" -> {
                    long studentsCount = studentRepository.count();
                    long teachersCount = teacherRepository.count();
                    long groupsCount = groupRepository.count();
                    long disciplinesCount = disciplineRepository.count();
                    long marksCount = markRepository.count();
                    yield Map.of("success", true, "message",
                        "Статистика школы: учеников — " + studentsCount +
                        ", учителей — " + teachersCount +
                        ", групп — " + groupsCount +
                        ", предметов — " + disciplinesCount +
                        ", оценок — " + marksCount);
                }

                case "add_student" -> {
                    String groupName = args.path("groupName").asText();
                    GroupEntity group = groupRepository.findAll().stream()
                        .filter(g -> g.getName().equalsIgnoreCase(groupName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Группа '" + groupName + "' не найдена"));
                    StudentRequest req = new StudentRequest(
                        args.path("firstName").asText(),
                        args.path("secondName").asText(),
                        args.path("thirdName").asText(""),
                        args.path("email").asText(),
                        args.path("password").asText(),
                        args.path("age").asInt(18),
                        group.getId()
                    );
                    studentService.create(req);
                    yield Map.of("success", true, "message",
                        "Ученик " + req.secondName() + " " + req.firstName() + " добавлен в группу " + groupName);
                }

                case "add_multiple_students" -> {
                    String groupName = args.path("groupName").asText();
                    GroupEntity group = groupRepository.findAll().stream()
                        .filter(g -> g.getName().equalsIgnoreCase(groupName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Группа '" + groupName + "' не найдена"));
                    JsonNode students = args.path("students");
                    List<String> added = new ArrayList<>();
                    List<String> errors = new ArrayList<>();
                    for (JsonNode s : students) {
                        try {
                            StudentRequest req = new StudentRequest(
                                s.path("firstName").asText(),
                                s.path("secondName").asText(),
                                s.path("thirdName").asText(""),
                                s.path("email").asText(),
                                s.path("password").asText(),
                                s.path("age").asInt(18),
                                group.getId()
                            );
                            studentService.create(req);
                            added.add(req.secondName() + " " + req.firstName());
                        } catch (Exception e) {
                            errors.add(s.path("firstName").asText() + ": " + e.getMessage());
                        }
                    }
                    String msg = "Добавлено " + added.size() + " учеников: " + String.join(", ", added);
                    if (!errors.isEmpty()) msg += ". Ошибки: " + String.join("; ", errors);
                    yield Map.of("success", true, "message", msg);
                }

                case "add_group" -> {
                    GroupEntity group = new GroupEntity();
                    group.setName(args.path("name").asText());
                    if (args.has("course")) group.setCourse(args.path("course").asInt());
                    boolean hasOffice = random.nextBoolean();
                    group.setHasOffice(hasOffice);
                    if (hasOffice) group.setOffice(OFFICES.get(random.nextInt(OFFICES.size())));
                    group.setFundingType(random.nextBoolean() ? "BUDGET" : "CONTRACT");
                    groupRepository.save(group);
                    yield Map.of("success", true, "message", "Группа '" + group.getName() + "' создана");
                }

                case "add_multiple_groups" -> {
                    JsonNode names = args.path("names");
                    List<String> nameList = new ArrayList<>();
                    for (JsonNode n : names) nameList.add(n.asText());
                    int studentsPerGroup = args.path("students_per_group").asInt(0);
                    String msg;
                    if (studentsPerGroup > 0) {
                        msg = generatorService.createGroupsWithStudents(nameList, studentsPerGroup);
                    } else {
                        msg = generatorService.createGroupsWithStudents(nameList, 0);
                    }
                    yield Map.of("success", true, "message", msg);
                }

                case "add_students_random" -> {
                    String groupName = args.path("groupName").asText();
                    int count = args.path("count").asInt(10);
                    String msg = generatorService.createStudentsInGroup(groupName, count);
                    yield Map.of("success", true, "message", msg);
                }

                case "add_discipline" -> {
                    Discipline d = new Discipline();
                    d.setName(args.path("name").asText());
                    disciplineRepository.save(d);
                    yield Map.of("success", true, "message", "Предмет '" + d.getName() + "' добавлен");
                }

                case "delete_student" -> {
                    String name = args.path("studentName").asText("").toLowerCase();
                    List<Student> found = studentRepository.findAll().stream()
                        .filter(s -> (s.getUser().getSecondName() + " " + s.getUser().getFirstName()).toLowerCase().contains(name))
                        .collect(Collectors.toList());
                    if (found.isEmpty()) yield Map.of("success", false, "message", "Ученик не найден: " + name);
                    if (found.size() > 1) yield Map.of("success", false, "message",
                        "Найдено несколько учеников: " + found.stream()
                            .map(s -> s.getUser().getSecondName() + " " + s.getUser().getFirstName())
                            .collect(Collectors.joining(", ")) + ". Уточните имя.");
                    Student s = found.get(0);
                    String fullName = s.getUser().getSecondName() + " " + s.getUser().getFirstName();
                    studentRepository.deleteById(s.getId());
                    yield Map.of("success", true, "message", "Ученик " + fullName + " удалён");
                }

                case "generate_schedule" -> {
                    String groupName = args.path("groupName").asText();
                    int lessonsPerDay = args.path("lessonsPerDay").asInt(5);
                    String startDateStr = args.path("startDate").asText("");

                    GroupEntity group = groupRepository.findAll().stream()
                        .filter(g -> g.getName().equalsIgnoreCase(groupName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("\u0413\u0440\u0443\u043f\u043f\u0430 '" + groupName + "' \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430"));

                    List<Teacher> teachers = teacherRepository.findAll();
                    List<Discipline> disciplines = disciplineRepository.findAll();
                    if (teachers.isEmpty()) yield Map.of("success", false, "message", "\u041d\u0435\u0442 \u0443\u0447\u0438\u0442\u0435\u043b\u0435\u0439");
                    if (disciplines.isEmpty()) yield Map.of("success", false, "message", "\u041d\u0435\u0442 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432");

                    java.time.LocalDate start;
                    try {
                        start = startDateStr.isBlank() ? java.time.LocalDate.now() : java.time.LocalDate.parse(startDateStr);
                    } catch (Exception e) {
                        start = java.time.LocalDate.now();
                    }
                    // \u041d\u0430\u0447\u0438\u043d\u0430\u0435\u043c \u0441 \u043f\u043e\u043d\u0435\u0434\u0435\u043b\u044c\u043d\u0438\u043a
                    while (start.getDayOfWeek().getValue() > 5) start = start.plusDays(1);

                    java.time.LocalTime[] startTimes = {
                        java.time.LocalTime.of(8, 0), java.time.LocalTime.of(8, 55),
                        java.time.LocalTime.of(9, 55), java.time.LocalTime.of(10, 50),
                        java.time.LocalTime.of(11, 50), java.time.LocalTime.of(12, 45)
                    };

                    int created = 0;
                    for (int day = 0; day < 5; day++) {
                        java.time.LocalDate date = start.plusDays(day);
                        for (int lesson = 0; lesson < Math.min(lessonsPerDay, startTimes.length); lesson++) {
                            Schedule s = new Schedule();
                            s.setGroup(group);
                            Discipline disc = disciplines.get((day * lessonsPerDay + lesson) % disciplines.size());
                            Teacher teacher = teachers.stream()
                                .filter(t -> t.getDisciplines().contains(disc))
                                .findFirst()
                                .orElse(teachers.get(lesson % teachers.size()));
                            s.setDiscipline(disc);
                            s.setTeacher(teacher);
                            s.setLessonNumber(lesson + 1);
                            s.setDate(date);
                            s.setStartTime(startTimes[lesson]);
                            s.setEndTime(startTimes[lesson].plusMinutes(45));
                            s.setClassroom(teacher.getOffice() != null ? teacher.getOffice() : String.valueOf(100 + random.nextInt(200)));
                            s.setDayOfWeek(date.getDayOfWeek().getValue());
                            scheduleRepository.save(s);
                            created++;
                        }
                    }
                    yield Map.of("success", true, "message",
                        "\u0420\u0430\u0441\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u0434\u043b\u044f " + groupName + " \u0441\u043e\u0437\u0434\u0430\u043d\u043e: " + created + " \u0443\u0440\u043e\u043a\u043e\u0432 (\u043f\u043d-\u043f\u0442 \u0441 " + start + ")");
                }

                case "get_schedule" -> {
                    String groupName = args.path("groupName").asText();
                    GroupEntity group = groupRepository.findAll().stream()
                        .filter(g -> g.getName().equalsIgnoreCase(groupName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("\u0413\u0440\u0443\u043f\u043f\u0430 '" + groupName + "' \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430"));
                    List<Schedule> schedules = scheduleRepository.findByGroupIdAndDateBetween(
                        group.getId(), java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(7));
                    if (schedules.isEmpty()) yield Map.of("success", true, "message", "\u0420\u0430\u0441\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u0434\u043b\u044f " + groupName + " \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e");
                    String msg = schedules.stream()
                        .map(s -> s.getDate() + " " + s.getStartTime() + " " + s.getDiscipline().getName()
                            + " (" + s.getTeacher().getUser().getSecondName() + ", \u043a\u0430\u0431. " + s.getClassroom() + ")")
                        .collect(Collectors.joining("\n"));
                    yield Map.of("success", true, "message", "\u0420\u0430\u0441\u043f\u0438\u0441\u0430\u043d\u0438\u0435 " + groupName + ":\n" + msg);
                }

                case "analyze_performance" -> {
                    String groupFilter = args.path("groupName").asText("");
                    List<Student> students = studentRepository.findAll();
                    if (!groupFilter.isBlank()) {
                        students = students.stream()
                            .filter(s -> s.getGroup() != null && s.getGroup().getName().equalsIgnoreCase(groupFilter))
                            .collect(Collectors.toList());
                    }
                    if (students.isEmpty()) yield Map.of("success", false, "message", "\u0423\u0447\u0435\u043d\u0438\u043a\u043e\u0432 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e");

                    Map<String, Double> avgByGroup = new LinkedHashMap<>();
                    Map<String, Double> avgByDisc = new LinkedHashMap<>();
                    List<String> top = new ArrayList<>();
                    List<String> low = new ArrayList<>();

                    for (Student st : students) {
                        List<Integer> marks = st.getMarks().stream()
                            .filter(m -> m.getDeletedAt() == null)
                            .map(Mark::getValue).collect(Collectors.toList());
                        if (marks.isEmpty()) continue;
                        double avg = marks.stream().mapToInt(Integer::intValue).average().orElse(0);
                        String fullName = st.getUser().getSecondName() + " " + st.getUser().getFirstName();
                        String grpName = st.getGroup() != null ? st.getGroup().getName() : "?";
                        avgByGroup.merge(grpName, avg, (a, b) -> (a + b) / 2);
                        if (avg >= 4.5) top.add(fullName + "(" + String.format("%.1f", avg) + ")");
                        if (avg < 3.5) low.add(fullName + "(" + String.format("%.1f", avg) + ")");

                        st.getMarks().stream().filter(m -> m.getDeletedAt() == null).forEach(m -> {
                            String disc = m.getDiscipline().getName();
                            avgByDisc.merge(disc, (double) m.getValue(), (a, b) -> (a + b) / 2);
                        });
                    }

                    StringBuilder sb = new StringBuilder("\u0410\u043d\u0430\u043b\u0438\u0437 \u0443\u0441\u043f\u0435\u0432\u0430\u0435\u043c\u043e\u0441\u0442\u0438:\n");
                    if (!avgByGroup.isEmpty()) {
                        sb.append("\u0421\u0440\u0435\u0434\u043d\u044f\u044f \u043e\u0446\u0435\u043d\u043a\u0430 \u043f\u043e \u043a\u043b\u0430\u0441\u0441\u0430\u043c: ");
                        avgByGroup.forEach((g, a) -> sb.append(g).append(":").append(String.format("%.1f", a)).append(" "));
                        sb.append("\n");
                    }
                    if (!avgByDisc.isEmpty()) {
                        sb.append("\u041f\u043e \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430\u043c: ");
                        avgByDisc.entrySet().stream()
                            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                            .limit(5)
                            .forEach(e -> sb.append(e.getKey()).append(":").append(String.format("%.1f", e.getValue())).append(" "));
                        sb.append("\n");
                    }
                    if (!top.isEmpty()) sb.append("\u041e\u0442\u043b\u0438\u0447\u043d\u0438\u043a\u0438: ").append(String.join(", ", top.subList(0, Math.min(5, top.size())))).append("\n");
                    if (!low.isEmpty()) sb.append("\u0422\u0440\u043e\u0439\u043d\u0438\u043a\u0438: ").append(String.join(", ", low.subList(0, Math.min(5, low.size())))).append("\n");

                    yield Map.of("success", true, "message", sb.toString());
                }

                case "add_course" -> {
                    String teacherName = args.path("teacherName").asText("").toLowerCase();
                    Teacher teacher = teacherRepository.findAll().stream()
                        .filter(t -> (t.getUser().getSecondName() + " " + t.getUser().getFirstName()).toLowerCase().contains(teacherName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("\u0423\u0447\u0438\u0442\u0435\u043b\u044c '" + teacherName + "' \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d"));
                    Course course = new Course();
                    course.setName(args.path("name").asText());
                    course.setDescription(args.path("description").asText(""));
                    course.setTeacher(teacher);
                    courseRepository.save(course);
                    yield Map.of("success", true, "message",
                        "\u041a\u0443\u0440\u0441 '" + course.getName() + "' \u0441\u043e\u0437\u0434\u0430\u043d, \u0443\u0447\u0438\u0442\u0435\u043b\u044c: " + teacher.getUser().getSecondName());
                }

                case "get_courses" -> {
                    List<Course> courses = courseRepository.findAll();
                    if (courses.isEmpty()) yield Map.of("success", true, "message", "\u041a\u0443\u0440\u0441\u043e\u0432 \u043d\u0435\u0442");
                    String msg = courses.stream()
                        .map(c -> c.getName() + (c.getTeacher() != null ? " (" + c.getTeacher().getUser().getSecondName() + ")" : ""))
                        .collect(Collectors.joining(", "));
                    yield Map.of("success", true, "message", "\u041a\u0443\u0440\u0441\u044b: " + msg);
                }

                default -> Map.of("success", false, "message", "\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u043e\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435: " + functionName);
            };
        } catch (Exception e) {
            return Map.of("success", false, "message", "Ошибка: " + e.getMessage());
        }
    }
}
