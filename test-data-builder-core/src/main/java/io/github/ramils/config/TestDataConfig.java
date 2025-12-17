package io.github.ramils.config;

import io.github.ramils.metadata.MetadataProvider;
import io.github.ramils.util.PropertyReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Configuration for test data generation.
 * <p>
 * Allows setting default values, sequences, and derived values for entity properties.
 * Configuration is typically set up once in a base test class or {@code @BeforeAll} method.
 * <p>
 * Example:
 * <pre>{@code
 * TestData.configure(config -> config
 *     .forType(User.class, defaults -> defaults
 *         .set(User::getFirstName, "Test")           // Fixed value
 *         .set(User::getLastName, "User")
 *         .sequence(User::getEmail, i -> "user" + i + "@test.com")  // Sequential
 *         .derive(User::getFullName, u ->            // Computed from other fields
 *             u.getFirstName() + " " + u.getLastName()))
 *     .forType(Order.class, defaults -> defaults
 *         .set(Order::getStatus, OrderStatus.PENDING)
 *         .generate(Order::getCreatedAt, () -> Instant.now()))  // Supplier
 *     .abstractDefault(Payment.class, CreditCardPayment.class)  // Concrete impl
 *     .maxDepth(5)
 * );
 *
 * // All Users created will now have these defaults:
 * User user = TestData.create(User.class);
 * // user.getFirstName() -> "Test"
 * // user.getEmail() -> "user0@test.com"
 *
 * // Builder overrides take priority over global config:
 * User custom = TestData.of(User.class)
 *     .set(User::getFirstName, "Custom")
 *     .create();
 * // custom.getFirstName() -> "Custom"
 * }</pre>
 *
 * @see io.github.ramils.core.TestData#configure(java.util.function.Consumer)
 */
public class TestDataConfig {

    private final Map<Class<?>, TypeDefaults<?>> typeDefaults = new ConcurrentHashMap<>();
    private final Map<Class<?>, Class<?>> abstractDefaults = new ConcurrentHashMap<>();
    private final List<MetadataProvider> metadataProviders = new ArrayList<>();
    private int maxDepth = 10;

    /**
     * Registers a metadata provider.
     * Providers are tried in order of priority (highest first).
     */
    public TestDataConfig metadataProvider(MetadataProvider provider) {
        metadataProviders.add(provider);
        metadataProviders.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return this;
    }

    /**
     * Returns all registered metadata providers.
     */
    public List<MetadataProvider> getMetadataProviders() {
        return metadataProviders;
    }

    /**
     * Finds a suitable metadata provider for the given class.
     */
    public MetadataProvider findProvider(Class<?> entityClass) {
        for (MetadataProvider provider : metadataProviders) {
            if (provider.supports(entityClass)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Configures defaults for a specific entity type.
     */
    public <T> TestDataConfig forType(Class<T> type, java.util.function.Consumer<TypeDefaults<T>> configurer) {
        @SuppressWarnings("unchecked")
        TypeDefaults<T> defaults = (TypeDefaults<T>) typeDefaults.computeIfAbsent(type,
                k -> new TypeDefaults<>());
        configurer.accept(defaults);
        return this;
    }

    /**
     * Sets the default concrete implementation for an abstract class or interface.
     */
    public TestDataConfig abstractDefault(Class<?> abstractType, Class<?> concreteType) {
        if (abstractType.equals(concreteType)) {
            throw new IllegalArgumentException("Abstract default must be different from the abstract type");
        }
        if (!abstractType.isAssignableFrom(concreteType)) {
            throw new IllegalArgumentException(
                    concreteType.getName() + " is not a subtype of " + abstractType.getName()
            );
        }
        abstractDefaults.put(abstractType, concreteType);
        return this;
    }

    /**
     * Sets the maximum depth for nested entity creation.
     */
    public TestDataConfig maxDepth(int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("Max depth must be at least 1");
        }
        this.maxDepth = depth;
        return this;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    @SuppressWarnings("unchecked")
    public <T> TypeDefaults<T> getTypeDefaults(Class<T> type) {
        return (TypeDefaults<T>) typeDefaults.get(type);
    }

    public Class<?> getConcreteType(Class<?> abstractType) {
        return abstractDefaults.get(abstractType);
    }

    public boolean hasAbstractDefault(Class<?> type) {
        return abstractDefaults.containsKey(type);
    }

    /**
     * Resets all configuration to defaults.
     */
    public void reset() {
        typeDefaults.clear();
        abstractDefaults.clear();
        maxDepth = 10;
        // Note: metadataProviders are not cleared - they're typically set once at startup
    }

    /**
     * Clears all metadata provider caches.
     */
    public void clearMetadataCache() {
        for (MetadataProvider provider : metadataProviders) {
            provider.clearCache();
        }
    }

    // Shorthand methods for common operations

    /**
     * Sets a default fixed value for a property.
     */
    public <T, V> TestDataConfig defaultValue(Class<T> type, PropertyReference<T, V> property, V value) {
        return forType(type, defaults -> defaults.set(property, value));
    }

    /**
     * Sets a default supplier for a property.
     */
    public <T, V> TestDataConfig defaultGenerator(Class<T> type,
                                                   PropertyReference<T, V> property,
                                                   Supplier<V> supplier) {
        return forType(type, defaults -> defaults.generate(property, supplier));
    }

    /**
     * Sets a default sequence generator for a property.
     */
    public <T, V> TestDataConfig defaultSequence(Class<T> type,
                                                  PropertyReference<T, V> property,
                                                  IntFunction<V> generator) {
        return forType(type, defaults -> defaults.sequence(property, generator));
    }

    /**
     * Sets a default derived value for a property.
     */
    public <T, V> TestDataConfig defaultDerived(Class<T> type,
                                                 PropertyReference<T, V> property,
                                                 Function<T, V> derivation) {
        return forType(type, defaults -> defaults.derive(property, derivation));
    }

    /**
     * Holds default value configurations for a specific type.
     * <p>
     * Example:
     * <pre>{@code
     * TestData.configure(config -> config
     *     .forType(User.class, defaults -> defaults
     *         .set(User::getRole, Role.USER)                    // Fixed value
     *         .generate(User::getCreatedAt, () -> Instant.now()) // Dynamic (Supplier)
     *         .sequence(User::getEmail, i -> "user" + i + "@test.com")
     *         .derive(User::getDisplayName, u -> u.getFirstName() + " " + u.getLastName())
     *         .setNull(User::getDeletedAt)));
     * }</pre>
     */
    public static class TypeDefaults<T> {
        private final Map<String, Object> fixedValues = new HashMap<>();
        private final Map<String, Supplier<?>> suppliers = new HashMap<>();
        private final Map<String, IntFunction<?>> sequences = new HashMap<>();
        private final Map<String, Function<T, ?>> derivedValues = new HashMap<>();
        private final Map<String, Boolean> nullableOverrides = new HashMap<>();

        /**
         * Sets a fixed value for a property. The same value is used for all instances.
         *
         * <pre>{@code
         * .set(User::getRole, Role.USER)
         * }</pre>
         *
         * @param property method reference to the getter
         * @param value the fixed value to use
         * @return this for chaining
         */
        public <V> TypeDefaults<T> set(PropertyReference<T, V> property, V value) {
            fixedValues.put(property.getPropertyName(), value);
            return this;
        }

        /**
         * Sets a supplier for a property. The supplier is called each time an entity is created,
         * allowing for dynamic values like timestamps or UUIDs.
         *
         * <pre>{@code
         * .generate(User::getCreatedAt, () -> Instant.now())
         * .generate(User::getId, () -> UUID.randomUUID())
         * }</pre>
         *
         * @param property method reference to the getter
         * @param supplier function that provides a new value each time
         * @return this for chaining
         */
        public <V> TypeDefaults<T> generate(PropertyReference<T, V> property, Supplier<V> supplier) {
            suppliers.put(property.getPropertyName(), supplier);
            return this;
        }

        /**
         * Sets a sequence generator for a property. Useful for creating unique values
         * when generating multiple entities with {@code createList()}.
         * The function receives the current index (0, 1, 2, ...).
         *
         * <pre>{@code
         * .sequence(User::getEmail, i -> "user" + i + "@test.com")
         * // Creates: user0@test.com, user1@test.com, user2@test.com...
         * }</pre>
         *
         * @param property method reference to the getter
         * @param generator function that takes index and returns value
         * @return this for chaining
         */
        public <V> TypeDefaults<T> sequence(PropertyReference<T, V> property, IntFunction<V> generator) {
            sequences.put(property.getPropertyName(), generator);
            return this;
        }

        /**
         * Sets a derived value computed from the entity instance after other properties are set.
         * Useful for computed fields that depend on other values.
         *
         * <pre>{@code
         * .set(User::getFirstName, "John")
         * .set(User::getLastName, "Doe")
         * .derive(User::getEmail, u ->
         *     u.getFirstName().toLowerCase() + "." +
         *     u.getLastName().toLowerCase() + "@company.com")
         * // Creates email: john.doe@company.com
         * }</pre>
         *
         * @param property method reference to the getter
         * @param derivation function that computes value from the entity
         * @return this for chaining
         */
        public <V> TypeDefaults<T> derive(PropertyReference<T, V> property, Function<T, V> derivation) {
            derivedValues.put(property.getPropertyName(), derivation);
            return this;
        }

        /**
         * Marks a property to be explicitly set to null, preventing auto-generation.
         *
         * <pre>{@code
         * .setNull(User::getDeletedAt)
         * }</pre>
         *
         * @param property method reference to the getter
         * @return this for chaining
         */
        public <V> TypeDefaults<T> setNull(PropertyReference<T, V> property) {
            nullableOverrides.put(property.getPropertyName(), true);
            return this;
        }

        public boolean hasFixedValue(String propertyName) {
            return fixedValues.containsKey(propertyName);
        }

        public Object getFixedValue(String propertyName) {
            return fixedValues.get(propertyName);
        }

        public boolean hasSupplier(String propertyName) {
            return suppliers.containsKey(propertyName);
        }

        @SuppressWarnings("unchecked")
        public <V> V getSuppliedValue(String propertyName) {
            Supplier<?> supplier = suppliers.get(propertyName);
            return supplier != null ? (V) supplier.get() : null;
        }

        public boolean hasSequence(String propertyName) {
            return sequences.containsKey(propertyName);
        }

        @SuppressWarnings("unchecked")
        public <V> V getSequenceValue(String propertyName, int index) {
            IntFunction<?> sequence = sequences.get(propertyName);
            return sequence != null ? (V) sequence.apply(index) : null;
        }

        public boolean hasDerivedValue(String propertyName) {
            return derivedValues.containsKey(propertyName);
        }

        @SuppressWarnings("unchecked")
        public <V> V getDerivedValue(String propertyName, T instance) {
            Function<T, ?> derivation = derivedValues.get(propertyName);
            return derivation != null ? (V) derivation.apply(instance) : null;
        }

        public boolean isExplicitlyNull(String propertyName) {
            return nullableOverrides.getOrDefault(propertyName, false);
        }

        public Map<String, Function<T, ?>> getDerivedValues() {
            return derivedValues;
        }
    }
}
