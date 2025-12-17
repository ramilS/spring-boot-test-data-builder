package io.github.ramils.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Context object passed during entity building.
 * Tracks known instances to prevent infinite recursion in circular dependencies.
 * <p>
 * Key mechanism (from Grails build-test-data):
 * - knownInstances: cache of already created objects by type
 * - buildingNow: set of types currently being built (for cycle detection)
 */
public class BuildContext {

    /**
     * Cache of already created instances by their class.
     * Used to reuse instances when the same type is needed multiple times.
     */
    private final Map<Class<?>, Object> knownInstances = new HashMap<>();

    /**
     * Set of classes currently being built.
     * Used to detect circular dependencies.
     */
    private final Set<Class<?>> buildingNow = new HashSet<>();

    /**
     * Current depth of nested entity creation.
     */
    private int currentDepth = 0;

    /**
     * Maximum allowed depth for nested entity creation.
     */
    private final int maxDepth;

    /**
     * Index for sequence generation (for createList).
     */
    private int sequenceIndex = 0;

    public BuildContext() {
        this(10); // Default max depth
    }

    public BuildContext(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * Checks if we're currently building the given type (cycle detection).
     */
    public boolean isBuilding(Class<?> type) {
        return buildingNow.contains(type);
    }

    /**
     * Checks if the given type (or any of its supertypes) is in the current build stack.
     * This is used to detect if we're currently in the middle of building a chain
     * that includes this type.
     */
    public boolean isInBuildStack(Class<?> type) {
        for (Class<?> building : buildingNow) {
            if (type.isAssignableFrom(building) || building.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Marks a type as currently being built.
     */
    public void startBuilding(Class<?> type) {
        buildingNow.add(type);
        currentDepth++;
    }

    /**
     * Marks a type as finished building.
     */
    public void finishBuilding(Class<?> type) {
        buildingNow.remove(type);
        currentDepth--;
    }

    /**
     * Checks if we have a known instance of the given type (or subtype).
     * Optimized to avoid stream operations for better performance.
     */
    public boolean hasKnownInstance(Class<?> type) {
        // First check for exact match (most common case)
        if (knownInstances.containsKey(type)) {
            return true;
        }
        // Fall back to subtype check
        for (Class<?> key : knownInstances.keySet()) {
            if (type.isAssignableFrom(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a known instance of the given type (or subtype).
     * Optimized to avoid stream operations for better performance.
     */
    @SuppressWarnings("unchecked")
    public <T> T getKnownInstance(Class<T> type) {
        // First check for exact match (most common case)
        Object exactMatch = knownInstances.get(type);
        if (exactMatch != null) {
            return (T) exactMatch;
        }
        // Fall back to subtype check
        for (Map.Entry<Class<?>, Object> entry : knownInstances.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return (T) entry.getValue();
            }
        }
        return null;
    }

    /**
     * Registers a known instance.
     */
    public void registerInstance(Object instance) {
        knownInstances.put(instance.getClass(), instance);
    }

    /**
     * Removes a known instance.
     */
    public void unregisterInstance(Class<?> type) {
        knownInstances.remove(type);
    }

    /**
     * Checks if the given type is assignable from the currently building types.
     * Used for self-reference detection.
     */
    public boolean isSelfReference(Class<?> type, Object currentInstance) {
        return type.isAssignableFrom(currentInstance.getClass());
    }

    /**
     * Checks if max depth is exceeded.
     */
    public boolean isMaxDepthExceeded() {
        return currentDepth > maxDepth;
    }

    public int getCurrentDepth() {
        return currentDepth;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getSequenceIndex() {
        return sequenceIndex;
    }

    public int nextSequenceIndex() {
        return sequenceIndex++;
    }

    public void resetSequenceIndex() {
        sequenceIndex = 0;
    }

    /**
     * Creates a child context for nested entity building.
     * Shares knownInstances but has independent sequence index.
     */
    public BuildContext createChildContext() {
        BuildContext child = new BuildContext(maxDepth);
        child.knownInstances.putAll(this.knownInstances);
        child.buildingNow.addAll(this.buildingNow);
        child.sequenceIndex = 0; // Reset for nested collections
        return child;
    }
}
