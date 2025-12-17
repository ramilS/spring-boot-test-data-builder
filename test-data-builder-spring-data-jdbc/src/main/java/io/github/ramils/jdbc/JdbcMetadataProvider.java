package io.github.ramils.jdbc;

import io.github.ramils.metadata.EntityMetadata;
import io.github.ramils.metadata.MetadataProvider;
import io.github.ramils.metadata.PropertyMetadata;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MetadataProvider for Spring Data JDBC entities.
 * <p>
 * Analyzes Spring Data JDBC annotations like @Table, @Column, @Id,
 * @MappedCollection, @Embedded.
 * Also supports Bean Validation annotations if present.
 *
 * <pre>{@code
 * TestData.registerProvider(new JdbcMetadataProvider());
 * }</pre>
 */
public class JdbcMetadataProvider implements MetadataProvider {

    private static final Map<Class<?>, EntityMetadata> CACHE = new ConcurrentHashMap<>();

    // Bean Validation annotation classes (loaded dynamically to avoid hard dependency)
    private static Class<?> notNullClass;
    private static Class<?> notBlankClass;
    private static Class<?> notEmptyClass;
    private static Class<?> sizeClass;
    private static Class<?> minClass;
    private static Class<?> maxClass;
    private static Class<?> emailClass;
    private static Class<?> patternClass;

    static {
        try {
            notNullClass = Class.forName("jakarta.validation.constraints.NotNull");
            notBlankClass = Class.forName("jakarta.validation.constraints.NotBlank");
            notEmptyClass = Class.forName("jakarta.validation.constraints.NotEmpty");
            sizeClass = Class.forName("jakarta.validation.constraints.Size");
            minClass = Class.forName("jakarta.validation.constraints.Min");
            maxClass = Class.forName("jakarta.validation.constraints.Max");
            emailClass = Class.forName("jakarta.validation.constraints.Email");
            patternClass = Class.forName("jakarta.validation.constraints.Pattern");
        } catch (ClassNotFoundException e) {
            // Bean Validation not on classpath, that's OK
        }
    }

    @Override
    public EntityMetadata analyze(Class<?> entityClass) {
        return CACHE.computeIfAbsent(entityClass, this::doAnalyze);
    }

    @Override
    public boolean supports(Class<?> entityClass) {
        // Support @Table annotated classes
        if (entityClass.isAnnotationPresent(Table.class)) {
            return true;
        }
        // Support any class that has @Id field (Spring Data JDBC convention)
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return true;
            }
        }
        // Support plain classes for flexible testing
        return true;
    }

    @Override
    public void clearCache() {
        CACHE.clear();
    }

    @Override
    public int getPriority() {
        return 50; // Lower than JPA, so JPA takes precedence if both are present
    }

    private EntityMetadata doAnalyze(Class<?> entityClass) {
        Map<String, PropertyMetadata> properties = new LinkedHashMap<>();

        Class<?> currentClass = entityClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (shouldIncludeField(field)) {
                    PropertyMetadata metadata = analyzeField(field, entityClass);
                    properties.putIfAbsent(field.getName(), metadata);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        boolean isAbstract = Modifier.isAbstract(entityClass.getModifiers());

        return new EntityMetadata(entityClass, properties, isAbstract, null);
    }

    private boolean shouldIncludeField(Field field) {
        int modifiers = field.getModifiers();

        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
            return false;
        }

        if (field.isAnnotationPresent(Transient.class)) {
            return false;
        }

        return true;
    }

    private PropertyMetadata analyzeField(Field field, Class<?> declaringClass) {
        PropertyMetadata.Builder builder = PropertyMetadata.builder()
                .name(field.getName())
                .type(field.getType())
                .declaringClass(declaringClass)
                .field(field);

        analyzeSpringDataAnnotations(field, builder);
        analyzeValidationAnnotations(field, builder);

        boolean required = determineIfRequired(field);
        builder.required(required);

        return builder.build();
    }

    private void analyzeSpringDataAnnotations(Field field, PropertyMetadata.Builder builder) {
        // @Id
        if (field.isAnnotationPresent(Id.class)) {
            builder.required(true).nullable(false);
        }

        // @Column - Spring Data JDBC version
        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            // Spring Data JDBC's @Column doesn't have nullable/unique attributes
            // like JPA's @Column, so we rely on validation annotations
        }

        // @Embedded (including @Embedded.Nullable, @Embedded.Empty)
        if (isEmbedded(field)) {
            builder.association(true)
                    .associationType(PropertyMetadata.AssociationType.EMBEDDED)
                    .associatedEntityType(field.getType())
                    .embedded(true)
                    .owning(true);
        }

        // @MappedCollection - One-to-Many relationship
        MappedCollection mappedCollection = field.getAnnotation(MappedCollection.class);
        if (mappedCollection != null) {
            Class<?> elementType = extractCollectionElementType(field);
            builder.association(true)
                    .associationType(PropertyMetadata.AssociationType.ONE_TO_MANY)
                    .associatedEntityType(elementType)
                    .nullable(true)
                    .owning(true);
        }

        // Handle collections without @MappedCollection (aggregate references)
        if (Collection.class.isAssignableFrom(field.getType()) && mappedCollection == null) {
            Class<?> elementType = extractCollectionElementType(field);
            // Check if element type looks like an entity (has @Id)
            if (hasIdField(elementType)) {
                builder.association(true)
                        .associationType(PropertyMetadata.AssociationType.ONE_TO_MANY)
                        .associatedEntityType(elementType)
                        .nullable(true);
            }
        }

        // Handle single-valued references (aggregate references)
        if (!field.getType().isPrimitive()
            && !field.getType().getName().startsWith("java.")
            && !field.isAnnotationPresent(Embedded.class)
            && !Collection.class.isAssignableFrom(field.getType())
            && hasIdField(field.getType())) {
            builder.association(true)
                    .associationType(PropertyMetadata.AssociationType.MANY_TO_ONE)
                    .associatedEntityType(field.getType())
                    .nullable(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void analyzeValidationAnnotations(Field field, PropertyMetadata.Builder builder) {
        if (notNullClass == null) {
            return; // Bean Validation not available
        }

        try {
            // @NotNull
            if (field.isAnnotationPresent((Class) notNullClass)) {
                builder.nullable(false).required(true);
            }

            // @NotBlank
            if (field.isAnnotationPresent((Class) notBlankClass)) {
                builder.nullable(false).required(true);
            }

            // @NotEmpty
            if (field.isAnnotationPresent((Class) notEmptyClass)) {
                builder.nullable(false).required(true);
            }

            // @Size
            if (sizeClass != null && field.isAnnotationPresent((Class) sizeClass)) {
                Object annotation = field.getAnnotation((Class) sizeClass);
                int min = (int) sizeClass.getMethod("min").invoke(annotation);
                int max = (int) sizeClass.getMethod("max").invoke(annotation);

                if (Collection.class.isAssignableFrom(field.getType())) {
                    builder.collectionMinSize(min);
                    builder.collectionMaxSize(max == Integer.MAX_VALUE ? null : max);
                } else {
                    builder.minLength(min);
                    builder.maxLength(max == Integer.MAX_VALUE ? null : max);
                }
            }

            // @Min
            if (minClass != null && field.isAnnotationPresent((Class) minClass)) {
                Object annotation = field.getAnnotation((Class) minClass);
                long value = (long) minClass.getMethod("value").invoke(annotation);
                builder.minValue(value);
            }

            // @Max
            if (maxClass != null && field.isAnnotationPresent((Class) maxClass)) {
                Object annotation = field.getAnnotation((Class) maxClass);
                long value = (long) maxClass.getMethod("value").invoke(annotation);
                builder.maxValue(value);
            }

            // @Email
            if (emailClass != null && field.isAnnotationPresent((Class) emailClass)) {
                builder.email(true);
            }

            // @Pattern
            if (patternClass != null && field.isAnnotationPresent((Class) patternClass)) {
                Object annotation = field.getAnnotation((Class) patternClass);
                String regexp = (String) patternClass.getMethod("regexp").invoke(annotation);
                builder.pattern(regexp);
            }

        } catch (Exception e) {
            // Ignore reflection errors
        }
    }

    @SuppressWarnings("unchecked")
    private boolean determineIfRequired(Field field) {
        // @Id fields are required (unless auto-generated, but JDBC doesn't have @GeneratedValue)
        if (field.isAnnotationPresent(Id.class)) {
            // In Spring Data JDBC, IDs can be null for new entities
            return false;
        }

        // Check validation annotations
        if (notNullClass != null) {
            try {
                if (field.isAnnotationPresent((Class) notNullClass)
                    || field.isAnnotationPresent((Class) notBlankClass)
                    || field.isAnnotationPresent((Class) notEmptyClass)) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        return false;
    }

    private Class<?> extractCollectionElementType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        return Object.class;
    }

    private boolean hasIdField(Class<?> type) {
        if (type == null || type == Object.class) {
            return false;
        }
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return true;
            }
        }
        return hasIdField(type.getSuperclass());
    }

    private boolean isEmbedded(Field field) {
        // Check for @Embedded, @Embedded.Nullable, @Embedded.Empty
        for (java.lang.annotation.Annotation annotation : field.getAnnotations()) {
            Class<?> annotationType = annotation.annotationType();
            // Check if it's Embedded or a nested annotation of Embedded
            if (annotationType == Embedded.class
                || annotationType.getDeclaringClass() == Embedded.class) {
                return true;
            }
        }
        return false;
    }
}
