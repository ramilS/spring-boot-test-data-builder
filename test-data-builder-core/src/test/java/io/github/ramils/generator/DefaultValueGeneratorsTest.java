package io.github.ramils.generator;

import io.github.ramils.metadata.PropertyMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultValueGeneratorsTest {

    private GeneratorContext context;

    @BeforeEach
    void setUp() {
        context = new GeneratorContext();
    }

    @Nested
    @DisplayName("EnumGenerator")
    class EnumGeneratorTest {

        private DefaultValueGenerators.EnumGenerator generator = new DefaultValueGenerators.EnumGenerator();

        @Test
        @DisplayName("Should support enum types")
        void shouldSupportEnumTypes() {
            PropertyMetadata prop = mockProperty(TestEnum.class);
            assertThat(generator.supports(prop)).isTrue();
        }

        @Test
        @DisplayName("Should not support non-enum types")
        void shouldNotSupportNonEnumTypes() {
            PropertyMetadata prop = mockProperty(String.class);
            assertThat(generator.supports(prop)).isFalse();
        }

        @Test
        @DisplayName("Should return first enum value")
        void shouldReturnFirstEnumValue() {
            PropertyMetadata prop = mockProperty(TestEnum.class);
            Object result = generator.generate(prop, context);
            assertThat(result).isEqualTo(TestEnum.FIRST);
        }
    }

    @Nested
    @DisplayName("EmailGenerator")
    class EmailGeneratorTest {

        private DefaultValueGenerators.EmailGenerator generator = new DefaultValueGenerators.EmailGenerator();

        @Test
        @DisplayName("Should support @Email annotated fields")
        void shouldSupportEmailAnnotatedFields() {
            PropertyMetadata prop = mockProperty(String.class);
            when(prop.isEmail()).thenReturn(true);
            assertThat(generator.supports(prop)).isTrue();
        }

        @Test
        @DisplayName("Should support fields named email")
        void shouldSupportFieldsNamedEmail() {
            PropertyMetadata prop = mockProperty(String.class);
            when(prop.getName()).thenReturn("userEmail");
            assertThat(generator.supports(prop)).isTrue();
        }

        @Test
        @DisplayName("Should generate valid email")
        void shouldGenerateValidEmail() {
            PropertyMetadata prop = mockProperty(String.class);
            when(prop.getName()).thenReturn("email");
            when(prop.getDeclaringClass()).thenReturn((Class) TestClass.class);

            Object result = generator.generate(prop, context);

            assertThat(result.toString()).contains("@test.com");
        }

        @Test
        @DisplayName("Should add counter for unique emails")
        void shouldAddCounterForUniqueEmails() {
            PropertyMetadata prop = mockProperty(String.class);
            when(prop.getName()).thenReturn("email");
            when(prop.getDeclaringClass()).thenReturn((Class) TestClass.class);
            when(prop.isUnique()).thenReturn(true);

            String email1 = (String) generator.generate(prop, context);
            String email2 = (String) generator.generate(prop, context);

            assertThat(email1).isNotEqualTo(email2);
        }
    }

    @Nested
    @DisplayName("StringGenerator")
    class StringGeneratorTest {

        private DefaultValueGenerators.StringGenerator generator = new DefaultValueGenerators.StringGenerator();

        @Test
        @DisplayName("Should support String type")
        void shouldSupportStringType() {
            PropertyMetadata prop = mockProperty(String.class);
            assertThat(generator.supports(prop)).isTrue();
        }

        @Test
        @DisplayName("Should generate string from property name")
        void shouldGenerateStringFromPropertyName() {
            PropertyMetadata prop = mock(PropertyMetadata.class);
            when(prop.getType()).thenReturn((Class) String.class);
            when(prop.getName()).thenReturn("firstName");
            when(prop.getDeclaringClass()).thenReturn((Class) TestClass.class);
            when(prop.isUnique()).thenReturn(false);
            when(prop.getMinLength()).thenReturn(null);
            when(prop.getMaxLength()).thenReturn(null);

            Object result = generator.generate(prop, context);

            assertThat(result).isEqualTo("firstName");
        }

        @Test
        @DisplayName("Should respect maxLength constraint")
        void shouldRespectMaxLength() {
            PropertyMetadata prop = mock(PropertyMetadata.class);
            when(prop.getType()).thenReturn((Class) String.class);
            when(prop.getName()).thenReturn("longPropertyName");
            when(prop.getMaxLength()).thenReturn(5);
            when(prop.getMinLength()).thenReturn(null);
            when(prop.getDeclaringClass()).thenReturn((Class) TestClass.class);
            when(prop.isUnique()).thenReturn(false);

            Object result = generator.generate(prop, context);

            assertThat(result.toString()).hasSize(5);
        }

        @Test
        @DisplayName("Should respect minLength constraint")
        void shouldRespectMinLength() {
            PropertyMetadata prop = mock(PropertyMetadata.class);
            when(prop.getType()).thenReturn((Class) String.class);
            when(prop.getName()).thenReturn("ab");
            when(prop.getMinLength()).thenReturn(10);
            when(prop.getMaxLength()).thenReturn(null);
            when(prop.getDeclaringClass()).thenReturn((Class) TestClass.class);
            when(prop.isUnique()).thenReturn(false);

            Object result = generator.generate(prop, context);

            assertThat(result.toString()).hasSizeGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("Should add counter for unique strings")
        void shouldAddCounterForUniqueStrings() {
            PropertyMetadata prop = mock(PropertyMetadata.class);
            when(prop.getType()).thenReturn((Class) String.class);
            when(prop.getName()).thenReturn("code");
            when(prop.getDeclaringClass()).thenReturn((Class) TestClass.class);
            when(prop.isUnique()).thenReturn(true);
            when(prop.getMinLength()).thenReturn(null);
            when(prop.getMaxLength()).thenReturn(null);

            String str1 = (String) generator.generate(prop, context);
            String str2 = (String) generator.generate(prop, context);

            assertThat(str1).isNotEqualTo(str2);
        }
    }

    @Nested
    @DisplayName("NumberGenerator")
    class NumberGeneratorTest {

        private DefaultValueGenerators.NumberGenerator generator = new DefaultValueGenerators.NumberGenerator();

        @Test
        @DisplayName("Should support Integer type")
        void shouldSupportIntegerType() {
            assertThat(generator.supports(mockProperty(Integer.class))).isTrue();
            assertThat(generator.supports(mockProperty(int.class))).isTrue();
        }

        @Test
        @DisplayName("Should support Long type")
        void shouldSupportLongType() {
            assertThat(generator.supports(mockProperty(Long.class))).isTrue();
            assertThat(generator.supports(mockProperty(long.class))).isTrue();
        }

        @Test
        @DisplayName("Should support BigDecimal type")
        void shouldSupportBigDecimalType() {
            assertThat(generator.supports(mockProperty(BigDecimal.class))).isTrue();
        }

        @Test
        @DisplayName("Should generate Integer value")
        void shouldGenerateIntegerValue() {
            PropertyMetadata prop = mockProperty(Integer.class);
            when(prop.getDeclaringClass()).thenReturn((Class) TestClass.class);

            Object result = generator.generate(prop, context);

            assertThat(result).isInstanceOf(Integer.class);
        }

        @Test
        @DisplayName("Should respect @Min constraint")
        void shouldRespectMinConstraint() {
            PropertyMetadata prop = mockProperty(Integer.class);
            when(prop.getMinValue()).thenReturn(10);
            when(prop.getDeclaringClass()).thenReturn((Class) TestClass.class);

            Object result = generator.generate(prop, context);

            assertThat((Integer) result).isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("Should generate BigDecimal value")
        void shouldGenerateBigDecimalValue() {
            PropertyMetadata prop = mockProperty(BigDecimal.class);
            when(prop.getDeclaringClass()).thenReturn((Class) TestClass.class);

            Object result = generator.generate(prop, context);

            assertThat(result).isInstanceOf(BigDecimal.class);
        }
    }

    @Nested
    @DisplayName("BooleanGenerator")
    class BooleanGeneratorTest {

        private DefaultValueGenerators.BooleanGenerator generator = new DefaultValueGenerators.BooleanGenerator();

        @Test
        @DisplayName("Should support Boolean types")
        void shouldSupportBooleanTypes() {
            assertThat(generator.supports(mockProperty(Boolean.class))).isTrue();
            assertThat(generator.supports(mockProperty(boolean.class))).isTrue();
        }

        @Test
        @DisplayName("Should generate false by default")
        void shouldGenerateFalseByDefault() {
            PropertyMetadata prop = mockProperty(boolean.class);
            Object result = generator.generate(prop, context);
            assertThat(result).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("TemporalGenerator")
    class TemporalGeneratorTest {

        private DefaultValueGenerators.TemporalGenerator generator = new DefaultValueGenerators.TemporalGenerator();

        @Test
        @DisplayName("Should support LocalDate")
        void shouldSupportLocalDate() {
            assertThat(generator.supports(mockProperty(LocalDate.class))).isTrue();
        }

        @Test
        @DisplayName("Should support LocalDateTime")
        void shouldSupportLocalDateTime() {
            assertThat(generator.supports(mockProperty(LocalDateTime.class))).isTrue();
        }

        @Test
        @DisplayName("Should support Instant")
        void shouldSupportInstant() {
            assertThat(generator.supports(mockProperty(Instant.class))).isTrue();
        }

        @Test
        @DisplayName("Should generate LocalDate")
        void shouldGenerateLocalDate() {
            PropertyMetadata prop = mockProperty(LocalDate.class);
            Object result = generator.generate(prop, context);
            assertThat(result).isInstanceOf(LocalDate.class);
        }

        @Test
        @DisplayName("Should generate LocalDateTime")
        void shouldGenerateLocalDateTime() {
            PropertyMetadata prop = mockProperty(LocalDateTime.class);
            Object result = generator.generate(prop, context);
            assertThat(result).isInstanceOf(LocalDateTime.class);
        }

        @Test
        @DisplayName("Should support java.util.Date")
        void shouldSupportJavaUtilDate() {
            assertThat(generator.supports(mockProperty(Date.class))).isTrue();
        }

        @Test
        @DisplayName("Should generate java.util.Date")
        void shouldGenerateJavaUtilDate() {
            PropertyMetadata prop = mockProperty(Date.class);
            Object result = generator.generate(prop, context);
            assertThat(result).isInstanceOf(Date.class);
        }
    }

    @Nested
    @DisplayName("UuidGenerator")
    class UuidGeneratorTest {

        private DefaultValueGenerators.UuidGenerator generator = new DefaultValueGenerators.UuidGenerator();

        @Test
        @DisplayName("Should support UUID type")
        void shouldSupportUuidType() {
            assertThat(generator.supports(mockProperty(UUID.class))).isTrue();
        }

        @Test
        @DisplayName("Should generate unique UUIDs")
        void shouldGenerateUniqueUuids() {
            PropertyMetadata prop = mockProperty(UUID.class);

            UUID uuid1 = (UUID) generator.generate(prop, context);
            UUID uuid2 = (UUID) generator.generate(prop, context);

            assertThat(uuid1).isNotEqualTo(uuid2);
        }
    }

    @Nested
    @DisplayName("CollectionGenerator")
    class CollectionGeneratorTest {

        private DefaultValueGenerators.CollectionGenerator generator = new DefaultValueGenerators.CollectionGenerator();

        @Test
        @DisplayName("Should support List type")
        void shouldSupportListType() {
            assertThat(generator.supports(mockProperty(List.class))).isTrue();
        }

        @Test
        @DisplayName("Should support Set type")
        void shouldSupportSetType() {
            assertThat(generator.supports(mockProperty(Set.class))).isTrue();
        }

        @Test
        @DisplayName("Should support Map type")
        void shouldSupportMapType() {
            assertThat(generator.supports(mockProperty(Map.class))).isTrue();
        }

        @Test
        @DisplayName("Should generate empty ArrayList for List")
        void shouldGenerateEmptyArrayListForList() {
            PropertyMetadata prop = mockProperty(List.class);
            Object result = generator.generate(prop, context);
            assertThat(result).isInstanceOf(ArrayList.class);
            assertThat((List<?>) result).isEmpty();
        }

        @Test
        @DisplayName("Should generate empty HashSet for Set")
        void shouldGenerateEmptyHashSetForSet() {
            PropertyMetadata prop = mockProperty(Set.class);
            Object result = generator.generate(prop, context);
            assertThat(result).isInstanceOf(HashSet.class);
            assertThat((Set<?>) result).isEmpty();
        }

        @Test
        @DisplayName("Should generate empty HashMap for Map")
        void shouldGenerateEmptyHashMapForMap() {
            PropertyMetadata prop = mockProperty(Map.class);
            Object result = generator.generate(prop, context);
            assertThat(result).isInstanceOf(HashMap.class);
            assertThat((Map<?, ?>) result).isEmpty();
        }
    }

    @Nested
    @DisplayName("PatternGenerator")
    class PatternGeneratorTest {

        private DefaultValueGenerators.PatternGenerator generator = new DefaultValueGenerators.PatternGenerator();

        @Test
        @DisplayName("Should support String with pattern")
        void shouldSupportStringWithPattern() {
            PropertyMetadata prop = mockProperty(String.class);
            when(prop.getPattern()).thenReturn("[A-Z]{3}");
            assertThat(generator.supports(prop)).isTrue();
        }

        @Test
        @DisplayName("Should not support String without pattern")
        void shouldNotSupportStringWithoutPattern() {
            PropertyMetadata prop = mockProperty(String.class);
            when(prop.getPattern()).thenReturn(null);
            assertThat(generator.supports(prop)).isFalse();
        }

        @Test
        @DisplayName("Should generate string matching simple pattern")
        void shouldGenerateStringMatchingPattern() {
            PropertyMetadata prop = mockProperty(String.class);
            when(prop.getName()).thenReturn("code");
            when(prop.getPattern()).thenReturn("[A-Z]{3}");

            Object result = generator.generate(prop, context);

            assertThat(result.toString()).matches("[A-Z]{3}");
        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAllTest {

        @Test
        @DisplayName("Should return all generators")
        void shouldReturnAllGenerators() {
            List<ValueGenerator> generators = DefaultValueGenerators.getAll();

            assertThat(generators).hasSize(10);
            assertThat(generators).hasAtLeastOneElementOfType(DefaultValueGenerators.EnumGenerator.class);
            assertThat(generators).hasAtLeastOneElementOfType(DefaultValueGenerators.EmailGenerator.class);
            assertThat(generators).hasAtLeastOneElementOfType(DefaultValueGenerators.StringGenerator.class);
            assertThat(generators).hasAtLeastOneElementOfType(DefaultValueGenerators.NumberGenerator.class);
            assertThat(generators).hasAtLeastOneElementOfType(DefaultValueGenerators.BooleanGenerator.class);
            assertThat(generators).hasAtLeastOneElementOfType(DefaultValueGenerators.TemporalGenerator.class);
            assertThat(generators).hasAtLeastOneElementOfType(DefaultValueGenerators.UuidGenerator.class);
        }
    }

    // Helper methods

    private PropertyMetadata mockProperty(Class<?> type) {
        PropertyMetadata prop = mock(PropertyMetadata.class);
        when(prop.getType()).thenReturn((Class) type);
        when(prop.getName()).thenReturn("testProperty");
        when(prop.getDeclaringClass()).thenReturn((Class) TestClass.class);
        return prop;
    }

    // Test types

    enum TestEnum {
        FIRST, SECOND, THIRD
    }

    static class TestClass {
        private String field;
    }
}
