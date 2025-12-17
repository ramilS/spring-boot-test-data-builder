package io.github.ramils.jpa;

import io.github.ramils.metadata.EntityMetadata;
import io.github.ramils.metadata.MetadataProvider;
import io.github.ramils.metadata.PropertyMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MetadataProvider for JPA entities.
 * <p>
 * Supports both {@code javax.persistence} (Spring Boot 2.x) and
 * {@code jakarta.persistence} (Spring Boot 3.x) annotations using reflection.
 * <p>
 * Analyzes JPA annotations like @Entity, @Column, @ManyToOne, @OneToMany,
 * and Bean Validation annotations like @NotNull, @Size, @Email.
 *
 * <pre>{@code
 * TestData.registerProvider(new JpaMetadataProvider());
 * }</pre>
 */
public class JpaMetadataProvider implements MetadataProvider {

    private static final Map<Class<?>, EntityMetadata> CACHE = new ConcurrentHashMap<>();

    // JPA annotation class names (support both javax and jakarta) - using Set for O(1) lookup
    private static final Set<String> ENTITY_ANNOTATIONS = Set.of(
            "jakarta.persistence.Entity", "javax.persistence.Entity"
    );
    private static final Set<String> EMBEDDABLE_ANNOTATIONS = Set.of(
            "jakarta.persistence.Embeddable", "javax.persistence.Embeddable"
    );
    private static final Set<String> MAPPED_SUPERCLASS_ANNOTATIONS = Set.of(
            "jakarta.persistence.MappedSuperclass", "javax.persistence.MappedSuperclass"
    );
    private static final Set<String> TRANSIENT_ANNOTATIONS = Set.of(
            "jakarta.persistence.Transient", "javax.persistence.Transient"
    );
    private static final Set<String> ID_ANNOTATIONS = Set.of(
            "jakarta.persistence.Id", "javax.persistence.Id"
    );
    private static final Set<String> EMBEDDED_ID_ANNOTATIONS = Set.of(
            "jakarta.persistence.EmbeddedId", "javax.persistence.EmbeddedId"
    );
    private static final Set<String> GENERATED_VALUE_ANNOTATIONS = Set.of(
            "jakarta.persistence.GeneratedValue", "javax.persistence.GeneratedValue"
    );
    private static final Set<String> COLUMN_ANNOTATIONS = Set.of(
            "jakarta.persistence.Column", "javax.persistence.Column"
    );
    private static final Set<String> JOIN_COLUMN_ANNOTATIONS = Set.of(
            "jakarta.persistence.JoinColumn", "javax.persistence.JoinColumn"
    );
    private static final Set<String> ONE_TO_ONE_ANNOTATIONS = Set.of(
            "jakarta.persistence.OneToOne", "javax.persistence.OneToOne"
    );
    private static final Set<String> MANY_TO_ONE_ANNOTATIONS = Set.of(
            "jakarta.persistence.ManyToOne", "javax.persistence.ManyToOne"
    );
    private static final Set<String> ONE_TO_MANY_ANNOTATIONS = Set.of(
            "jakarta.persistence.OneToMany", "javax.persistence.OneToMany"
    );
    private static final Set<String> MANY_TO_MANY_ANNOTATIONS = Set.of(
            "jakarta.persistence.ManyToMany", "javax.persistence.ManyToMany"
    );
    private static final Set<String> EMBEDDED_ANNOTATIONS = Set.of(
            "jakarta.persistence.Embedded", "javax.persistence.Embedded"
    );

    // Validation annotation class names (support both javax and jakarta) - using Set for O(1) lookup
    private static final Set<String> NOT_NULL_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.NotNull", "javax.validation.constraints.NotNull"
    );
    private static final Set<String> NOT_BLANK_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.NotBlank", "javax.validation.constraints.NotBlank"
    );
    private static final Set<String> NOT_EMPTY_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.NotEmpty", "javax.validation.constraints.NotEmpty"
    );
    private static final Set<String> SIZE_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.Size", "javax.validation.constraints.Size"
    );
    private static final Set<String> MIN_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.Min", "javax.validation.constraints.Min"
    );
    private static final Set<String> MAX_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.Max", "javax.validation.constraints.Max"
    );
    private static final Set<String> DECIMAL_MIN_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.DecimalMin", "javax.validation.constraints.DecimalMin"
    );
    private static final Set<String> DECIMAL_MAX_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.DecimalMax", "javax.validation.constraints.DecimalMax"
    );
    private static final Set<String> PATTERN_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.Pattern", "javax.validation.constraints.Pattern"
    );
    private static final Set<String> EMAIL_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.Email", "javax.validation.constraints.Email"
    );
    private static final Set<String> POSITIVE_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.Positive", "javax.validation.constraints.Positive"
    );
    private static final Set<String> POSITIVE_OR_ZERO_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.PositiveOrZero", "javax.validation.constraints.PositiveOrZero"
    );
    private static final Set<String> NEGATIVE_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.Negative", "javax.validation.constraints.Negative"
    );
    private static final Set<String> NEGATIVE_OR_ZERO_ANNOTATIONS = Set.of(
            "jakarta.validation.constraints.NegativeOrZero", "javax.validation.constraints.NegativeOrZero"
    );

    @Override
    public EntityMetadata analyze(Class<?> entityClass) {
        return CACHE.computeIfAbsent(entityClass, this::doAnalyze);
    }

    @Override
    public boolean supports(Class<?> entityClass) {
        // Support JPA @Entity classes
        if (hasAnnotation(entityClass, ENTITY_ANNOTATIONS)) {
            return true;
        }
        // Support @Embeddable classes
        if (hasAnnotation(entityClass, EMBEDDABLE_ANNOTATIONS)) {
            return true;
        }
        // Support @MappedSuperclass
        if (hasAnnotation(entityClass, MAPPED_SUPERCLASS_ANNOTATIONS)) {
            return true;
        }
        // Also support plain classes (for flexible testing)
        return true;
    }

    @Override
    public void clearCache() {
        CACHE.clear();
    }

    @Override
    public int getPriority() {
        return 100; // High priority for JPA
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
        Class<?> superclass = findEntitySuperclass(entityClass);

        return new EntityMetadata(entityClass, properties, isAbstract, superclass);
    }

    private boolean shouldIncludeField(Field field) {
        int modifiers = field.getModifiers();

        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
            return false;
        }

        if (hasAnnotation(field, TRANSIENT_ANNOTATIONS)) {
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

        analyzeJpaAnnotations(field, builder);
        analyzeValidationAnnotations(field, builder);

        boolean required = determineIfRequired(field, builder);
        builder.required(required);

        return builder.build();
    }

    private void analyzeJpaAnnotations(Field field, PropertyMetadata.Builder builder) {
        // @Column
        Annotation column = getAnnotation(field, COLUMN_ANNOTATIONS);
        if (column != null) {
            builder.nullable(getAnnotationValue(column, "nullable", true));
            builder.unique(getAnnotationValue(column, "unique", false));
            int length = getAnnotationValue(column, "length", 255);
            if (length != 255) {
                builder.maxLength(length);
            }
        }

        // @Id
        if (hasAnnotation(field, ID_ANNOTATIONS)) {
            builder.required(true).nullable(false);
        }

        // @OneToOne
        Annotation oneToOne = getAnnotation(field, ONE_TO_ONE_ANNOTATIONS);
        if (oneToOne != null) {
            String mappedBy = getAnnotationValue(oneToOne, "mappedBy", "");
            builder.association(true)
                    .associationType(PropertyMetadata.AssociationType.ONE_TO_ONE)
                    .associatedEntityType(field.getType())
                    .nullable(getAnnotationValue(oneToOne, "optional", true))
                    .mappedBy(mappedBy.isEmpty() ? null : mappedBy)
                    .owning(mappedBy.isEmpty());
            builder.addConstraint(oneToOne);
        }

        // @ManyToOne
        Annotation manyToOne = getAnnotation(field, MANY_TO_ONE_ANNOTATIONS);
        if (manyToOne != null) {
            builder.association(true)
                    .associationType(PropertyMetadata.AssociationType.MANY_TO_ONE)
                    .associatedEntityType(field.getType())
                    .nullable(getAnnotationValue(manyToOne, "optional", true))
                    .owning(true);
            builder.addConstraint(manyToOne);
        }

        // @OneToMany
        Annotation oneToMany = getAnnotation(field, ONE_TO_MANY_ANNOTATIONS);
        if (oneToMany != null) {
            Class<?> elementType = extractCollectionElementType(field);
            String mappedBy = getAnnotationValue(oneToMany, "mappedBy", "");
            builder.association(true)
                    .associationType(PropertyMetadata.AssociationType.ONE_TO_MANY)
                    .associatedEntityType(elementType)
                    .nullable(true)
                    .mappedBy(mappedBy.isEmpty() ? null : mappedBy)
                    .owning(mappedBy.isEmpty());
            builder.addConstraint(oneToMany);
        }

        // @ManyToMany
        Annotation manyToMany = getAnnotation(field, MANY_TO_MANY_ANNOTATIONS);
        if (manyToMany != null) {
            Class<?> elementType = extractCollectionElementType(field);
            String mappedBy = getAnnotationValue(manyToMany, "mappedBy", "");
            builder.association(true)
                    .associationType(PropertyMetadata.AssociationType.MANY_TO_MANY)
                    .associatedEntityType(elementType)
                    .nullable(true)
                    .mappedBy(mappedBy.isEmpty() ? null : mappedBy)
                    .owning(mappedBy.isEmpty());
            builder.addConstraint(manyToMany);
        }

        // @Embedded
        if (hasAnnotation(field, EMBEDDED_ANNOTATIONS)) {
            builder.association(true)
                    .associationType(PropertyMetadata.AssociationType.EMBEDDED)
                    .associatedEntityType(field.getType())
                    .embedded(true)
                    .owning(true);
        }

        // @EmbeddedId
        if (hasAnnotation(field, EMBEDDED_ID_ANNOTATIONS)) {
            builder.association(true)
                    .associationType(PropertyMetadata.AssociationType.EMBEDDED)
                    .associatedEntityType(field.getType())
                    .embedded(true)
                    .required(true)
                    .nullable(false)
                    .owning(true);
        }

        // @JoinColumn
        Annotation joinColumn = getAnnotation(field, JOIN_COLUMN_ANNOTATIONS);
        if (joinColumn != null) {
            builder.nullable(getAnnotationValue(joinColumn, "nullable", true));
            builder.unique(getAnnotationValue(joinColumn, "unique", false));
        }
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private void analyzeValidationAnnotations(Field field, PropertyMetadata.Builder builder) {
        // @NotNull
        Annotation notNull = getAnnotation(field, NOT_NULL_ANNOTATIONS);
        if (notNull != null) {
            builder.nullable(false).required(true);
            builder.addConstraint(notNull);
        }

        // @NotBlank
        Annotation notBlank = getAnnotation(field, NOT_BLANK_ANNOTATIONS);
        if (notBlank != null) {
            builder.nullable(false).required(true);
            builder.addConstraint(notBlank);
        }

        // @NotEmpty
        Annotation notEmpty = getAnnotation(field, NOT_EMPTY_ANNOTATIONS);
        if (notEmpty != null) {
            builder.nullable(false).required(true);
            builder.addConstraint(notEmpty);
        }

        // @Size
        Annotation size = getAnnotation(field, SIZE_ANNOTATIONS);
        if (size != null) {
            int minSize = getAnnotationValue(size, "min", 0);
            int maxSize = getAnnotationValue(size, "max", Integer.MAX_VALUE);
            if (Collection.class.isAssignableFrom(field.getType())) {
                builder.collectionMinSize(minSize);
                builder.collectionMaxSize(maxSize == Integer.MAX_VALUE ? null : maxSize);
            } else {
                builder.minLength(minSize);
                builder.maxLength(maxSize == Integer.MAX_VALUE ? null : maxSize);
            }
            builder.addConstraint(size);
        }

        // @Min
        Annotation min = getAnnotation(field, MIN_ANNOTATIONS);
        if (min != null) {
            builder.minValue(getAnnotationValue(min, "value", 0L));
            builder.addConstraint(min);
        }

        // @Max
        Annotation max = getAnnotation(field, MAX_ANNOTATIONS);
        if (max != null) {
            builder.maxValue(getAnnotationValue(max, "value", Long.MAX_VALUE));
            builder.addConstraint(max);
        }

        // @DecimalMin
        Annotation decimalMin = getAnnotation(field, DECIMAL_MIN_ANNOTATIONS);
        if (decimalMin != null) {
            String value = getAnnotationValue(decimalMin, "value", "0");
            builder.minValue(new java.math.BigDecimal(value));
            builder.addConstraint(decimalMin);
        }

        // @DecimalMax
        Annotation decimalMax = getAnnotation(field, DECIMAL_MAX_ANNOTATIONS);
        if (decimalMax != null) {
            String value = getAnnotationValue(decimalMax, "value", "0");
            builder.maxValue(new java.math.BigDecimal(value));
            builder.addConstraint(decimalMax);
        }

        // @Pattern
        Annotation pattern = getAnnotation(field, PATTERN_ANNOTATIONS);
        if (pattern != null) {
            builder.pattern(getAnnotationValue(pattern, "regexp", ""));
            builder.addConstraint(pattern);
        }

        // @Email
        Annotation email = getAnnotation(field, EMAIL_ANNOTATIONS);
        if (email != null) {
            builder.email(true);
            builder.addConstraint(email);
        }

        // @Positive, @PositiveOrZero
        Annotation positive = getAnnotation(field, POSITIVE_ANNOTATIONS);
        if (positive != null) {
            builder.minValue(1);
            builder.addConstraint(positive);
        }
        Annotation positiveOrZero = getAnnotation(field, POSITIVE_OR_ZERO_ANNOTATIONS);
        if (positiveOrZero != null) {
            builder.minValue(0);
            builder.addConstraint(positiveOrZero);
        }

        // @Negative, @NegativeOrZero
        Annotation negative = getAnnotation(field, NEGATIVE_ANNOTATIONS);
        if (negative != null) {
            builder.maxValue(-1);
            builder.addConstraint(negative);
        }
        Annotation negativeOrZero = getAnnotation(field, NEGATIVE_OR_ZERO_ANNOTATIONS);
        if (negativeOrZero != null) {
            builder.maxValue(0);
            builder.addConstraint(negativeOrZero);
        }
    }

    private boolean determineIfRequired(Field field, PropertyMetadata.Builder builder) {
        if (hasAnnotation(field, ID_ANNOTATIONS) || hasAnnotation(field, EMBEDDED_ID_ANNOTATIONS)) {
            if (hasAnnotation(field, GENERATED_VALUE_ANNOTATIONS)) {
                return false;
            }
            return true;
        }

        if (hasAnnotation(field, NOT_NULL_ANNOTATIONS)
            || hasAnnotation(field, NOT_BLANK_ANNOTATIONS)
            || hasAnnotation(field, NOT_EMPTY_ANNOTATIONS)) {
            return true;
        }

        Annotation column = getAnnotation(field, COLUMN_ANNOTATIONS);
        if (column != null && !getAnnotationValue(column, "nullable", true)) {
            return true;
        }

        Annotation joinColumn = getAnnotation(field, JOIN_COLUMN_ANNOTATIONS);
        if (joinColumn != null && !getAnnotationValue(joinColumn, "nullable", true)) {
            return true;
        }

        Annotation manyToOne = getAnnotation(field, MANY_TO_ONE_ANNOTATIONS);
        if (manyToOne != null && !getAnnotationValue(manyToOne, "optional", true)) {
            return true;
        }

        Annotation oneToOne = getAnnotation(field, ONE_TO_ONE_ANNOTATIONS);
        if (oneToOne != null && !getAnnotationValue(oneToOne, "optional", true)) {
            return true;
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

    private Class<?> findEntitySuperclass(Class<?> entityClass) {
        Class<?> superclass = entityClass.getSuperclass();
        while (superclass != null && superclass != Object.class) {
            if (hasAnnotation(superclass, ENTITY_ANNOTATIONS)
                || hasAnnotation(superclass, MAPPED_SUPERCLASS_ANNOTATIONS)) {
                return superclass;
            }
            superclass = superclass.getSuperclass();
        }
        return null;
    }

    // ==================== Reflection Helper Methods ====================

    /**
     * Checks if a class has any of the specified annotations (by class name).
     * Optimized to use Set.contains() for O(1) lookup.
     */
    private boolean hasAnnotation(Class<?> clazz, Set<String> annotationClassNames) {
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotationClassNames.contains(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a field has any of the specified annotations (by class name).
     * Optimized to use Set.contains() for O(1) lookup.
     */
    private boolean hasAnnotation(Field field, Set<String> annotationClassNames) {
        for (Annotation annotation : field.getAnnotations()) {
            if (annotationClassNames.contains(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets an annotation from a field matching any of the specified class names.
     * Optimized to use Set.contains() for O(1) lookup.
     */
    private Annotation getAnnotation(Field field, Set<String> annotationClassNames) {
        for (Annotation annotation : field.getAnnotations()) {
            if (annotationClassNames.contains(annotation.annotationType().getName())) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Gets an attribute value from an annotation using reflection.
     */
    @SuppressWarnings("unchecked")
    private <T> T getAnnotationValue(Annotation annotation, String attributeName, T defaultValue) {
        try {
            Method method = annotation.annotationType().getMethod(attributeName);
            Object value = method.invoke(annotation);
            return value != null ? (T) value : defaultValue;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return defaultValue;
        }
    }
}
