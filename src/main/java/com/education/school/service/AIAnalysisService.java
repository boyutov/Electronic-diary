package com.education.school.service;

import com.education.school.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AIAnalysisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final DisciplineRepository disciplineRepository;
    private final MarkRepository markRepository;
    private final AttendanceRepository attendanceRepository;
    private final ComplaintRepository complaintRepository;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    public String analyzeSchoolData(String query, String schoolName) {
        if (openaiApiKey.isEmpty()) {
            return "AI анализ недоступен. Настройте OPENAI_API_KEY в переменных окружения.";
        }

        String dbContext = buildDatabaseContext(schoolName);
        
        String prompt = String.format("""
            Ты - AI помощник для анализа школьных данных.
            
            Контекст базы данных школы "%s":
            %s
            
            Вопрос пользователя: %s
            
            Проанализируй данные и дай подробный ответ с конкретными цифрами и рекомендациями.
            Если нужны дополнительные данные - укажи это.
            """, schoolName, dbContext, query);

        return callOpenAI(prompt);
    }

    private String buildDatabaseContext(String schoolName) {
        StringBuilder context = new StringBuilder();
        
        try {
            long studentCount = studentRepository.count();
            context.append("Учеников: ").append(studentCount).append("\n");
            
            long teacherCount = teacherRepository.count();
            context.append("Учителей: ").append(teacherCount).append("\n");
            
            var groups = groupRepository.findAll();
            context.append("Классов: ").append(groups.size()).append("\n");
            groups.forEach(g -> context.append("- ").append(g.getName()).append(": ").append(g.getStudents().size()).append(" учеников\n"));
            
            var disciplines = disciplineRepository.findAll();
            context.append("Предметов: ").append(disciplines.size()).append("\n");
            disciplines.forEach(d -> context.append("- ").append(d.getName()).append("\n"));
            
            var recentMarks = markRepository.findTop100ByOrderByCreatedAtDesc();
            if (!recentMarks.isEmpty()) {
                double avgMark = recentMarks.stream().mapToInt(m -> m.getValue()).average().orElse(0);
                context.append("Средняя оценка (последние 100): ").append(String.format("%.2f", avgMark)).append("\n");
            }
            
            long attendanceCount = attendanceRepository.count();
            context.append("Записей посещаемости: ").append(attendanceCount).append("\n");
            
            long complaintCount = complaintRepository.count();
            context.append("Жалоб: ").append(complaintCount).append("\n");
            
        } catch (Exception e) {
            context.append("Ошибка получения данных: ").append(e.getMessage()).append("\n");
        }
        
        return context.toString();
    }

    public String analyzeFile(byte[] fileData, String fileName, String query) {
        if (openaiApiKey.isEmpty()) {
            return "AI анализ недоступен. Настройте OPENAI_API_KEY в переменных окружения.";
        }

        String fileContent = extractFileContent(fileData, fileName);
        
        String prompt = String.format("""
            Проанализируй содержимое файла "%s" и ответь на вопрос.
            
            Содержимое файла:
            %s
            
            Вопрос: %s
            
            Дай подробный анализ с выводами и рекомендациями.
            """, fileName, fileContent, query);

        return callOpenAI(prompt);
    }

    private String callOpenAI(String prompt) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 2000
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            
            return "Ошибка получения ответа от AI";
            
        } catch (Exception e) {
            return "Ошибка вызова AI: " + e.getMessage();
        }
    }

    private String extractFileContent(byte[] fileData, String fileName) {
        try {
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            
            return switch (extension) {
                case "pdf" -> extractPdfText(fileData);
                case "jpg", "jpeg", "png", "bmp" -> extractImageText(fileData);
                case "xlsx", "xls" -> extractExcelText(fileData);
                case "txt" -> new String(fileData);
                default -> "Неподдерживаемый формат файла: " + extension;
            };
        } catch (Exception e) {
            return "Ошибка обработки файла: " + e.getMessage();
        }
    }

    private String extractPdfText(byte[] pdfData) {
        try {
            org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfData);
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text;
        } catch (Exception e) {
            return "Ошибка чтения PDF: " + e.getMessage();
        }
    }

    private String extractImageText(byte[] imageData) {
        try {
            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
            tesseract.setDatapath("tessdata");
            tesseract.setLanguage("rus+eng");
            
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(imageData));
            return tesseract.doOCR(image);
        } catch (Exception e) {
            return "Ошибка OCR: " + e.getMessage() + ". Убедитесь, что Tesseract установлен.";
        }
    }

    private String extractExcelText(byte[] excelData) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(excelData));
            StringBuilder text = new StringBuilder();
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(i);
                text.append("Лист: ").append(sheet.getSheetName()).append("\n");
                
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        text.append(getCellValue(cell)).append("\t");
                    }
                    text.append("\n");
                }
            }
            workbook.close();
            return text.toString();
        } catch (Exception e) {
            return "Ошибка чтения Excel: " + e.getMessage();
        }
    }

    public String executeAction(String action, String schoolName) {
        if (openaiApiKey.isEmpty()) {
            return "AI недоступен. Настройте OPENAI_API_KEY в переменных окружения.";
        }

        String dbContext = buildDatabaseContext(schoolName);
        
        String prompt = String.format("""
            Ты - AI помощник для управления школьными данными.
            
            Текущее состояние базы данных школы "%s":
            %s
            
            Задача: %s
            
            Если это запрос на создание данных, ответь ТОЧНЫМ планом действий в формате:
            ACTION: параметры
            
            Доступные действия:
            - CREATE_DISCIPLINES: список предметов через запятую
            - CREATE_GROUPS: список классов через запятую  
            - CREATE_TEACHERS: количество учителей
            - CREATE_STUDENTS: количество учеников
            - CREATE_PARENTS: создать родителей
            - CREATE_MARKS: поставить оценки
            - CREATE_NEWS: создать новости
            
            Если это вопрос для анализа - дай подробный ответ с цифрами.
            """, schoolName, dbContext, action);

        return callOpenAI(prompt);
    }

    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}