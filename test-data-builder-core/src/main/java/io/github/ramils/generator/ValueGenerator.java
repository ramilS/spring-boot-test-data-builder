package io.github.ramils.generator;

import io.github.ramils.metadata.PropertyMetadata;

/**
 * Generates values for entity properties based on their type and constraints.
 */
public interface ValueGenerator {

    /**
     * Checks if this generator can handle the given property.
     */
    boolean supports(PropertyMetadata property);

    /**
     * Generates a value for the property.
     *
     * @param property the property metadata
     * @param context  generation context (for sequences, uniqueness, etc.)
     * @return the generated value
     */
    Object generate(PropertyMetadata property, GeneratorContext context);

    /**
     * Returns the priority of this generator.
     * Higher priority generators are tried first.
     */
    default int getPriority() {
        return 0;
    }
}
