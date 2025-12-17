package io.github.ramils.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Metadata about an entity, including all its properties and their constraints.
 */
public class EntityMetadata {

    private final Class<?> entityClass;
    private final Map<String, PropertyMetadata> properties;
    private final boolean abstractClass;
    private final Class<?> superclass;

    public EntityMetadata(Class<?> entityClass, Map<String, PropertyMetadata> properties,
                          boolean abstractClass, Class<?> superclass) {
        this.entityClass = entityClass;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        this.abstractClass = abstractClass;
        this.superclass = superclass;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public Map<String, PropertyMetadata> getProperties() {
        return properties;
    }

    public PropertyMetadata getProperty(String name) {
        return properties.get(name);
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public boolean isAbstractClass() {
        return abstractClass;
    }

    public Class<?> getSuperclass() {
        return superclass;
    }

    /**
     * Returns all required properties (non-nullable and non-optional).
     */
    public List<PropertyMetadata> getRequiredProperties() {
        return properties.values().stream()
                .filter(PropertyMetadata::isRequired)
                .collect(Collectors.toList());
    }

    /**
     * Returns all association properties.
     */
    public List<PropertyMetadata> getAssociations() {
        return properties.values().stream()
                .filter(PropertyMetadata::isAssociation)
                .collect(Collectors.toList());
    }

    /**
     * Returns all required association properties.
     */
    public List<PropertyMetadata> getRequiredAssociations() {
        return properties.values().stream()
                .filter(p -> p.isAssociation() && p.isRequired())
                .collect(Collectors.toList());
    }

    /**
     * Returns all simple (non-association) properties.
     */
    public List<PropertyMetadata> getSimpleProperties() {
        return properties.values().stream()
                .filter(p -> !p.isAssociation())
                .collect(Collectors.toList());
    }

    /**
     * Returns property names.
     */
    public Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String toString() {
        return "EntityMetadata{" +
                "entityClass=" + entityClass.getSimpleName() +
                ", properties=" + properties.keySet() +
                ", abstract=" + abstractClass +
                '}';
    }
}
