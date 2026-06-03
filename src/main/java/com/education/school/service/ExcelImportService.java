package com.education.school.service;

import com.education.school.dto.StudentRequest;
import com.education.school.entity.GroupEntity;
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

    private final StudentService studentService;
    private final GroupRepository groupRepository;

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
            if (lower.contains("имя") || lower.contains("name") || lower.equals("first")) {
                mapping.put(h, "firstName");
            } else if (lower.contains("фамил") || lower.contains("surname") || lower.contains("last")) {
                mapping.put(h, "secondName");
            } else if (lower.contains("отчест") || lower.contains("middle") || lower.contains("patron")) {
                mapping.put(h, "thirdName");
            } else if (lower.contains("email") || lower.contains("почта") || lower.contains("mail")) {
                mapping.put(h, "email");
            } else if (lower.contains("пароль") || lower.contains("password") || lower.contains("pass")) {
                mapping.put(h, "password");
            } else if (lower.contains("возраст") || lower.contains("age") || lower.contains("лет")) {
                mapping.put(h, "age");
            } else if (lower.contains("класс") || lower.contains("группа") || lower.contains("group")) {
                mapping.put(h, "groupName");
            } else {
                mapping.put(h, "ignore");
            }
        }
        return mapping;
    }

    /** Импортирует студентов по маппингу */
    @SuppressWarnings("unchecked")
    public Map<String, Object> importStudents(Map<String, Object> body) throws Exception {
        String base64 = (String) body.get("fileData");
        Map<String, String> mapping = (Map<String, String>) body.get("mapping");
        String defaultGroup = (String) body.get("defaultGroup");
        String defaultPassword = (String) body.getOrDefault("defaultPassword", "password123");

        byte[] bytes = Base64.getDecoder().decode(base64);
        Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes));
        Sheet sheet = workbook.getSheetAt(0);

        List<String> headers = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        int rowNum = 0;
        for (Row row : sheet) {
            if (rowNum == 0) {
                for (Cell cell : row) headers.add(getCellValue(cell));
                rowNum++;
                continue;
            }

            Map<String, String> rowData = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String field = mapping.getOrDefault(headers.get(i), "ignore");
                if (!"ignore".equals(field)) {
                    Cell cell = row.getCell(i);
                    rowData.put(field, cell != null ? getCellValue(cell) : "");
                }
            }

            try {
                String groupName = rowData.getOrDefault("groupName", defaultGroup);
                if (groupName == null || groupName.isBlank()) {
                    errors.add("Строка " + rowNum + ": не указана группа");
                    rowNum++;
                    continue;
                }

                GroupEntity group = groupRepository.findAll().stream()
                    .filter(g -> g.getName().equalsIgnoreCase(groupName.trim()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Группа '" + groupName + "' не найдена"));

                String password = rowData.getOrDefault("password", defaultPassword);
                if (password.isBlank()) password = defaultPassword;

                StudentRequest req = new StudentRequest(
                    rowData.getOrDefault("firstName", ""),
                    rowData.getOrDefault("secondName", ""),
                    rowData.getOrDefault("thirdName", ""),
                    rowData.getOrDefault("email", ""),
                    password,
                    parseAge(rowData.getOrDefault("age", "18")),
                    group.getId()
                );

                if (req.firstName().isBlank() || req.email().isBlank()) {
                    errors.add("Строка " + rowNum + ": пустое имя или email");
                    rowNum++;
                    continue;
                }

                studentService.create(req);
                added.add(req.secondName() + " " + req.firstName());
            } catch (Exception e) {
                errors.add("Строка " + rowNum + ": " + e.getMessage());
            }
            rowNum++;
        }
        workbook.close();

        return Map.of(
            "added", added.size(),
            "errors", errors.size(),
            "addedNames", added,
            "errorDetails", errors
        );
    }

    private int parseAge(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 18; }
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
