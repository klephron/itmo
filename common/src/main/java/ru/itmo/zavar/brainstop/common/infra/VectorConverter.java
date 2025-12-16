package ru.itmo.zavar.brainstop.common.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Converter(autoApply = false)
public class VectorConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }

        String body = IntStream.range(0, attribute.length)
                .mapToObj(i -> Float.toString(attribute[i]))
                .collect(Collectors.joining(","));

        return "[" + body + "]";
    }


    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String s = dbData.trim();
        if (s.isEmpty() || s.equals("[]")) {
            return new float[0];
        }
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1);
        }
        String[] parts = s.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
