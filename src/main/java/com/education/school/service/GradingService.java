package com.education.school.service;

import com.education.school.entity.*;
import com.education.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingService {

    private final GradingSettingsRepository settingsRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final MarkRepository markRepository;
    private final CurrentSchoolService currentSchoolService;

    public static final String FIVE = "FIVE";
    public static final String HUNDRED = "HUNDRED";

    /** Шкала оценок текущей школы; без настроек — 100-балльная */
    @Transactional(readOnly = true)
    public String currentScale() {
        return settingsRepository.findBySchoolName(currentSchoolService.get().getName())
                .map(GradingSettings::getMarkScale).orElse(HUNDRED);
    }

    public static int maxMark(String scale) {
        return FIVE.equals(scale) ? 5 : 100;
    }

    /** Проверка оценки по шкале школы — и для новой, и для изменённой оценки */
    public void validateMark(Integer value) {
        int max = maxMark(currentScale());
        if (value == null || value < 1 || value > max) {
            throw new IllegalArgumentException("Введите оценку от 1 до " + max);
        }
    }

    // ── Настройки ──

    // Обычный школьный календарь («месяц-день»): лето и каникулы в периоды не входят
    public static final Map<String, List<String[]>> DEFAULT_PERIODS = Map.of(
            "QUARTER", List.of(new String[]{"09-01", "10-31"}, new String[]{"11-01", "12-31"},
                               new String[]{"01-01", "03-31"}, new String[]{"04-01", "05-31"}),
            "SEMESTER", List.of(new String[]{"09-01", "12-31"}, new String[]{"01-01", "05-31"}),
            "YEAR", List.<String[]>of(new String[]{"09-01", "05-31"}));
    private static final Map<String, String[]> PERIOD_NAMES = Map.of(
            "QUARTER", new String[]{"1 четверть", "2 четверть", "3 четверть", "4 четверть"},
            "SEMESTER", new String[]{"1 полугодие", "2 полугодие"},
            "YEAR", new String[]{"Учебный год"});
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    /** Шаблон периодов школы для типа: заданный директором или школьный календарь по умолчанию */
    private List<String[]> template(GradingSettings s, String type) {
        if (s != null && s.getCustomPeriods() != null) {
            try {
                var node = JSON.readTree(s.getCustomPeriods()).path(type);
                if (node.isArray() && node.size() == DEFAULT_PERIODS.get(type).size()) {
                    List<String[]> result = new ArrayList<>();
                    node.forEach(p -> result.add(new String[]{p.path(0).asText(), p.path(1).asText()}));
                    return result;
                }
            } catch (Exception ignored) { /* повреждённая запись — работаем по умолчанию */ }
        }
        return DEFAULT_PERIODS.get(type);
    }

    private static int[] md(String value) {
        if (value == null || !value.matches("\\d{2}-\\d{2}")) throw new IllegalArgumentException("Дата периода должна быть в формате ММ-ДД");
        int m = Integer.parseInt(value.substring(0, 2)), d = Integer.parseInt(value.substring(3));
        if (m < 1 || m > 12 || d < 1 || d > java.time.Month.of(m).maxLength()) throw new IllegalArgumentException("Неверная дата периода: " + value);
        return new int[]{m, d};
    }

    /** Порядковый номер дня внутри учебного года, который начинается в first («месяц-день») */
    private static int ordinal(String value, String first) {
        int[] v = md(value), f = md(first);
        boolean nextYear = v[0] * 100 + v[1] < f[0] * 100 + f[1];
        return (nextYear ? 10000 : 0) + v[0] * 100 + v[1];
    }

    /** Конкретная дата «месяц-день» в учебном году, начавшемся в году year */
    private static LocalDate dateIn(String value, String first, int year) {
        int[] v = md(value);
        int y = ordinal(value, first) >= 10000 ? year + 1 : year;
        return LocalDate.of(y, v[0], Math.min(v[1], java.time.YearMonth.of(y, v[0]).lengthOfMonth()));
    }

    /** Проверка периодов: нужное количество, каждый не пустой, идут друг за другом и умещаются в год */
    private static void validateTemplate(String type, List<String[]> periods) {
        int expected = DEFAULT_PERIODS.get(type).size();
        if (periods.size() != expected) throw new IllegalArgumentException("Нужно периодов: " + expected);
        String first = periods.get(0)[0];
        int previousEnd = -1;
        for (int i = 0; i < periods.size(); i++) {
            int from = ordinal(periods.get(i)[0], first), to = ordinal(periods.get(i)[1], first);
            String name = PERIOD_NAMES.get(type)[i];
            if (to < from) throw new IllegalArgumentException(name + ": конец раньше начала");
            if (from <= previousEnd) throw new IllegalArgumentException(name + " начинается раньше, чем закончился предыдущий период");
            previousEnd = to;
        }
    }

    private Map<String, Object> templateView(GradingSettings s) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String type : List.of("QUARTER", "SEMESTER", "YEAR")) {
            result.put(type, template(s, type).stream().map(p -> List.of(p[0], p[1])).collect(Collectors.toList()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSettings(String schoolName) {
        GradingSettings s = settingsRepository.findBySchoolName(schoolName).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodType", s != null ? s.getPeriodType() : "QUARTER");
        result.put("markScale", s != null ? s.getMarkScale() : HUNDRED);
        result.put("periods", templateView(s));
        result.put("defaultPeriods", templateView(null));
        return result;
    }

    /**
     * Сохранение настроек. periods — даты периодов выбранного типа в формате «ММ-ДД»;
     * null — оставить как есть, пустой список — вернуть школьный календарь по умолчанию.
     */
    @Transactional
    public Map<String, Object> saveSettings(String schoolName, String periodType, String markScale, List<String[]> periods) {
        if (!java.util.Set.of("QUARTER", "SEMESTER", "YEAR").contains(String.valueOf(periodType))) {
            throw new IllegalArgumentException("Тип периода должен быть QUARTER, SEMESTER или YEAR");
        }
        if (markScale != null && !java.util.Set.of(FIVE, HUNDRED).contains(markScale)) {
            throw new IllegalArgumentException("Шкала оценок должна быть FIVE или HUNDRED");
        }
        School school = schoolRepository.findByName(schoolName)
                .orElseThrow(() -> new IllegalArgumentException("School not found"));
        GradingSettings s = settingsRepository.findBySchoolName(schoolName).orElse(new GradingSettings());
        // 5-балльная шкала не может включиться, пока в журнале есть оценки выше 5 — иначе средние потеряют смысл
        if (FIVE.equals(markScale) && !FIVE.equals(s.getMarkScale())) {
            long above = markRepository.countByValueGreaterThanAndDeletedAtIsNull(5);
            if (above > 0) {
                throw new IllegalArgumentException("В журнале уже есть оценки выше 5 (" + above
                        + "). Перейти на 5-балльную шкалу можно, когда их исправят или удалят.");
            }
        }
        if (periods != null) {
            try {
                var root = s.getCustomPeriods() != null
                        ? (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(s.getCustomPeriods())
                        : JSON.createObjectNode();
                if (periods.isEmpty()) {
                    root.remove(periodType);
                } else {
                    validateTemplate(periodType, periods);
                    var array = root.putArray(periodType);
                    periods.forEach(p -> array.addArray().add(p[0]).add(p[1]));
                }
                s.setCustomPeriods(root.isEmpty() ? null : JSON.writeValueAsString(root));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                s.setCustomPeriods(null);
            }
        }
        s.setSchool(school);
        s.setPeriodType(periodType);
        // Начало учебного года — месяц начала первого периода (поле оставлено для совместимости)
        s.setAcademicYearStart(md(template(s, periodType).get(0)[0])[0]);
        if (markScale != null) s.setMarkScale(markScale);
        s.setUpdatedAt(LocalDateTime.now());
        settingsRepository.save(s);
        return getSettings(schoolName);
    }

    // ── Расчёт периодов за несколько лет ──

    /** Периоды школы с конкретными датами — с учебного года первой оценки до текущего; будущие не включаются */
    public List<Map<String, Object>> buildPeriods(GradingSettings s, String periodType, LocalDate firstMark) {
        List<String[]> template = template(s, periodType);
        String first = template.get(0)[0];
        LocalDate today = LocalDate.now();
        int startYear = academicYearOf(firstMark != null ? firstMark : today, first);
        int currentYear = academicYearOf(today, first);

        List<Map<String, Object>> all = new ArrayList<>();
        for (int y = startYear; y <= currentYear; y++) {
            String yearLabel = y + "/" + (y + 1);
            for (int i = 0; i < template.size(); i++) {
                all.add(period(PERIOD_NAMES.get(periodType)[i] + " " + yearLabel,
                        dateIn(template.get(i)[0], first, y), dateIn(template.get(i)[1], first, y)));
            }
        }
        return all.stream()
                .filter(p -> !LocalDate.parse((String) p.get("from")).isAfter(today))
                .collect(Collectors.toList());
    }

    /** Учебный год даты: до начала первого периода — ещё прошлый год (июль 2024 при старте 09-01 → 2023) */
    private static int academicYearOf(LocalDate date, String first) {
        int[] f = md(first);
        return date.getMonthValue() * 100 + date.getDayOfMonth() >= f[0] * 100 + f[1] ? date.getYear() : date.getYear() - 1;
    }

    /** Периоды текущей школы с датами — для страниц родителя, куратора и журнала учителя */
    @Transactional(readOnly = true)
    public Map<String, Object> currentPeriods() {
        GradingSettings s = settingsRepository.findBySchoolName(currentSchoolService.get().getName()).orElse(null);
        String type = s != null ? s.getPeriodType() : "QUARTER";
        var firstMark = markRepository.findFirstMarkDate();
        List<Map<String, Object>> periods = buildPeriods(s, type, firstMark != null ? firstMark.toLocalDate() : null);
        Collections.reverse(periods); // новые сверху
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodType", type);
        result.put("periods", periods);
        return result;
    }

    private Map<String, Object> period(String name, LocalDate from, LocalDate to) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("from", from.toString());
        m.put("to", to.toString());
        return m;
    }

    // ── Оценки студента по периодам ──

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentGrades(String schoolName) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Student not found"));

        GradingSettings settings = settingsRepository.findBySchoolName(schoolName).orElse(null);
        String periodType = settings != null ? settings.getPeriodType() : "QUARTER";

        // Все оценки студента
        List<Mark> allMarks = markRepository.findAll().stream()
                .filter(m -> m.getStudent().getId().equals(student.getId()))
                .filter(m -> m.getDeletedAt() == null)
                .collect(Collectors.toList());

        // Дата первой оценки
        LocalDate firstMark = allMarks.stream()
                .map(m -> m.getCreatedAt().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(null);

        List<Map<String, Object>> periods = buildPeriods(settings, periodType, firstMark);

        List<Map<String, Object>> periodsWithGrades = periods.stream().map(p -> {
            LocalDate from = LocalDate.parse((String) p.get("from"));
            LocalDate to   = LocalDate.parse((String) p.get("to"));

            List<Mark> periodMarks = allMarks.stream()
                    .filter(m -> {
                        LocalDate d = m.getCreatedAt().toLocalDate();
                        return !d.isBefore(from) && !d.isAfter(to);
                    })
                    .collect(Collectors.toList());

            // Группируем по предмету
            Map<String, List<Mark>> byDisc = periodMarks.stream()
                    .collect(Collectors.groupingBy(m -> m.getDiscipline().getName()));

            List<Map<String, Object>> disciplines = byDisc.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> {
                        List<Integer> vals = e.getValue().stream()
                                .sorted(Comparator.comparing(m -> m.getCreatedAt()))
                                .map(Mark::getValue).collect(Collectors.toList());
                        double avg = vals.stream().mapToInt(Integer::intValue).average().orElse(0);
                        Map<String, Object> d = new LinkedHashMap<>();
                        d.put("name", e.getKey());
                        d.put("avg", Math.round(avg * 10.0) / 10.0);
                        d.put("marks", vals);
                        d.put("count", vals.size());
                        return d;
                    })
                    .collect(Collectors.toList());

            double overallAvg = disciplines.isEmpty() ? 0 :
                    disciplines.stream().mapToDouble(d -> (Double) d.get("avg")).average().orElse(0);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", p.get("name"));
            result.put("from", p.get("from"));
            result.put("to", p.get("to"));
            result.put("disciplines", disciplines);
            result.put("overallAvg", Math.round(overallAvg * 10.0) / 10.0);
            result.put("totalMarks", periodMarks.size());
            return result;
        }).collect(Collectors.toList());

        // Переворачиваем — новые периоды сверху
        Collections.reverse(periodsWithGrades);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("periodType", periodType);
        response.put("markScale", settings != null ? settings.getMarkScale() : HUNDRED);
        response.put("periods", periodsWithGrades);
        return response;
    }
}
