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

    @Transactional(readOnly = true)
    public Map<String, Object> getSettings(String schoolName) {
        GradingSettings s = settingsRepository.findBySchoolName(schoolName).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodType", s != null ? s.getPeriodType() : "QUARTER");
        result.put("academicYearStart", s != null ? s.getAcademicYearStart() : 9);
        result.put("markScale", s != null ? s.getMarkScale() : HUNDRED);
        return result;
    }

    @Transactional
    public Map<String, Object> saveSettings(String schoolName, String periodType, Integer yearStart, String markScale) {
        if (!java.util.Set.of("QUARTER", "SEMESTER", "YEAR").contains(String.valueOf(periodType))) {
            throw new IllegalArgumentException("Тип периода должен быть QUARTER, SEMESTER или YEAR");
        }
        if (yearStart != null && (yearStart < 1 || yearStart > 12)) {
            throw new IllegalArgumentException("Месяц начала учебного года должен быть от 1 до 12");
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
        s.setSchool(school);
        s.setPeriodType(periodType);
        s.setAcademicYearStart(yearStart != null ? yearStart : 9);
        if (markScale != null) s.setMarkScale(markScale);
        s.setUpdatedAt(LocalDateTime.now());
        settingsRepository.save(s);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodType", s.getPeriodType());
        result.put("academicYearStart", s.getAcademicYearStart());
        result.put("markScale", s.getMarkScale());
        return result;
    }

    // ── Расчёт периодов за несколько лет ──

    /**
     * Возвращает все периоды начиная с firstMarkDate до сегодня.
     * Если оценок нет — возвращает текущий учебный год.
     */
    public List<Map<String, Object>> buildPeriods(String periodType, int yearStart, LocalDate firstMark) {
        LocalDate today = LocalDate.now();

        // Определяем первый учебный год (где есть оценки)
        int startYear = academicYearOf(firstMark != null ? firstMark : today, yearStart);
        int currentYear = academicYearOf(today, yearStart);

        List<Map<String, Object>> all = new ArrayList<>();

        for (int y = startYear; y <= currentYear; y++) {
            all.addAll(periodsForYear(periodType, yearStart, y));
        }

        // Убираем будущие периоды (начало которых ещё не наступило)
        return all.stream()
                .filter(p -> !LocalDate.parse((String) p.get("from")).isAfter(today))
                .collect(Collectors.toList());
    }

    /** Учебный год, в котором находится дата (напр. сент 2023 → 2023, янв 2024 → 2023) */
    private int academicYearOf(LocalDate date, int yearStart) {
        return date.getMonthValue() >= yearStart ? date.getYear() : date.getYear() - 1;
    }

    /** Все периоды одного учебного года */
    private List<Map<String, Object>> periodsForYear(String periodType, int yearStart, int year) {
        List<Map<String, Object>> periods = new ArrayList<>();
        String yearLabel = year + "/" + (year + 1);

        if ("YEAR".equals(periodType)) {
            LocalDate from = LocalDate.of(year, yearStart, 1);
            LocalDate to = LocalDate.of(year + 1, yearStart, 1).minusDays(1);
            periods.add(period("Учебный год " + yearLabel, from, to));

        } else if ("SEMESTER".equals(periodType)) {
            // S1: yearStart .. yearStart+3 (4 месяца), S2: остаток
            LocalDate s1from = LocalDate.of(year, yearStart, 1);
            LocalDate s1to   = s1from.plusMonths(4).minusDays(1);
            LocalDate s2from = s1to.plusDays(1);
            LocalDate s2to   = LocalDate.of(year + 1, yearStart, 1).minusDays(1);
            periods.add(period("1 полугодие " + yearLabel, s1from, s1to));
            periods.add(period("2 полугодие " + yearLabel, s2from, s2to));

        } else { // QUARTER — 4 четверти по ~2 месяца
            LocalDate yearFrom = LocalDate.of(year, yearStart, 1);
            LocalDate yearTo   = LocalDate.of(year + 1, yearStart, 1).minusDays(1);
            long totalDays = yearTo.toEpochDay() - yearFrom.toEpochDay();
            long qLen = totalDays / 4;
            String[] names = {"1 четверть", "2 четверть", "3 четверть", "4 четверть"};
            for (int i = 0; i < 4; i++) {
                LocalDate from = yearFrom.plusDays(i * qLen);
                LocalDate to   = (i == 3) ? yearTo : yearFrom.plusDays((i + 1) * qLen).minusDays(1);
                periods.add(period(names[i] + " " + yearLabel, from, to));
            }
        }
        return periods;
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
        int yearStart = settings != null && settings.getAcademicYearStart() != null && settings.getAcademicYearStart() >= 1 && settings.getAcademicYearStart() <= 12 ? settings.getAcademicYearStart() : 9 /* сохранённое ранее некорректное значение не ломает страницу */;

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

        List<Map<String, Object>> periods = buildPeriods(periodType, yearStart, firstMark);

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
