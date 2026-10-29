package com.education.school.service;

import com.education.school.dto.TeacherRequest;
import com.education.school.entity.Discipline;
import com.education.school.entity.GroupEntity;
import com.education.school.repository.DisciplineRepository;
import com.education.school.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final GroupRepository groupRepository;
    private final DisciplineRepository disciplineRepository;
    private final TeacherService teacherService;
    private final StudentEnrollmentService enrollmentService;
    private final CredentialMailService credentialMailService;

    /** Читает Excel и возвращает превью: заголовки + первые 5 строк */
    public Map<String, Object> preview(MultipartFile file) throws Exception {
        Workbook workbook = openWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);

        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        int rowCount = 0;
        for (Row row : sheet) {
            if (rowCount == 0) {
                for (Cell cell : row) headers.add(getCellValue(cell));
            } else if (rowCount <= 5) {
                List<String> rowData = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i);
                    rowData.add(cell != null ? getCellValue(cell) : "");
                }
                rows.add(rowData);
            }
            rowCount++;
        }
        workbook.close();

        // AI-подсказка маппинга колонок
        Map<String, String> suggestedMapping = suggestMapping(headers);

        return Map.of(
            "headers", headers,
            "preview", rows,
            "totalRows", rowCount - 1,
            "suggestedMapping", suggestedMapping
        );
    }

    /** Угадывает маппинг колонок по названию */
    private Map<String, String> suggestMapping(List<String> headers) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String h : headers) {
            String lower = h.toLowerCase().trim();
            boolean mail = lower.contains("почта") || lower.contains("email") || lower.contains("mail");
            // Порядок важен: колонки родителя, "фио" и "surname" проверяем раньше, чем "имя"/"name"
            if (lower.contains("родител") || lower.contains("parent")) {
                mapping.put(h, mail ? "parentEmail" : "parentFullName");
            } else if (lower.equals("фио") || lower.contains("ф.и.о") || lower.contains("full name") || lower.contains("fullname")) {
                mapping.put(h, "fullName");
            } else if (lower.contains("фамил") || lower.contains("surname") || lower.contains("last")) {
                mapping.put(h, "secondName");
            } else if (lower.contains("отчест") || lower.contains("middle") || lower.contains("patron")) {
                mapping.put(h, "thirdName");
            } else if (mail) {
                mapping.put(h, "email");
            } else if (lower.contains("имя") || lower.contains("name") || lower.equals("first")) {
                mapping.put(h, "firstName");
            } else if (lower.contains("возраст") || lower.contains("age") || lower.contains("лет")) {
                mapping.put(h, "age");
            } else if (lower.contains("класс") || lower.contains("группа") || lower.contains("group")) {
                mapping.put(h, "groupName");
            } else if (lower.contains("предмет") || lower.contains("subject") || lower.contains("дисциплин")) {
                mapping.put(h, "disciplines");
            } else if (lower.contains("телефон") || lower.contains("phone")) {
                mapping.put(h, "phone");
            } else {
                mapping.put(h, "ignore");
            }
        }
        return mapping;
    }

    /** Импорт по маппингу: type = "students" (по умолчанию) или "teachers" */
    @SuppressWarnings("unchecked")
    public Map<String, Object> importStudents(Map<String, Object> body) throws Exception {
        // Каждый получает свой временный пароль на почту — без почты импорт бессмыслен
        credentialMailService.requireConfigured();

        boolean teachers = "teachers".equals(body.get("type"));
        Map<String, String> mapping = (Map<String, String>) body.get("mapping");
        String defaultGroup = (String) body.get("defaultGroup");

        byte[] bytes = Base64.getDecoder().decode((String) body.get("fileData"));
        List<String> added = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = new ArrayList<>();
            int rowNum = 0;
            for (Row row : sheet) {
                if (rowNum++ == 0) {
                    for (Cell cell : row) headers.add(getCellValue(cell));
                    continue;
                }
                Map<String, String> rowData = readRow(row, headers, mapping);
                if (rowData.values().stream().allMatch(String::isBlank)) continue; // пустая строка

                try {
                    added.add(teachers ? importTeacher(rowData) : importStudent(rowData, defaultGroup));
                } catch (Exception e) {
                    errors.add("Строка " + rowNum + ": " + e.getMessage());
                }
            }
        }

        return Map.of(
            "added", added.size(),
            "errors", errors.size(),
            "addedNames", added,
            "errorDetails", errors
        );
    }

    private String importStudent(Map<String, String> rowData, String defaultGroup) {
        String groupName = rowData.getOrDefault("groupName", "").isBlank() ? defaultGroup : rowData.get("groupName");
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("не указан класс");
        }
        GroupEntity group = groupRepository.findAll().stream()
            .filter(g -> g.getName().equalsIgnoreCase(groupName.trim()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("класс '" + groupName + "' не найден"));

        splitFullName(rowData, "fullName", "secondName", "firstName", "thirdName");
        splitFullName(rowData, "parentFullName", "parentSecondName", "parentFirstName", "parentThirdName");

        // Пароль не задаём: он генерируется и уходит на почту ученика (или родителя)
        return enrollmentService.enroll(new StudentEnrollmentService.Enrollment(
            rowData.get("secondName"), rowData.get("firstName"), rowData.get("thirdName"),
            rowData.get("email"), rowData.get("parentEmail"),
            rowData.get("parentSecondName"), rowData.get("parentFirstName"),
            parseAge(rowData.get("age")), group));
    }

    private String importTeacher(Map<String, String> rowData) {
        splitFullName(rowData, "fullName", "secondName", "firstName", "thirdName");
        String email = rowData.getOrDefault("email", "").trim();
        if (rowData.getOrDefault("firstName", "").isBlank() || rowData.getOrDefault("secondName", "").isBlank() || email.isBlank()) {
            throw new IllegalArgumentException("нет ФИО или почты учителя");
        }
        // Предметы через запятую: "Математика, Физика"; новые предметы школы создаются автоматически
        List<Integer> disciplineIds = new ArrayList<>();
        for (String name : rowData.getOrDefault("disciplines", "").split("[,;]")) {
            if (!name.isBlank()) disciplineIds.add(findOrCreateDiscipline(name.trim()).getId());
        }
        TeacherRequest req = new TeacherRequest(
            rowData.get("firstName").trim(), rowData.get("secondName").trim(), rowData.get("thirdName"),
            email, null, rowData.get("phone"), null, false, null, false, null, disciplineIds);
        teacherService.create(req); // пароль генерируется и уходит на почту учителя
        return req.secondName() + " " + req.firstName();
    }

    private Discipline findOrCreateDiscipline(String name) {
        return disciplineRepository.findAll().stream()
            .filter(d -> d.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> {
                Discipline d = new Discipline();
                d.setName(name);
                return disciplineRepository.save(d);
            });
    }

    private Map<String, String> readRow(Row row, List<String> headers, Map<String, String> mapping) {
        Map<String, String> rowData = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String field = mapping.getOrDefault(headers.get(i), "ignore");
            if (!"ignore".equals(field)) {
                Cell cell = row.getCell(i);
                rowData.put(field, cell != null ? getCellValue(cell) : "");
            }
        }
        return rowData;
    }

    // "ФИО" одной колонкой: Фамилия Имя [Отчество] — раскладываем, если отдельных колонок нет
    private void splitFullName(Map<String, String> rowData, String fullKey, String lastKey, String firstKey, String middleKey) {
        String full = rowData.get(fullKey);
        if (full == null || full.isBlank()) return;
        String[] parts = full.trim().split("\\s+", 3);
        if (parts.length >= 1) rowData.putIfAbsent(lastKey, parts[0]);
        if (parts.length >= 2) rowData.putIfAbsent(firstKey, parts[1]);
        if (parts.length == 3) rowData.putIfAbsent(middleKey, parts[2]);
    }

    // null — возраст определится по номеру класса
    private Integer parseAge(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Workbook openWorkbook(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        InputStream is = file.getInputStream();
        if (name.endsWith(".xlsx")) return new XSSFWorkbook(is);
        if (name.endsWith(".xls"))  return new HSSFWorkbook(is);
        // Попробуем xlsx по умолчанию
        return new XSSFWorkbook(is);
    }
}
