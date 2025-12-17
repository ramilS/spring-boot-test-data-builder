package io.github.ramils.generator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Context for value generation, providing sequence numbers and uniqueness tracking.
 */
public class GeneratorContext {

    /**
     * Global counters for unique value generation, keyed by field identifier.
     */
    private static final Map<String, AtomicInteger> GLOBAL_COUNTERS = new ConcurrentHashMap<>();

    /**
     * Current sequence index (for list generation).
     */
    private final int sequenceIndex;

    public GeneratorContext() {
        this(0);
    }

    public GeneratorContext(int sequenceIndex) {
        this.sequenceIndex = sequenceIndex;
    }

    /**
     * Returns the current sequence index.
     */
    public int getSequenceIndex() {
        return sequenceIndex;
    }

    /**
     * Gets the next unique counter value for a field.
     * Used to generate unique values for @Column(unique=true) fields.
     *
     * @param fieldKey unique identifier for the field (e.g., "User.email")
     * @return the next counter value
     */
    public int nextUniqueCounter(String fieldKey) {
        return GLOBAL_COUNTERS
                .computeIfAbsent(fieldKey, k -> new AtomicInteger(0))
                .getAndIncrement();
    }

    /**
     * Gets the current counter value without incrementing.
     */
    public int getCurrentCounter(String fieldKey) {
        AtomicInteger counter = GLOBAL_COUNTERS.get(fieldKey);
        return counter != null ? counter.get() : 0;
    }

    /**
     * Resets all counters. Useful for test isolation.
     */
    public static void resetCounters() {
        GLOBAL_COUNTERS.clear();
    }

    /**
     * Resets a specific counter.
     */
    public static void resetCounter(String fieldKey) {
        GLOBAL_COUNTERS.remove(fieldKey);
    }

    /**
     * Creates a new context for the next item in a sequence.
     */
    public GeneratorContext forNextInSequence() {
        return new GeneratorContext(sequenceIndex + 1);
    }
}
