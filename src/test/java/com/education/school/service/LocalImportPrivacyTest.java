package com.education.school.service;

import com.education.school.config.AIConfig;
import com.education.school.entity.GroupEntity;
import com.education.school.repository.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClientException;
import java.io.ByteArrayOutputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalImportPrivacyTest {
    private final GroupRepository groups = mock(GroupRepository.class);
    private final StudentEnrollmentService enrollment = mock(StudentEnrollmentService.class);
    private final CredentialMailService mail = mock(CredentialMailService.class);
    private final ExcelImportService service = new ExcelImportService(groups,
        mock(DisciplineRepository.class), mock(TeacherService.class), enrollment, mail);

    private byte[] workbook(String[][] rows) throws Exception {
        try (var wb = new XSSFWorkbook(); var bytes = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet();
            for (int i = 0; i < rows.length; i++) {
                var row = sheet.createRow(i);
                for (int j = 0; j < rows[i].length; j++) row.createCell(j).setCellValue(rows[i][j]);
            }
            wb.write(bytes);
            return bytes.toByteArray();
        }
    }

    @Test void previewAndImportUseSameColumnsAndEnrollmentService() throws Exception {
        byte[] bytes = workbook(new String[][]{{"ФИО", "", "Почта"},
            {"Асанов Айбек", "", "a@example.org"}, {"Бекова Нурайым", "", "b@example.org"}});
        var preview = service.preview(new MockMultipartFile("file", "class.xlsx", "application/octet-stream", bytes));
        assertEquals(List.of("ФИО", "Колонка 2", "Почта"), preview.get("headers"));
        var group = new GroupEntity(); group.setId(7); group.setName("9А");
        when(groups.findAll()).thenReturn(List.of(group));
        when(enrollment.enroll(any())).thenAnswer(invocation -> {
            StudentEnrollmentService.Enrollment row = invocation.getArgument(0);
            assertSame(group, row.group());
            if (row.studentEmail().equals("b@example.org")) throw new IllegalArgumentException("duplicate");
            assertEquals("Айбек", row.firstName());
            assertEquals("Асанов", row.secondName());
            return "Асанов Айбек";
        });
        var result = service.importStudents(Map.of("fileData", Base64.getEncoder().encodeToString(bytes),
            "mapping", preview.get("suggestedMapping"), "defaultGroup", "9А"));
        assertEquals(1, result.get("added"));
        assertEquals(1, result.get("errors"));
        verify(enrollment, times(2)).enroll(any());
        verify(mail).requireConfigured();
    }

    @Test void refusesMissingMappingBeforeCreatingAccounts() {
        assertThrows(IllegalArgumentException.class, () -> service.importStudents(Map.of("fileData", "")));
        verifyNoInteractions(enrollment);
    }

    @Test void refusesOversizedUpload() {
        assertThrows(IllegalArgumentException.class, () -> service.preview(
            new MockMultipartFile("file", new byte[5_000_001])));
        verifyNoInteractions(enrollment);
    }

    @Test void legacyHttpClientCannotSendSchoolDataEvenWithConfiguredUrl() {
        assertThrows(RestClientException.class, () -> new AIConfig().restTemplate()
            .postForObject("https://api.openai.com/v1/chat/completions", "private pupil data", String.class));
    }

    @Test void masksKnownNamesAndNewContactsAndRestoresToolArguments() throws Exception {
        AiPrivacy privacy = new AiPrivacy(List.of("Асанов", "Айбек", "plit-99"));
        String original = "Добавь Асанов Айбек в plit-99, почта aibek@example.org, телефон +996 555 123456";
        String masked = privacy.mask(original);
        assertFalse(masked.contains("Асанов"));
        assertFalse(masked.contains("Айбек"));
        assertFalse(masked.contains("plit-99"));
        assertFalse(masked.contains("example.org"));
        assertFalse(masked.contains("555"));
        assertEquals(original, privacy.restore(masked));
        var node = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        node.put("message", masked);
        assertEquals(original, privacy.restore(node).path("message").asText());
    }

    @Test void masksFullNamesOfPeopleNotYetInDatabase() {
        AiPrivacy privacy = new AiPrivacy(List.of());
        String original = "Добавь ученика Жумабеков Эрлан Асанович в 8В и учителя Мамат-Уулу Бакыт";
        String masked = privacy.mask(original);
        assertFalse(masked.contains("Жумабеков"));
        assertFalse(masked.contains("Эрлан"));
        assertFalse(masked.contains("Бакыт"));
        assertTrue(masked.startsWith("Добавь ученика "));
        assertTrue(masked.contains(" в 8В "));
        assertEquals(original, privacy.restore(masked));
        assertEquals("создай расписание для 9А, предметы Русский язык и Математика",
            privacy.mask("создай расписание для 9А, предметы Русский язык и Математика"));
    }

    @Test void rejectsSensitiveTopicsBeforeExternalRequest() {
        assertTrue(AiPrivacy.containsSensitiveTopic("пароль ученика qwerty123"));
        assertTrue(AiPrivacy.containsSensitiveTopic("диагноз ребёнка"));
        assertTrue(AiPrivacy.containsSensitiveTopic("Bearer abc123"));
        assertFalse(AiPrivacy.containsSensitiveTopic("создай расписание для 9А"));
    }

    @Test void restoresMoreThanTenDistinctAliasesWithoutPrefixCollisions() {
        AiPrivacy privacy = new AiPrivacy(List.of());
        String original = java.util.stream.IntStream.range(0, 15)
            .mapToObj(i -> "student" + i + "@example.org").collect(java.util.stream.Collectors.joining(" "));
        assertEquals(original, privacy.restore(privacy.mask(original)));
    }
}
