package io.github.ramils.jdbc;

import io.github.ramils.metadata.EntityMetadata;
import io.github.ramils.metadata.PropertyMetadata;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcMetadataProviderTest {

    private JdbcMetadataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JdbcMetadataProvider();
        provider.clearCache();
    }

    @Nested
    @DisplayName("supports()")
    class SupportsTests {

        @Test
        @DisplayName("Should support @Table classes")
        void shouldSupportTableClasses() {
            assertThat(provider.supports(TableEntity.class)).isTrue();
        }

        @Test
        @DisplayName("Should support classes with @Id field")
        void shouldSupportClassesWithId() {
            assertThat(provider.supports(SimpleEntity.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("@Id analysis")
    class IdTests {

        @Test
        @DisplayName("Should detect @Id field")
        void shouldDetectId() {
            EntityMetadata metadata = provider.analyze(SimpleEntity.class);
            PropertyMetadata prop = metadata.getProperty("id");

            assertThat(prop).isNotNull();
            assertThat(prop.isNullable()).isFalse();
        }

        @Test
        @DisplayName("@Id should not be required (auto-generated in JDBC)")
        void idShouldNotBeRequired() {
            EntityMetadata metadata = provider.analyze(SimpleEntity.class);
            PropertyMetadata prop = metadata.getProperty("id");

            // In Spring Data JDBC, new entities have null ID
            assertThat(prop.isRequired()).isFalse();
        }
    }

    @Nested
    @DisplayName("@Embedded analysis")
    class EmbeddedTests {

        @Test
        @DisplayName("Should detect @Embedded fields")
        void shouldDetectEmbedded() {
            EntityMetadata metadata = provider.analyze(EntityWithEmbedded.class);
            PropertyMetadata prop = metadata.getProperty("address");

            assertThat(prop.isAssociation()).isTrue();
            assertThat(prop.getAssociationType()).isEqualTo(PropertyMetadata.AssociationType.EMBEDDED);
            assertThat(prop.isEmbedded()).isTrue();
        }
    }

    @Nested
    @DisplayName("@MappedCollection analysis")
    class MappedCollectionTests {

        @Test
        @DisplayName("Should detect @MappedCollection")
        void shouldDetectMappedCollection() {
            EntityMetadata metadata = provider.analyze(EntityWithCollection.class);
            PropertyMetadata prop = metadata.getProperty("items");

            assertThat(prop.isAssociation()).isTrue();
            assertThat(prop.getAssociationType()).isEqualTo(PropertyMetadata.AssociationType.ONE_TO_MANY);
        }
    }

    @Nested
    @DisplayName("Bean Validation analysis")
    class ValidationTests {

        @Test
        @DisplayName("Should detect @NotNull")
        void shouldDetectNotNull() {
            EntityMetadata metadata = provider.analyze(ValidationEntity.class);
            PropertyMetadata prop = metadata.getProperty("notNullField");

            assertThat(prop.isRequired()).isTrue();
            assertThat(prop.isNullable()).isFalse();
        }

        @Test
        @DisplayName("Should detect @NotBlank")
        void shouldDetectNotBlank() {
            EntityMetadata metadata = provider.analyze(ValidationEntity.class);
            PropertyMetadata prop = metadata.getProperty("notBlankField");

            assertThat(prop.isRequired()).isTrue();
        }

        @Test
        @DisplayName("Should detect @Size")
        void shouldDetectSize() {
            EntityMetadata metadata = provider.analyze(ValidationEntity.class);
            PropertyMetadata prop = metadata.getProperty("sizedField");

            assertThat(prop.getMinLength()).isEqualTo(2);
            assertThat(prop.getMaxLength()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should detect @Min and @Max")
        void shouldDetectMinMax() {
            EntityMetadata metadata = provider.analyze(ValidationEntity.class);
            PropertyMetadata prop = metadata.getProperty("rangedField");

            assertThat(prop.getMinValue()).isEqualTo(1L);
            assertThat(prop.getMaxValue()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should detect @Email")
        void shouldDetectEmail() {
            EntityMetadata metadata = provider.analyze(ValidationEntity.class);
            PropertyMetadata prop = metadata.getProperty("emailField");

            assertThat(prop.isEmail()).isTrue();
        }

        @Test
        @DisplayName("Should detect @Pattern")
        void shouldDetectPattern() {
            EntityMetadata metadata = provider.analyze(ValidationEntity.class);
            PropertyMetadata prop = metadata.getProperty("patternField");

            assertThat(prop.getPattern()).isEqualTo("[A-Z]{3}");
        }
    }

    @Nested
    @DisplayName("Field filtering")
    class FieldFilteringTests {

        @Test
        @DisplayName("Should skip @Transient fields")
        void shouldSkipTransientFields() {
            EntityMetadata metadata = provider.analyze(TransientEntity.class);

            assertThat(metadata.hasProperty("transientField")).isFalse();
            assertThat(metadata.hasProperty("staticField")).isFalse();
            assertThat(metadata.hasProperty("normalField")).isTrue();
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class InheritanceTests {

        @Test
        @DisplayName("Should include inherited fields")
        void shouldIncludeInheritedFields() {
            EntityMetadata metadata = provider.analyze(ChildEntity.class);

            assertThat(metadata.hasProperty("id")).isTrue();
            assertThat(metadata.hasProperty("createdAt")).isTrue();
            assertThat(metadata.hasProperty("childField")).isTrue();
        }
    }

    @Nested
    @DisplayName("Priority")
    class PriorityTests {

        @Test
        @DisplayName("Should have lower priority than JPA")
        void shouldHaveLowerPriorityThanJpa() {
            assertThat(provider.getPriority()).isLessThan(100);
        }
    }

    // Test entities

    static class SimpleEntity {
        @Id
        private Long id;
        private String name;

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    @Table("custom_table")
    static class TableEntity {
        @Id
        private Long id;

        public Long getId() { return id; }
    }

    static class EntityWithEmbedded {
        @Id
        private Long id;

        @Embedded.Empty
        private EmbeddableAddress address;

        public Long getId() { return id; }
        public EmbeddableAddress getAddress() { return address; }
    }

    static class EmbeddableAddress {
        private String street;
        private String city;

        public String getStreet() { return street; }
        public String getCity() { return city; }
    }

    static class EntityWithCollection {
        @Id
        private Long id;

        @MappedCollection(idColumn = "parent_id")
        private List<ChildItem> items;

        public Long getId() { return id; }
        public List<ChildItem> getItems() { return items; }
    }

    static class ChildItem {
        @Id
        private Long id;
        private String name;

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    static class ValidationEntity {
        @Id
        private Long id;

        @NotNull
        private String notNullField;

        @NotBlank
        private String notBlankField;

        @Size(min = 2, max = 100)
        private String sizedField;

        @Min(1)
        @Max(100)
        private Integer rangedField;

        @Email
        private String emailField;

        @Pattern(regexp = "[A-Z]{3}")
        private String patternField;

        public Long getId() { return id; }
        public String getNotNullField() { return notNullField; }
        public String getNotBlankField() { return notBlankField; }
        public String getSizedField() { return sizedField; }
        public Integer getRangedField() { return rangedField; }
        public String getEmailField() { return emailField; }
        public String getPatternField() { return patternField; }
    }

    static class TransientEntity {
        @Id
        private Long id;

        private String normalField;

        @Transient
        private String transientField;

        private static String staticField;

        public Long getId() { return id; }
        public String getNormalField() { return normalField; }
        public String getTransientField() { return transientField; }
    }

    static class BaseEntity {
        @Id
        private Long id;

        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    static class ChildEntity extends BaseEntity {
        private String childField;

        public String getChildField() { return childField; }
    }
}
