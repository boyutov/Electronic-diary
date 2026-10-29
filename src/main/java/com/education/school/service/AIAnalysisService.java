package com.education.school.service;

import com.education.school.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AIAnalysisService {

    private final ObjectMapper objectMapper;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final DisciplineRepository disciplineRepository;
    private final MarkRepository markRepository;
    private final AttendanceRepository attendanceRepository;
    private final ComplaintRepository complaintRepository;
    private final OpenAiClient openAiClient;
    private final CurrentSchoolService currentSchoolService;

    // Во внешний ИИ уходят только агрегаты (количества, средний балл) и замаскированный вопрос —
    // ни ФИО, ни почт, ни оценок конкретных учеников
    public String analyzeSchoolData(String query, String schoolName) {
        String summary = buildDatabaseContext(schoolName);
        if (!openAiClient.isConfigured()) return "Локальная сводка школы:\n" + summary;

        AiPrivacy privacy = AiPrivacy.forSchool(schoolName, currentSchoolService.findUsers());
        String system = """
            Ты — помощник для анализа обезличенной статистики школы.
            Значения PRIVATE_* — непрозрачные обозначения, не пытайся их расшифровать.
            Дай краткий ответ с конкретными цифрами и рекомендациями. Отвечай на языке вопроса.
            """;
        String prompt = "Статистика школы:\n" + privacy.mask(summary) + "\nВопрос: " + privacy.mask(query);
        try {
            return privacy.restore(openAiClient.complete(system, prompt, 1500));
        } catch (Exception e) {
            return "ИИ временно недоступен. Локальная сводка школы:\n" + summary;
        }
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
        // Содержимое файлов — это персональные данные, во внешний ИИ их не отправляем
        return "Анализ файлов во внешнем ИИ отключён: в файлах персональные данные. Для учеников и учителей используйте импорт Excel — он работает локально.";
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
        // Содержимое файлов — это персональные данные, во внешний ИИ их не отправляем
        return "Анализ файлов во внешнем ИИ отключён: в файлах персональные данные. Для учеников и учителей используйте импорт Excel — он работает локально.";
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