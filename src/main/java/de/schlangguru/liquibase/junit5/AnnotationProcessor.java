package de.schlangguru.liquibase.junit5;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public class AnnotationProcessor {

    private final Class<?> testClass;

    public AnnotationProcessor(Class<?> testClass) {
        this.testClass = testClass;
    }

    public <R> Optional<Function<Object, R>> findProvider(Class<R> expectedReturnType) {
        return provideByField(expectedReturnType)
                .or(() -> provideByMethod(expectedReturnType));
    }

    private <R> Optional<Function<Object, R>> provideByField(Class<R> expectedReturnType) {
        return Arrays.stream(testClass.getDeclaredFields())
                .filter(this::isDatasourceProvider)
                .filter(f -> f.getType() == expectedReturnType)
                .findFirst()
                .map(this::fieldProvider);
    }

    private <R> Optional<Function<Object, R>> provideByMethod(Class<R> expectedReturnType) {
        return Arrays.stream(testClass.getDeclaredMethods())
                .filter(this::isDatasourceProvider)
                .filter(m -> m.getReturnType() == expectedReturnType)
                .findFirst()
                .map(this::methodProvider);
    }

    private boolean isDatasourceProvider(Method method) {
        return Optional.ofNullable(method.getAnnotation(ProvideForLiquibase.class)).isPresent();
    }

    private boolean isDatasourceProvider(Field field) {
        return Optional.ofNullable(field.getAnnotation(ProvideForLiquibase.class)).isPresent();
    }

    private <R> Function<Object, R> fieldProvider(Field field) {
        return (testInstance) -> {
            try {
                field.setAccessible(true);
                return (R) field.get(testInstance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field to retrieve DataSource.", e);
            }
        };
    }

    private <R> Function<Object, R> methodProvider(Method method) {
        return (testInstance) -> {
            try {
                method.setAccessible(true);
                return (R) method.invoke(testInstance);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Cannot invoke method to access DataSource.", e);
            }
        };
    }

}
