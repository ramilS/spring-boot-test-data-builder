package io.github.ramils.core;

/**
 * Thrown when the maximum depth for nested entity creation is exceeded.
 */
public class MaxDepthExceededException extends RuntimeException {

    private final int maxDepth;
    private final Class<?> entityType;

    public MaxDepthExceededException(int maxDepth, Class<?> entityType) {
        super(String.format(
                "Maximum depth of %d exceeded while building %s.%n" +
                "This usually indicates a deep or circular entity graph.%n" +
                "Hint: Configure a higher maxDepth or provide explicit values for some associations.",
                maxDepth, entityType.getSimpleName()
        ));
        this.maxDepth = maxDepth;
        this.entityType = entityType;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public Class<?> getEntityType() {
        return entityType;
    }
}
