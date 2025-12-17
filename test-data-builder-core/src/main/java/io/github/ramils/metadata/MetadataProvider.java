package io.github.ramils.metadata;

/**
 * Interface for analyzing entity classes and extracting metadata.
 * <p>
 * Different implementations handle different persistence technologies:
 * <ul>
 *   <li>JPA - analyzes @Entity, @Column, @ManyToOne, etc.</li>
 *   <li>Spring Data JDBC - analyzes @Table, @Column, @MappedCollection, etc.</li>
 * </ul>
 *
 * <pre>{@code
 * // Implementations are auto-detected via Spring Boot auto-configuration
 * // or can be set manually:
 * TestData.configure(config -> config.metadataProvider(new JpaMetadataProvider()));
 * }</pre>
 */
public interface MetadataProvider {

    /**
     * Analyzes an entity class and returns its metadata.
     * Results should be cached for performance.
     *
     * @param entityClass the class to analyze
     * @return metadata about the entity's properties and constraints
     */
    EntityMetadata analyze(Class<?> entityClass);

    /**
     * Checks if this provider can handle the given class.
     *
     * @param entityClass the class to check
     * @return true if this provider supports the class
     */
    boolean supports(Class<?> entityClass);

    /**
     * Clears any cached metadata.
     */
    void clearCache();

    /**
     * Returns the priority of this provider.
     * Higher priority providers are preferred when multiple providers
     * support the same class.
     *
     * @return priority value (higher = more preferred)
     */
    default int getPriority() {
        return 0;
    }
}
