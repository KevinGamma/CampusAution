package com.campus.auction.filter;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Enables @QueryParam LocalDate in JAX-RS resource methods.
 *
 * Mirrors Spring MVC's @DateTimeFormat(iso = ISO.DATE) behaviour:
 * accepts ISO-8601 date strings (yyyy-MM-dd), returns null for absent params.
 */
@Provider
public class LocalDateParamConverter implements ParamConverterProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(Class<T> rawType,
                                               Type genericType,
                                               Annotation[] annotations) {
        if (!LocalDate.class.equals(rawType)) {
            return null;
        }
        return (ParamConverter<T>) new ParamConverter<LocalDate>() {
            @Override
            public LocalDate fromString(String value) {
                if (value == null || value.isBlank()) return null;
                try {
                    return LocalDate.parse(value);
                } catch (DateTimeParseException ex) {
                    throw new jakarta.ws.rs.BadRequestException(
                            "Invalid date format '" + value + "' — expected yyyy-MM-dd");
                }
            }

            @Override
            public String toString(LocalDate value) {
                return value == null ? null : value.toString();
            }
        };
    }
}
