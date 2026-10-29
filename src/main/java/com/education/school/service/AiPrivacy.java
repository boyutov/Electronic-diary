package com.education.school.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.*;
import java.util.regex.Pattern;

/** Request-scoped substitutions. The reverse map never leaves the server. */
public final class AiPrivacy {
    private final Map<String, String> originals = new LinkedHashMap<>();
    private final Pattern sensitive;
    // Только буквы: цифры в метке могли случайно совпасть с фрагментом телефона или почты
    private final String prefix = "PRIVATE_" + UUID.randomUUID().toString().replace("-", "").chars()
        .map(c -> 'a' + Character.digit(c, 16)).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append) + "_";

    public AiPrivacy(Collection<String> knownValues) {
        List<String> patterns = new ArrayList<>();
        knownValues.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
            .distinct().sorted(Comparator.comparingInt(String::length).reversed())
            .forEach(s -> patterns.add("(?<![\\p{L}\\p{N}])" + Pattern.quote(s) + "(?![\\p{L}\\p{N}])"));
        // ФИО, которых ещё нет в базе («Добавь Жумабеков Эрлан»): 2–3 подряд слова с заглавной кириллицей.
        // (?-i:) — регистр здесь важен, иначе под шаблон попадёт любая пара слов
        patterns.add("(?-i:(?<![\\p{L}\\p{N}])[А-ЯЁӨҮҢ][а-яёөүң]+(?:-[А-ЯЁӨҮҢ][а-яёөүң]+)?(?:\\s+[А-ЯЁӨҮҢ][а-яёөүң]+(?:-[А-ЯЁӨҮҢ][а-яёөүң]+)?){1,2}(?![\\p{L}\\p{N}]))");
        patterns.add("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
        patterns.add("(?<!\\d)\\+?\\d[\\d ()-]{7,}\\d(?!\\d)");
        sensitive = Pattern.compile(String.join("|", patterns), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** Подстановки для всех людей школы: имена, фамилии, отчества, логины и почты. */
    public static AiPrivacy forSchool(String schoolName, Collection<com.education.school.entity.User> people) {
        List<String> known = new ArrayList<>();
        known.add(schoolName);
        for (var person : people) {
            known.add(person.getFirstName()); known.add(person.getSecondName());
            known.add(person.getThirdName()); known.add(person.getEmail());
            known.add(person.getContactEmail());
        }
        return new AiPrivacy(known);
    }

    public String mask(String value) {
        var matcher = sensitive.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String original = matcher.group();
            // ФИО — отдельная метка на каждое слово, чтобы ИИ мог разложить их по полям «фамилия»/«имя»
            String replacement = original.matches("[\\p{L}-]+(\\s+[\\p{L}-]+)+")
                ? Arrays.stream(original.split("\\s+")).map(this::token).collect(java.util.stream.Collectors.joining(" "))
                : token(original);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String token(String original) {
        return originals.entrySet().stream().filter(e -> e.getValue().equals(original))
            .map(Map.Entry::getKey).findFirst().orElseGet(() -> {
                String key = prefix + originals.size(); originals.put(key, original); return key;
            });
    }

    public String restore(String text) {
        // Longest token first: token 1 must not replace the prefix of token 10.
        for (String key : originals.keySet().stream().sorted(Comparator.comparingInt(String::length).reversed()).toList()) {
            text = text.replace(key, originals.get(key));
        }
        return text;
    }

    public JsonNode restore(JsonNode node) {
        if (node.isTextual()) return TextNode.valueOf(restore(node.asText()));
        if (node.isObject()) {
            var result = node.deepCopy();
            node.fields().forEachRemaining(e -> ((com.fasterxml.jackson.databind.node.ObjectNode) result).set(e.getKey(), restore(e.getValue())));
            return result;
        }
        if (node.isArray()) {
            var result = ((com.fasterxml.jackson.databind.node.ArrayNode) node).arrayNode();
            node.forEach(value -> result.add(restore(value)));
            return result;
        }
        return node;
    }

    public static boolean containsSensitiveTopic(String text) {
        return Pattern.compile("(?iu)(парол|password|passwd|секрет|secret|токен|token|bearer|api[_ -]?key|sk-[a-z0-9]|паспорт|диагноз|медицин|жалоб)")
            .matcher(text).find();
    }
}
