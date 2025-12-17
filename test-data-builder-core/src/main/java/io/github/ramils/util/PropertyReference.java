package io.github.ramils.util;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Functional interface for type-safe property references using method references.
 * <p>
 * Usage: {@code PropertyReference<User, String> ref = User::getEmail;}
 *
 * @param <T> the entity type
 * @param <R> the property return type
 */
@FunctionalInterface
public interface PropertyReference<T, R> extends Function<T, R>, Serializable {

    /**
     * Extracts the property name from a method reference.
     * <p>
     * Example: User::getEmail -> "email"
     */
    default String getPropertyName() {
        try {
            Method writeReplace = getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(this);

            String methodName = lambda.getImplMethodName();
            return extractPropertyName(methodName);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract property name from method reference", e);
        }
    }

    /**
     * Returns the entity class this reference belongs to.
     */
    default Class<?> getEntityClass() {
        try {
            Method writeReplace = getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(this);

            String className = lambda.getImplClass().replace('/', '.');
            return Class.forName(className);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract entity class from method reference", e);
        }
    }

    /**
     * Converts getter method name to property name.
     * <p>
     * Examples:
     * - getEmail -> email
     * - isActive -> active
     * - getURL -> URL (preserves consecutive uppercase)
     */
    private static String extractPropertyName(String methodName) {
        String propertyName;
        if (methodName.startsWith("get") && methodName.length() > 3) {
            propertyName = methodName.substring(3);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            propertyName = methodName.substring(2);
        } else {
            // Direct field reference
            return methodName;
        }

        // Handle first character
        if (propertyName.length() == 1) {
            return propertyName.toLowerCase();
        }

        // If second char is uppercase (like URL), keep as is
        if (Character.isUpperCase(propertyName.charAt(1))) {
            return propertyName;
        }

        return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
    }
}
