package io.github.ramils.jpa;

import io.github.ramils.metadata.EntityMetadata;
import io.github.ramils.metadata.PropertyMetadata;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JpaMetadataProviderTest {

    private JpaMetadataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JpaMetadataProvider();
        provider.clearCache();
    }

    @Nested
    @DisplayName("supports()")
    class SupportsTests {

        @Test
        @DisplayName("Should support @Entity classes")
        void shouldSupportEntityClasses() {
            assertThat(provider.supports(SimpleEntity.class)).isTrue();
        }

        @Test
        @DisplayName("Should support @Embeddable classes")
        void shouldSupportEmbeddableClasses() {
            assertThat(provider.supports(EmbeddableAddress.class)).isTrue();
        }

        @Test
        @DisplayName("Should support @MappedSuperclass")
        void shouldSupportMappedSuperclass() {
            assertThat(provider.supports(BaseEntity.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("@Column analysis")
    class ColumnTests {

        @Test
        @DisplayName("Should detect nullable from @Column")
        void shouldDetectNullable() {
            EntityMetadata metadata = provider.analyze(ColumnEntity.class);
            PropertyMetadata prop = metadata.getProperty("requiredField");

            assertThat(prop.isNullable()).isFalse();
            assertThat(prop.isRequired()).isTrue();
        }

        @Test
        @DisplayName("Should detect unique from @Column")
        void shouldDetectUnique() {
            EntityMetadata metadata = provider.analyze(ColumnEntity.class);
            PropertyMetadata prop = metadata.getProperty("uniqueField");

            assertThat(prop.isUnique()).isTrue();
        }

        @Test
        @DisplayName("Should detect length from @Column")
        void shouldDetectLength() {
            EntityMetadata metadata = provider.analyze(ColumnEntity.class);
            PropertyMetadata prop = metadata.getProperty("limitedField");

            assertThat(prop.getMaxLength()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("@Id analysis")
    class IdTests {

        @Test
        @DisplayName("Should mark @Id as required when not generated")
        void shouldMarkIdAsRequired() {
            EntityMetadata metadata = provider.analyze(ManualIdEntity.class);
            PropertyMetadata prop = metadata.getProperty("id");

            assertThat(prop.isRequired()).isTrue();
            assertThat(prop.isNullable()).isFalse();
        }

        @Test
        @DisplayName("Should not require @Id when @GeneratedValue present")
        void shouldNotRequireGeneratedId() {
            EntityMetadata metadata = provider.analyze(SimpleEntity.class);
            PropertyMetadata prop = metadata.getProperty("id");

            assertThat(prop.isRequired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Association analysis")
    class AssociationTests {

        @Test
        @DisplayName("Should detect @ManyToOne")
        void shouldDetectManyToOne() {
            EntityMetadata metadata = provider.analyze(AssociationEntity.class);
            PropertyMetadata prop = metadata.getProperty("department");

            assertThat(prop.isAssociation()).isTrue();
            assertThat(prop.getAssociationType()).isEqualTo(PropertyMetadata.AssociationType.MANY_TO_ONE);
            assertThat(prop.isOwning()).isTrue();
        }

        @Test
        @DisplayName("Should detect required @ManyToOne")
        void shouldDetectRequiredManyToOne() {
            EntityMetadata metadata = provider.analyze(AssociationEntity.class);
            PropertyMetadata prop = metadata.getProperty("requiredDepartment");

            assertThat(prop.isRequired()).isTrue();
            assertThat(prop.isNullable()).isFalse();
        }

        @Test
        @DisplayName("Should detect @OneToMany with mappedBy")
        void shouldDetectOneToMany() {
            EntityMetadata metadata = provider.analyze(AssociationEntity.class);
            PropertyMetadata prop = metadata.getProperty("items");

            assertThat(prop.isAssociation()).isTrue();
            assertThat(prop.getAssociationType()).isEqualTo(PropertyMetadata.AssociationType.ONE_TO_MANY);
            assertThat(prop.getMappedBy()).isEqualTo("parent");
            assertThat(prop.isOwning()).isFalse();
        }

        @Test
        @DisplayName("Should detect @OneToOne")
        void shouldDetectOneToOne() {
            EntityMetadata metadata = provider.analyze(AssociationEntity.class);
            PropertyMetadata prop = metadata.getProperty("profile");

            assertThat(prop.isAssociation()).isTrue();
            assertThat(prop.getAssociationType()).isEqualTo(PropertyMetadata.AssociationType.ONE_TO_ONE);
        }

        @Test
        @DisplayName("Should detect @ManyToMany")
        void shouldDetectManyToMany() {
            EntityMetadata metadata = provider.analyze(AssociationEntity.class);
            PropertyMetadata prop = metadata.getProperty("tags");

            assertThat(prop.isAssociation()).isTrue();
            assertThat(prop.getAssociationType()).isEqualTo(PropertyMetadata.AssociationType.MANY_TO_MANY);
        }

        @Test
        @DisplayName("Should detect @Embedded")
        void shouldDetectEmbedded() {
            EntityMetadata metadata = provider.analyze(AssociationEntity.class);
            PropertyMetadata prop = metadata.getProperty("address");

            assertThat(prop.isAssociation()).isTrue();
            assertThat(prop.getAssociationType()).isEqualTo(PropertyMetadata.AssociationType.EMBEDDED);
            assertThat(prop.isEmbedded()).isTrue();
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

        @Test
        @DisplayName("Should detect @Positive")
        void shouldDetectPositive() {
            EntityMetadata metadata = provider.analyze(ValidationEntity.class);
            PropertyMetadata prop = metadata.getProperty("positiveField");

            assertThat(prop.getMinValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should detect @DecimalMin/@DecimalMax")
        void shouldDetectDecimalMinMax() {
            EntityMetadata metadata = provider.analyze(ValidationEntity.class);
            PropertyMetadata prop = metadata.getProperty("decimalField");

            assertThat(prop.getMinValue()).isEqualTo(new BigDecimal("0.01"));
            assertThat(prop.getMaxValue()).isEqualTo(new BigDecimal("999.99"));
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

        @Test
        @DisplayName("Should detect entity superclass")
        void shouldDetectEntitySuperclass() {
            EntityMetadata metadata = provider.analyze(ChildEntity.class);

            assertThat(metadata.getSuperclass()).isEqualTo(BaseEntity.class);
        }
    }

    // Test entities

    @Entity
    static class SimpleEntity {
        @Id
        @GeneratedValue
        private Long id;
        private String name;

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    @Entity
    static class ManualIdEntity {
        @Id
        private Long id;

        public Long getId() { return id; }
    }

    @Entity
    static class ColumnEntity {
        @Id
        @GeneratedValue
        private Long id;

        @Column(nullable = false)
        private String requiredField;

        @Column(unique = true)
        private String uniqueField;

        @Column(length = 50)
        private String limitedField;

        public Long getId() { return id; }
        public String getRequiredField() { return requiredField; }
        public String getUniqueField() { return uniqueField; }
        public String getLimitedField() { return limitedField; }
    }

    @Entity
    static class AssociationEntity {
        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne
        private SimpleEntity department;

        @ManyToOne(optional = false)
        private SimpleEntity requiredDepartment;

        @OneToMany(mappedBy = "parent")
        private List<SimpleEntity> items;

        @OneToOne
        private SimpleEntity profile;

        @ManyToMany
        private Set<SimpleEntity> tags;

        @Embedded
        private EmbeddableAddress address;

        public Long getId() { return id; }
        public SimpleEntity getDepartment() { return department; }
        public SimpleEntity getRequiredDepartment() { return requiredDepartment; }
        public List<SimpleEntity> getItems() { return items; }
        public SimpleEntity getProfile() { return profile; }
        public Set<SimpleEntity> getTags() { return tags; }
        public EmbeddableAddress getAddress() { return address; }
    }

    @Embeddable
    static class EmbeddableAddress {
        private String street;
        private String city;

        public String getStreet() { return street; }
        public String getCity() { return city; }
    }

    @Entity
    static class ValidationEntity {
        @Id
        @GeneratedValue
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

        @Positive
        private Integer positiveField;

        @DecimalMin("0.01")
        @DecimalMax("999.99")
        private BigDecimal decimalField;

        public Long getId() { return id; }
        public String getNotNullField() { return notNullField; }
        public String getNotBlankField() { return notBlankField; }
        public String getSizedField() { return sizedField; }
        public Integer getRangedField() { return rangedField; }
        public String getEmailField() { return emailField; }
        public String getPatternField() { return patternField; }
        public Integer getPositiveField() { return positiveField; }
        public BigDecimal getDecimalField() { return decimalField; }
    }

    @Entity
    static class TransientEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String normalField;

        @Transient
        private String transientField;

        private static String staticField;

        public Long getId() { return id; }
        public String getNormalField() { return normalField; }
        public String getTransientField() { return transientField; }
    }

    @MappedSuperclass
    static class BaseEntity {
        @Id
        @GeneratedValue
        private Long id;

        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    @Entity
    static class ChildEntity extends BaseEntity {
        private String childField;

        public String getChildField() { return childField; }
    }
}
