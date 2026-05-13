package com.auraspark.note.core.config;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Configuration
public class HashIdConfig implements WebMvcConfigurer {

    private static final String SALT = "auraspark_note_2026";
    private static final int MIN_LENGTH = 8;
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    @Bean
    public HashIdCodec hashIdCodec() {
        return new HashIdCodec();
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, Long>() {
            private final HashIdCodec codec = new HashIdCodec();
            @Override
            public Long convert(String source) {
                if (source == null || source.isBlank()) return null;
                if (source.matches("\\d+")) return Long.parseLong(source);
                return codec.decode(source);
            }
        });
    }

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonSerialize(using = IdSerializer.class)
    @JsonDeserialize(using = IdDeserializer.class)
    public @interface EncodedId {}

    public static class IdSerializer extends JsonSerializer<Long> {
        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider p) throws IOException {
            if (value == null) { gen.writeNull(); return; }
            gen.writeString(SpringContextHolder.getBean(HashIdCodec.class).encode(value));
        }
    }

    public static class IdDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) return null;
            if (text.matches("\\d+")) return Long.parseLong(text);
            return SpringContextHolder.getBean(HashIdCodec.class).decode(text);
        }
    }

    public static class HashIdCodec {
        private final String salt;
        private final int minLength;

        public HashIdCodec() {
            this.salt = SALT;
            this.minLength = MIN_LENGTH;
        }

        public String encode(long num) {
            StringBuilder result = new StringBuilder();
            long value = num;
            int saltIndex = 0;
            while (value > 0 || result.length() < minLength) {
                int idx = (int) ((value + salt.charAt(saltIndex % salt.length())) % BASE62.length());
                result.insert(0, BASE62.charAt(idx));
                value = value / BASE62.length();
                saltIndex++;
                if (result.length() >= 50) break;
            }
            return result.toString();
        }

        public Long decode(String encoded) {
            if (encoded == null || encoded.isBlank()) return null;
            long result = 0;
            long power = 1;
            int len = encoded.length();
            for (int i = 0; i < len; i++) {
                char c = encoded.charAt(len - 1 - i);
                int idx = BASE62.indexOf(c);
                if (idx < 0) return null;
                int si = i % salt.length();
                int digit = ((idx - (salt.charAt(si) % BASE62.length())) + BASE62.length()) % BASE62.length();
                result += (long) digit * power;
                power *= BASE62.length();
                if (result < 0) return null;
            }
            return result;
        }
    }
}
