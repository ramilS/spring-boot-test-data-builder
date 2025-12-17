package io.github.ramils.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Metadata about an entity property, including its constraints and association info.
 */
public class PropertyMetadata {

    private final String name;
    private final Class<?> type;
    private final Class<?> declaringClass;
    private final Field field;
    private final Set<Annotation> constraints;

    // Flag to track if setAccessible has been called (for performance optimization)
    private volatile boolean fieldAccessibleSet;

    // Association info
    private final boolean association;
    private final AssociationType associationType;
    private final Class<?> associatedEntityType;
    private final boolean owning;
    private final String mappedBy;

    // Constraint info (extracted from annotations)
    private final boolean required;
    private final boolean nullable;
    private final boolean unique;
    private final Integer minLength;
    private final Integer maxLength;
    private final Number minValue;
    private final Number maxValue;
    private final String pattern;
    private final boolean email;
    private final Integer collectionMinSize;
    private final Integer collectionMaxSize;
    private final boolean embedded;

    private PropertyMetadata(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.declaringClass = builder.declaringClass;
        this.field = builder.field;
        this.constraints = Collections.unmodifiableSet(new HashSet<>(builder.constraints));
        this.association = builder.association;
        this.associationType = builder.associationType;
        this.associatedEntityType = builder.associatedEntityType;
        this.owning = builder.owning;
        this.mappedBy = builder.mappedBy;
        this.required = builder.required;
        this.nullable = builder.nullable;
        this.unique = builder.unique;
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.pattern = builder.pattern;
        this.email = builder.email;
        this.collectionMinSize = builder.collectionMinSize;
        this.collectionMaxSize = builder.collectionMaxSize;
        this.embedded = builder.embedded;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    public Field getField() {
        return field;
    }

    /**
     * Returns the field with accessibility already set.
     * This method ensures setAccessible(true) is called only once per field,
     * improving performance when setting field values multiple times.
     *
     * @return the accessible field
     */
    public Field getAccessibleField() {
        if (!fieldAccessibleSet && field != null) {
            synchronized (this) {
                if (!fieldAccessibleSet) {
                    field.setAccessible(true);
                    fieldAccessibleSet = true;
                }
            }
        }
        return field;
    }

    public Set<Annotation> getConstraints() {
        return constraints;
    }

    public boolean isAssociation() {
        return association;
    }

    public AssociationType getAssociationType() {
        return associationType;
    }

    public Class<?> getAssociatedEntityType() {
        return associatedEntityType;
    }

    public boolean isOwning() {
        return owning;
    }

    public String getMappedBy() {
        return mappedBy;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isUnique() {
        return unique;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public Number getMinValue() {
        return minValue;
    }

    public Number getMaxValue() {
        return maxValue;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean isEmail() {
        return email;
    }

    public Integer getCollectionMinSize() {
        return collectionMinSize;
    }

    public Integer getCollectionMaxSize() {
        return collectionMaxSize;
    }

    public boolean isCollection() {
        return java.util.Collection.class.isAssignableFrom(type);
    }

    public boolean isEnum() {
        return type.isEnum();
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum AssociationType {
        ONE_TO_ONE,
        MANY_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_MANY,
        EMBEDDED,
        NONE
    }

    public static class Builder {
        private String name;
        private Class<?> type;
        private Class<?> declaringClass;
        private Field field;
        private Set<Annotation> constraints = new HashSet<>();
        private boolean association;
        private AssociationType associationType = AssociationType.NONE;
        private Class<?> associatedEntityType;
        private boolean owning = true;
        private String mappedBy;
        private boolean required;
        private boolean nullable = true;
        private boolean unique;
        private Integer minLength;
        private Integer maxLength;
        private Number minValue;
        private Number maxValue;
        private String pattern;
        private boolean email;
        private Integer collectionMinSize;
        private Integer collectionMaxSize;
        private boolean embedded;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(Class<?> type) {
            this.type = type;
            return this;
        }

        public Builder declaringClass(Class<?> declaringClass) {
            this.declaringClass = declaringClass;
            return this;
        }

        public Builder field(Field field) {
            this.field = field;
            return this;
        }

        public Builder addConstraint(Annotation constraint) {
            this.constraints.add(constraint);
            return this;
        }

        public Builder association(boolean association) {
            this.association = association;
            return this;
        }

        public Builder associationType(AssociationType associationType) {
            this.associationType = associationType;
            return this;
        }

        public Builder associatedEntityType(Class<?> associatedEntityType) {
            this.associatedEntityType = associatedEntityType;
            return this;
        }

        public Builder owning(boolean owning) {
            this.owning = owning;
            return this;
        }

        public Builder mappedBy(String mappedBy) {
            this.mappedBy = mappedBy;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        public Builder minLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder maxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder minValue(Number minValue) {
            this.minValue = minValue;
            return this;
        }

        public Builder maxValue(Number maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder email(boolean email) {
            this.email = email;
            return this;
        }

        public Builder collectionMinSize(Integer collectionMinSize) {
            this.collectionMinSize = collectionMinSize;
            return this;
        }

        public Builder collectionMaxSize(Integer collectionMaxSize) {
            this.collectionMaxSize = collectionMaxSize;
            return this;
        }

        public Builder embedded(boolean embedded) {
            this.embedded = embedded;
            return this;
        }

        public PropertyMetadata build() {
            return new PropertyMetadata(this);
        }
    }
}
