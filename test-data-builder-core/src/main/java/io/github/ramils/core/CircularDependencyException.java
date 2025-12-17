package io.github.ramils.core;

/**
 * Thrown when a circular dependency is detected during entity building
 * that cannot be resolved automatically.
 */
public class CircularDependencyException extends RuntimeException {

    private final Class<?> sourceType;
    private final Class<?> targetType;
    private final String propertyName;

    public CircularDependencyException(Class<?> sourceType, String propertyName, Class<?> targetType) {
        super(buildMessage(sourceType, propertyName, targetType));
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.propertyName = propertyName;
    }

    private static String buildMessage(Class<?> sourceType, String propertyName, Class<?> targetType) {
        return String.format(
                "Circular dependency detected: %s.%s -> %s%n" +
                "The target type is already being built in the current chain.%n" +
                "Hint: Use .set(%s::%s, existingInstance) or .setNull(%s::%s) to break the cycle.",
                sourceType.getSimpleName(), propertyName, targetType.getSimpleName(),
                sourceType.getSimpleName(), getterName(propertyName),
                sourceType.getSimpleName(), getterName(propertyName)
        );
    }

    private static String getterName(String propertyName) {
        return "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    }

    public Class<?> getSourceType() {
        return sourceType;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
