package io.github.ramils.builder;

import io.github.ramils.config.TestDataConfig;
import io.github.ramils.core.BuildContext;
import io.github.ramils.core.CircularDependencyException;
import io.github.ramils.core.MaxDepthExceededException;
import io.github.ramils.generator.DefaultValueGenerators;
import io.github.ramils.generator.GeneratorContext;
import io.github.ramils.generator.ValueGenerator;
import io.github.ramils.metadata.EntityMetadata;
import io.github.ramils.metadata.MetadataProvider;
import io.github.ramils.metadata.PropertyMetadata;
import io.github.ramils.util.PropertyReference;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Fluent builder for creating entity instances with test data.
 * <p>
 * Provides methods to override auto-generated values, set sequences for lists,
 * derive values from other properties, and create single or multiple instances.
 *
 * <pre>{@code
 * // Single entity with overrides
 * User user = TestData.of(User.class)
 *     .set(User::getFirstName, "John")
 *     .set(User::getEmail, "john@example.com")
 *     .create();
 *
 * // Multiple entities with sequence
 * List<User> users = TestData.of(User.class)
 *     .sequence(User::getEmail, i -> "user" + i + "@test.com")
 *     .createList(10);
 * }</pre>
 *
 * @param <T> the entity type
 */
public class EntityBuilder<T> {

    private final Class<T> entityClass;
    private final TestDataConfig config;
    private final List<ValueGenerator> generators;

    // Lazily initialized collections - most builders don't use all of these
    private Map<String, Object> overrides;
    private Map<String, Supplier<?>> suppliers;
    private Map<String, IntFunction<?>> sequences;
    private Map<String, Function<T, ?>> derivations;
    private Set<String> explicitNulls;

    public EntityBuilder(Class<T> entityClass, TestDataConfig config) {
        this.entityClass = entityClass;
        this.config = config;
        this.generators = DefaultValueGenerators.getAll();
    }

    // Lazy initialization helpers for better memory efficiency
    private Map<String, Object> getOverrides() {
        if (overrides == null) {
            overrides = new LinkedHashMap<>(4);
        }
        return overrides;
    }

    private Map<String, Supplier<?>> getSuppliers() {
        if (suppliers == null) {
            suppliers = new LinkedHashMap<>(4);
        }
        return suppliers;
    }

    private Map<String, IntFunction<?>> getSequences() {
        if (sequences == null) {
            sequences = new LinkedHashMap<>(4);
        }
        return sequences;
    }

    private Map<String, Function<T, ?>> getDerivations() {
        if (derivations == null) {
            derivations = new LinkedHashMap<>(4);
        }
        return derivations;
    }

    private Set<String> getExplicitNulls() {
        if (explicitNulls == null) {
            explicitNulls = new HashSet<>(4);
        }
        return explicitNulls;
    }

    /**
     * Sets a fixed value for a property using a type-safe method reference.
     */
    public <V> EntityBuilder<T> set(PropertyReference<T, V> property, V value) {
        getOverrides().put(property.getPropertyName(), value);
        return this;
    }

    /**
     * Sets multiple property values from a map.
     */
    public EntityBuilder<T> setAll(Map<String, ?> values) {
        getOverrides().putAll(values);
        return this;
    }

    /**
     * Sets a supplier for a property, called each time an entity is created.
     */
    public <V> EntityBuilder<T> set(PropertyReference<T, V> property, Supplier<V> supplier) {
        getSuppliers().put(property.getPropertyName(), supplier);
        return this;
    }

    /**
     * Sets a sequence generator for a property.
     */
    public <V> EntityBuilder<T> sequence(PropertyReference<T, V> property, IntFunction<V> generator) {
        getSequences().put(property.getPropertyName(), generator);
        return this;
    }

    /**
     * Sets a derived value computed from the entity instance after other properties are set.
     */
    public <V> EntityBuilder<T> derive(PropertyReference<T, V> property, Function<T, V> derivation) {
        getDerivations().put(property.getPropertyName(), derivation);
        return this;
    }

    /**
     * Explicitly sets a property to null, overriding any auto-generation.
     */
    public <V> EntityBuilder<T> setNull(PropertyReference<T, V> property) {
        getExplicitNulls().add(property.getPropertyName());
        return this;
    }

    /**
     * Creates a single entity instance with all configured values applied.
     */
    public T create() {
        return create(new BuildContext(config.getMaxDepth()), new GeneratorContext());
    }

    /**
     * Creates multiple entity instances.
     */
    public List<T> createList(int count) {
        List<T> results = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            BuildContext itemContext = new BuildContext(config.getMaxDepth());
            results.add(create(itemContext, new GeneratorContext(i)));
        }

        return results;
    }

    /**
     * Internal create method with context.
     */
    T create(BuildContext buildContext, GeneratorContext generatorContext) {
        Class<T> actualClass = resolveConcreteType();

        if (buildContext.isMaxDepthExceeded()) {
            throw new MaxDepthExceededException(buildContext.getMaxDepth(), actualClass);
        }

        if (buildContext.isBuilding(actualClass)) {
            T known = buildContext.getKnownInstance(actualClass);
            if (known != null) {
                return known;
            }
            throw new CircularDependencyException(actualClass, "<self>", actualClass);
        }

        try {
            buildContext.startBuilding(actualClass);

            T instance = createInstance(actualClass);
            buildContext.registerInstance(instance);

            EntityMetadata metadata = analyzeEntity(actualClass);

            populateProperties(instance, metadata, buildContext, generatorContext);
            applyDerivedValues(instance, metadata);

            return instance;

        } finally {
            buildContext.finishBuilding(actualClass);
        }
    }

    /**
     * Analyzes the entity using the configured metadata provider.
     */
    private EntityMetadata analyzeEntity(Class<?> entityClass) {
        MetadataProvider provider = config.findProvider(entityClass);
        if (provider == null) {
            throw new IllegalStateException(
                    "No MetadataProvider found for " + entityClass.getName() +
                    ". Make sure to configure a provider using TestData.configure(c -> " +
                    "c.metadataProvider(new JpaMetadataProvider())) or use the Spring Boot starter."
            );
        }
        return provider.analyze(entityClass);
    }

    @SuppressWarnings("unchecked")
    private Class<T> resolveConcreteType() {
        if (!Modifier.isAbstract(entityClass.getModifiers()) && !entityClass.isInterface()) {
            return entityClass;
        }

        Class<?> concreteType = config.getConcreteType(entityClass);
        if (concreteType != null) {
            return (Class<T>) concreteType;
        }

        throw new IllegalArgumentException(
                "Cannot instantiate abstract class or interface: " + entityClass.getName() +
                ". Configure a concrete type using TestData.configure(c -> c.abstractDefault(" +
                entityClass.getSimpleName() + ".class, ConcreteClass.class))"
        );
    }

    private T createInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Entity " + clazz.getName() + " must have a no-argument constructor", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    private void populateProperties(T instance, EntityMetadata metadata,
                                    BuildContext buildContext, GeneratorContext generatorContext) {
        for (PropertyMetadata property : metadata.getProperties().values()) {
            String propName = property.getName();

            if (explicitNulls != null && explicitNulls.contains(propName)) {
                continue;
            }

            Object value = getOverrideValue(propName, instance, generatorContext);

            // Check again after getOverrideValue, as it may have added to explicitNulls
            if (explicitNulls != null && explicitNulls.contains(propName)) {
                continue;
            }

            if (value == null && shouldGenerateValue(property)) {
                value = generateValue(property, instance, buildContext, generatorContext);
            }

            if (value != null) {
                setFieldValue(instance, property, value);
            }
        }
    }

    private boolean shouldGenerateValue(PropertyMetadata property) {
        if (property.isRequired()) {
            return true;
        }

        if (property.isAssociation()
            && property.getAssociationType() == PropertyMetadata.AssociationType.EMBEDDED) {
            return true;
        }

        return false;
    }

    private Object getOverrideValue(String propName, T instance, GeneratorContext context) {
        if (overrides != null && overrides.containsKey(propName)) {
            return overrides.get(propName);
        }

        if (suppliers != null && suppliers.containsKey(propName)) {
            return suppliers.get(propName).get();
        }

        if (sequences != null && sequences.containsKey(propName)) {
            return sequences.get(propName).apply(context.getSequenceIndex());
        }

        TestDataConfig.TypeDefaults<T> defaults = config.getTypeDefaults(entityClass);
        if (defaults != null) {
            if (defaults.isExplicitlyNull(propName)) {
                getExplicitNulls().add(propName);
                return null;
            }
            if (defaults.hasSequence(propName)) {
                return defaults.getSequenceValue(propName, context.getSequenceIndex());
            }
            if (defaults.hasSupplier(propName)) {
                return defaults.getSuppliedValue(propName);
            }
            if (defaults.hasFixedValue(propName)) {
                return defaults.getFixedValue(propName);
            }
        }

        return null;
    }

    private Object generateValue(PropertyMetadata property, T instance,
                                 BuildContext buildContext, GeneratorContext generatorContext) {
        if (property.isAssociation()) {
            return generateAssociationValue(property, instance, buildContext, generatorContext);
        }

        for (ValueGenerator generator : generators) {
            if (generator.supports(property)) {
                return generator.generate(property, generatorContext);
            }
        }

        return null;
    }

    private Object generateAssociationValue(PropertyMetadata property, T instance,
                                            BuildContext buildContext, GeneratorContext generatorContext) {
        Class<?> associatedType = property.getAssociatedEntityType();

        if (associatedType == null) {
            return null;
        }

        switch (property.getAssociationType()) {
            case EMBEDDED:
                return buildEmbeddedObject(associatedType, buildContext, generatorContext);

            case ONE_TO_ONE:
            case MANY_TO_ONE:
                return buildNestedEntity(associatedType, property, instance, buildContext, generatorContext);

            case ONE_TO_MANY:
            case MANY_TO_MANY:
                Integer minSize = property.getCollectionMinSize();
                if (minSize != null && minSize > 0) {
                    return buildCollection(property, minSize, buildContext, generatorContext);
                }
                return createEmptyCollection(property.getType());

            default:
                return null;
        }
    }

    private Object buildEmbeddedObject(Class<?> embeddableType, BuildContext buildContext,
                                       GeneratorContext generatorContext) {
        try {
            Object instance = embeddableType.getDeclaredConstructor().newInstance();
            EntityMetadata metadata = analyzeEntity(embeddableType);

            for (PropertyMetadata property : metadata.getProperties().values()) {
                if (shouldGenerateEmbeddedField(property)) {
                    Object value = null;

                    if (property.isAssociation()) {
                        if (property.getAssociationType() == PropertyMetadata.AssociationType.EMBEDDED) {
                            value = buildEmbeddedObject(property.getAssociatedEntityType(),
                                                       buildContext, generatorContext);
                        }
                    } else {
                        for (ValueGenerator generator : generators) {
                            if (generator.supports(property)) {
                                value = generator.generate(property, generatorContext);
                                break;
                            }
                        }
                    }

                    if (value != null) {
                        setFieldValue(instance, property, value);
                    }
                }
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create embedded object of type "
                    + embeddableType.getName(), e);
        }
    }

    private boolean shouldGenerateEmbeddedField(PropertyMetadata property) {
        if (property.isRequired() || !property.isNullable()) {
            return true;
        }

        if (property.getPattern() != null
            || property.isEmail()
            || property.getMinLength() != null
            || property.getMaxLength() != null
            || property.getMinValue() != null
            || property.getMaxValue() != null) {
            return true;
        }

        Class<?> type = property.getType();
        if (type.isPrimitive()) {
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private Object buildNestedEntity(Class<?> entityType, PropertyMetadata property, T parentInstance,
                                     BuildContext buildContext, GeneratorContext generatorContext) {
        if (buildContext.isSelfReference(entityType, parentInstance)) {
            return null;
        }

        if (buildContext.isBuilding(entityType)) {
            throw new CircularDependencyException(
                    parentInstance.getClass(),
                    property.getName(),
                    entityType
            );
        }

        if (buildContext.hasKnownInstance(entityType) && !buildContext.isInBuildStack(entityType)) {
            return buildContext.getKnownInstance(entityType);
        }

        EntityBuilder<?> nestedBuilder = new EntityBuilder<>(entityType, config);
        return nestedBuilder.create(buildContext, generatorContext);
    }

    private Collection<?> buildCollection(PropertyMetadata property, int size,
                                          BuildContext buildContext, GeneratorContext generatorContext) {
        Class<?> elementType = property.getAssociatedEntityType();
        Collection<Object> collection = createEmptyCollection(property.getType());

        for (int i = 0; i < size; i++) {
            EntityBuilder<?> elementBuilder = new EntityBuilder<>(elementType, config);
            BuildContext childContext = buildContext.createChildContext();
            Object element = elementBuilder.create(childContext, new GeneratorContext(i));
            collection.add(element);
        }

        return collection;
    }

    @SuppressWarnings("unchecked")
    private <C extends Collection<Object>> C createEmptyCollection(Class<?> collectionType) {
        if (List.class.isAssignableFrom(collectionType)) {
            return (C) new ArrayList<>();
        }
        if (Set.class.isAssignableFrom(collectionType)) {
            return (C) new HashSet<>();
        }
        return (C) new ArrayList<>();
    }

    private void applyDerivedValues(T instance, EntityMetadata metadata) {
        if (derivations != null) {
            for (Map.Entry<String, Function<T, ?>> entry : derivations.entrySet()) {
                String propName = entry.getKey();
                if (hasOverride(propName) || hasSupplier(propName) || hasSequence(propName)) {
                    continue;
                }
                PropertyMetadata prop = metadata.getProperty(propName);
                if (prop != null) {
                    Object value = entry.getValue().apply(instance);
                    setFieldValue(instance, prop, value);
                }
            }
        }

        TestDataConfig.TypeDefaults<T> defaults = config.getTypeDefaults(entityClass);
        if (defaults != null) {
            for (Map.Entry<String, Function<T, ?>> entry : defaults.getDerivedValues().entrySet()) {
                String propName = entry.getKey();
                if (hasDerivation(propName) || hasOverride(propName)
                        || hasSupplier(propName) || hasSequence(propName)) {
                    continue;
                }
                PropertyMetadata prop = metadata.getProperty(propName);
                if (prop != null) {
                    Object value = entry.getValue().apply(instance);
                    setFieldValue(instance, prop, value);
                }
            }
        }
    }

    // Null-safe helper methods for checking collection contents
    private boolean hasOverride(String propName) {
        return overrides != null && overrides.containsKey(propName);
    }

    private boolean hasSupplier(String propName) {
        return suppliers != null && suppliers.containsKey(propName);
    }

    private boolean hasSequence(String propName) {
        return sequences != null && sequences.containsKey(propName);
    }

    private boolean hasDerivation(String propName) {
        return derivations != null && derivations.containsKey(propName);
    }

    private void setFieldValue(Object instance, PropertyMetadata property, Object value) {
        try {
            Field field = property.getAccessibleField();
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field " + property.getName(), e);
        }
    }
}
