package io.github.ramils.core;

import io.github.ramils.builder.EntityBuilder;
import io.github.ramils.config.TestDataConfig;
import io.github.ramils.metadata.MetadataProvider;

import java.util.function.Consumer;

/**
 * Main entry point for creating test data.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Simple creation with auto-filled required fields
 * User user = TestData.create(User.class);
 *
 * // With customizations (type-safe via method references)
 * User user = TestData.of(User.class)
 *     .set(User::getEmail, "custom@test.com")
 *     .set(User::getAge, 25)
 *     .create();
 *
 * // Create multiple with unique values
 * List<User> users = TestData.of(User.class)
 *     .sequence(User::getEmail, i -> "user" + i + "@test.com")
 *     .createList(10);
 *
 * // Derive one field from another
 * User user = TestData.of(User.class)
 *     .set(User::getFirstName, "John")
 *     .set(User::getLastName, "Doe")
 *     .derive(User::getEmail, u -> u.getFirstName().toLowerCase() + "."
 *         + u.getLastName().toLowerCase() + "@company.com")
 *     .create();
 * // user.getEmail() -> "john.doe@company.com"
 *
 * // Set values from Map (useful for dynamic/external data)
 * User user = TestData.of(User.class)
 *     .setAll(Map.of("firstName", "John", "lastName", "Doe"))
 *     .create();
 * }</pre>
 */
public final class TestData {

    private static final TestDataConfig GLOBAL_CONFIG = new TestDataConfig();

    private TestData() {
        // Utility class
    }

    /**
     * Creates a single instance with all required fields auto-populated.
     *
     * <pre>{@code
     * User user = TestData.create(User.class);
     * // user.getEmail() -> auto-generated valid email
     * // user.getFirstName() -> auto-generated string
     * }</pre>
     *
     * @param entityClass the entity class to instantiate
     * @param <T> the entity type
     * @return a new instance with required fields populated
     */
    public static <T> T create(Class<T> entityClass) {
        return of(entityClass).create();
    }

    /**
     * Creates a builder for more control over the entity creation.
     *
     * <pre>{@code
     * User user = TestData.of(User.class)
     *     .set(User::getFirstName, "John")
     *     .set(User::getEmail, "john@example.com")
     *     .create();
     * }</pre>
     *
     * @param entityClass the entity class to build
     * @param <T> the entity type
     * @return a new EntityBuilder for the given class
     */
    public static <T> EntityBuilder<T> of(Class<T> entityClass) {
        return new EntityBuilder<>(entityClass, GLOBAL_CONFIG);
    }

    /**
     * Configures global defaults for entity creation.
     * <p>
     * Configuration is typically set up once in a base test class or {@code @BeforeAll} method.
     * Builder-level settings (via {@link #of(Class)}) take priority over global configuration.
     *
     * <pre>{@code
     * TestData.configure(config -> config
     *     // Configure defaults for a specific type
     *     .forType(User.class, defaults -> defaults
     *         .set(User::getRole, Role.USER)                      // Fixed value
     *         .generate(User::getCreatedAt, () -> Instant.now())  // Dynamic (Supplier)
     *         .sequence(User::getEmail, i -> "user" + i + "@test.com")  // Index-based
     *         .derive(User::getFullName, u ->                     // Computed from other fields
     *             u.getFirstName() + " " + u.getLastName())
     *         .setNull(User::getDeletedAt))                       // Explicitly null
     *
     *     // Abstract type resolution
     *     .abstractDefault(Payment.class, CreditCardPayment.class)
     *
     *     // Max depth for nested entities
     *     .maxDepth(5)
     * );
     *
     * // All created Users now use these defaults
     * User user = TestData.create(User.class);
     *
     * // Builder overrides take priority
     * User custom = TestData.of(User.class)
     *     .set(User::getRole, Role.ADMIN)  // Overrides global config
     *     .create();
     * }</pre>
     *
     * @param configurer a consumer that modifies the global configuration
     * @see TestDataConfig
     * @see TestDataConfig.TypeDefaults
     */
    public static void configure(Consumer<TestDataConfig> configurer) {
        configurer.accept(GLOBAL_CONFIG);
    }

    /**
     * Registers a metadata provider.
     * Convenience method equivalent to configure(c -> c.metadataProvider(provider)).
     *
     * @param provider the metadata provider to register
     */
    public static void registerProvider(MetadataProvider provider) {
        GLOBAL_CONFIG.metadataProvider(provider);
    }

    /**
     * Resets all configuration to defaults.
     * Useful in {@code @BeforeEach} to ensure test isolation.
     *
     * <pre>{@code
     * @BeforeEach
     * void setUp() {
     *     TestData.reset();
     * }
     * }</pre>
     */
    public static void reset() {
        GLOBAL_CONFIG.reset();
    }

    /**
     * Returns the global configuration for inspection or direct modification.
     *
     * @return the global TestDataConfig instance
     */
    public static TestDataConfig getConfig() {
        return GLOBAL_CONFIG;
    }
}
