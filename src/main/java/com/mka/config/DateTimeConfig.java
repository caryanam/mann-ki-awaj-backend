package com.mka.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class DateTimeConfig {

    private static final DateTimeFormatter UTC_INSTANT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.timeZone(TimeZone.getTimeZone("UTC"));
            builder.featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            JavaTimeModule javaTimeModule = new JavaTimeModule();

            // Explicit Instant serializer: guarantees ISO-8601 format ending in 'Z'
            javaTimeModule.addSerializer(Instant.class, new JsonSerializer<>() {
                @Override
                public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                    if (value == null) {
                        gen.writeNull();
                    } else {
                        gen.writeString(UTC_INSTANT_FORMATTER.format(value));
                    }
                }
            });

            // Fallback LocalDateTime serializer: ensures any residual LocalDateTime is interpreted as UTC and serialized with 'Z'
            javaTimeModule.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                    if (value == null) {
                        gen.writeNull();
                    } else {
                        gen.writeString(value.atZone(ZoneOffset.UTC).toInstant().toString());
                    }
                }
            });

            builder.modules(javaTimeModule);
        };
    }
}
