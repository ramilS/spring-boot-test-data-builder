package io.github.ramils.builder;

import io.github.ramils.config.TestDataConfig;
import io.github.ramils.core.MaxDepthExceededException;
import io.github.ramils.metadata.EntityMetadata;
import io.github.ramils.metadata.MetadataProvider;
import io.github.ramils.metadata.PropertyMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityBuilderTest {

    private TestDataConfig config;

    @BeforeEach
    void setUp() {
        config = new TestDataConfig();
        config.metadataProvider(new SimpleMetadataProvider());
    }

    @Nested
    @DisplayName("set(PropertyReference, value)")
    class SetFixedValueTest {

        @Test
        @DisplayName("Should set fixed value using method reference")
        void shouldSetFixedValue() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            User user = builder
                    .set(User::getFirstName, "John")
                    .set(User::getLastName, "Doe")
                    .create();

            assertThat(user.getFirstName()).isEqualTo("John");
            assertThat(user.getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Should allow null values")
        void shouldAllowNullValues() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            User user = builder
                    .set(User::getFirstName, (String) null)
                    .create();

            assertThat(user.getFirstName()).isNull();
        }
    }

    @Nested
    @DisplayName("setAll(Map)")
    class SetAllTest {

        @Test
        @DisplayName("Should set multiple values from map")
        void shouldSetMultipleValues() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            Map<String, Object> values = new HashMap<>();
            values.put("firstName", "Jane");
            values.put("lastName", "Smith");
            values.put("email", "jane@test.com");

            User user = builder.setAll(values).create();

            assertThat(user.getFirstName()).isEqualTo("Jane");
            assertThat(user.getLastName()).isEqualTo("Smith");
            assertThat(user.getEmail()).isEqualTo("jane@test.com");
        }
    }

    @Nested
    @DisplayName("Supplier via global config")
    class SupplierViaGlobalConfigTest {

        @Test
        @DisplayName("Should call supplier each time via global config")
        void shouldCallSupplierEachTimeViaGlobalConfig() {
            AtomicInteger counter = new AtomicInteger(0);

            config.forType(User.class, defaults -> defaults
                    .generate(User::getFirstName, () -> "User" + counter.incrementAndGet()));

            User user1 = new EntityBuilder<>(User.class, config).create();
            User user2 = new EntityBuilder<>(User.class, config).create();

            assertThat(user1.getFirstName()).isEqualTo("User1");
            assertThat(user2.getFirstName()).isEqualTo("User2");
        }
    }

    @Nested
    @DisplayName("sequence(PropertyReference, IntFunction)")
    class SequenceTest {

        @Test
        @DisplayName("Should generate sequential values")
        void shouldGenerateSequentialValues() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            List<User> users = builder
                    .sequence(User::getEmail, i -> "user" + i + "@test.com")
                    .createList(3);

            assertThat(users).extracting(User::getEmail)
                    .containsExactly("user0@test.com", "user1@test.com", "user2@test.com");
        }

        @Test
        @DisplayName("Sequence should work with single create")
        void sequenceShouldWorkWithSingleCreate() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            User user = builder
                    .sequence(User::getEmail, i -> "user" + i + "@test.com")
                    .create();

            assertThat(user.getEmail()).isEqualTo("user0@test.com");
        }
    }

    @Nested
    @DisplayName("derive(PropertyReference, Function)")
    class DeriveTest {

        @Test
        @DisplayName("Should compute derived value from other fields")
        void shouldComputeDerivedValue() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            User user = builder
                    .set(User::getFirstName, "John")
                    .set(User::getLastName, "Doe")
                    .derive(User::getEmail, u ->
                            u.getFirstName().toLowerCase() + "." +
                            u.getLastName().toLowerCase() + "@company.com")
                    .create();

            assertThat(user.getEmail()).isEqualTo("john.doe@company.com");
        }

        @Test
        @DisplayName("Set should override derive")
        void setShouldOverrideDerive() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            User user = builder
                    .set(User::getEmail, "explicit@test.com")
                    .derive(User::getEmail, u -> "derived@test.com")
                    .create();

            assertThat(user.getEmail()).isEqualTo("explicit@test.com");
        }
    }

    @Nested
    @DisplayName("setNull(PropertyReference)")
    class SetNullTest {

        @Test
        @DisplayName("Should explicitly set property to null")
        void shouldExplicitlySetNull() {
            config.metadataProvider(new RequiredFieldsMetadataProvider());

            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            User user = builder
                    .setNull(User::getFirstName)
                    .create();

            assertThat(user.getFirstName()).isNull();
        }
    }

    @Nested
    @DisplayName("create()")
    class CreateTest {

        @Test
        @DisplayName("Should create instance with default values")
        void shouldCreateInstanceWithDefaults() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            User user = builder.create();

            assertThat(user).isNotNull();
        }

        @Test
        @DisplayName("Should auto-generate required string fields")
        void shouldAutoGenerateRequiredFields() {
            config.metadataProvider(new RequiredFieldsMetadataProvider());

            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            User user = builder.create();

            assertThat(user.getFirstName()).isNotNull();
            assertThat(user.getLastName()).isNotNull();
        }
    }

    @Nested
    @DisplayName("createList(count)")
    class CreateListTest {

        @Test
        @DisplayName("Should create specified number of instances")
        void shouldCreateSpecifiedCount() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            List<User> users = builder.createList(5);

            assertThat(users).hasSize(5);
        }

        @Test
        @DisplayName("Should create distinct instances")
        void shouldCreateDistinctInstances() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            List<User> users = builder.createList(3);

            assertThat(users.get(0)).isNotSameAs(users.get(1));
            assertThat(users.get(1)).isNotSameAs(users.get(2));
        }

        @Test
        @DisplayName("Should return empty list for count 0")
        void shouldReturnEmptyListForZero() {
            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            List<User> users = builder.createList(0);

            assertThat(users).isEmpty();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTest {

        @Test
        @DisplayName("Should throw when no metadata provider")
        void shouldThrowWhenNoProvider() {
            TestDataConfig emptyConfig = new TestDataConfig();

            EntityBuilder<User> builder = new EntityBuilder<>(User.class, emptyConfig);

            assertThatThrownBy(builder::create)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No MetadataProvider found");
        }

        @Test
        @DisplayName("Should throw for abstract class without concrete default")
        void shouldThrowForAbstractClass() {
            EntityBuilder<Payment> builder = new EntityBuilder<>(Payment.class, config);

            assertThatThrownBy(builder::create)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("abstract class");
        }

        @Test
        @DisplayName("Should throw for class without no-arg constructor")
        void shouldThrowForNoArgConstructor() {
            EntityBuilder<NoDefaultConstructor> builder =
                    new EntityBuilder<>(NoDefaultConstructor.class, config);

            assertThatThrownBy(builder::create)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no-argument constructor");
        }

        @Test
        @DisplayName("Should throw MaxDepthExceededException when depth exceeded")
        void shouldThrowWhenMaxDepthExceeded() {
            config.maxDepth(1);
            config.metadataProvider(new DeepNestingMetadataProvider());

            EntityBuilder<Level1> builder = new EntityBuilder<>(Level1.class, config);

            assertThatThrownBy(builder::create)
                    .isInstanceOf(MaxDepthExceededException.class);
        }
    }

    @Nested
    @DisplayName("Abstract type resolution")
    class AbstractTypeResolutionTest {

        @Test
        @DisplayName("Should resolve abstract class to concrete implementation")
        void shouldResolveAbstractClass() {
            config.abstractDefault(Payment.class, CreditCardPayment.class);

            EntityBuilder<Payment> builder = new EntityBuilder<>(Payment.class, config);

            Payment payment = builder.create();

            assertThat(payment).isInstanceOf(CreditCardPayment.class);
        }
    }

    @Nested
    @DisplayName("Global config integration")
    class GlobalConfigIntegrationTest {

        @Test
        @DisplayName("Should apply global config defaults")
        void shouldApplyGlobalDefaults() {
            config.forType(User.class, defaults -> defaults
                    .set(User::getFirstName, "GlobalDefault"));

            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            User user = builder.create();

            assertThat(user.getFirstName()).isEqualTo("GlobalDefault");
        }

        @Test
        @DisplayName("Builder should override global config")
        void builderShouldOverrideGlobalConfig() {
            config.forType(User.class, defaults -> defaults
                    .set(User::getFirstName, "GlobalDefault"));

            EntityBuilder<User> builder = new EntityBuilder<>(User.class, config);

            User user = builder
                    .set(User::getFirstName, "BuilderOverride")
                    .create();

            assertThat(user.getFirstName()).isEqualTo("BuilderOverride");
        }
    }

    // Test entities

    public static class User {
        private String firstName;
        private String lastName;
        private String email;
        private Integer age;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }

    public static abstract class Payment {
        private String amount;

        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
    }

    public static class CreditCardPayment extends Payment {
        private String cardNumber;

        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    }

    public static class NoDefaultConstructor {
        private final String value;

        public NoDefaultConstructor(String value) {
            this.value = value;
        }

        public String getValue() { return value; }
    }

    public static class SelfReferencingEntity {
        private Long id;
        private SelfReferencingEntity parent;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public SelfReferencingEntity getParent() { return parent; }
        public void setParent(SelfReferencingEntity parent) { this.parent = parent; }
    }

    public static class Level1 {
        private Long id;
        private Level2 level2;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Level2 getLevel2() { return level2; }
        public void setLevel2(Level2 level2) { this.level2 = level2; }
    }

    public static class Level2 {
        private Long id;
        private Level3 level3;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Level3 getLevel3() { return level3; }
        public void setLevel3(Level3 level3) { this.level3 = level3; }
    }

    public static class Level3 {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // Test metadata providers

    static class SimpleMetadataProvider implements MetadataProvider {

        @Override
        public EntityMetadata analyze(Class<?> entityClass) {
            Map<String, PropertyMetadata> properties = new LinkedHashMap<>();

            for (Field field : getAllFields(entityClass)) {
                PropertyMetadata prop = PropertyMetadata.builder()
                        .name(field.getName())
                        .field(field)
                        .type(field.getType())
                        .declaringClass(field.getDeclaringClass())
                        .required(false)
                        .nullable(true)
                        .build();
                properties.put(field.getName(), prop);
            }

            return new EntityMetadata(entityClass, properties, false, null);
        }

        private List<Field> getAllFields(Class<?> clazz) {
            List<Field> fields = new ArrayList<>();
            while (clazz != null && clazz != Object.class) {
                fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
                clazz = clazz.getSuperclass();
            }
            return fields;
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return true;
        }

        @Override
        public void clearCache() {
        }

        @Override
        public int getPriority() {
            return 0;
        }
    }

    static class RequiredFieldsMetadataProvider implements MetadataProvider {

        @Override
        public EntityMetadata analyze(Class<?> entityClass) {
            Map<String, PropertyMetadata> properties = new LinkedHashMap<>();

            for (Field field : getAllFields(entityClass)) {
                boolean isRequired = field.getType() == String.class;

                PropertyMetadata prop = PropertyMetadata.builder()
                        .name(field.getName())
                        .field(field)
                        .type(field.getType())
                        .declaringClass(field.getDeclaringClass())
                        .required(isRequired)
                        .nullable(!isRequired)
                        .build();
                properties.put(field.getName(), prop);
            }

            return new EntityMetadata(entityClass, properties, false, null);
        }

        private List<Field> getAllFields(Class<?> clazz) {
            List<Field> fields = new ArrayList<>();
            while (clazz != null && clazz != Object.class) {
                fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
                clazz = clazz.getSuperclass();
            }
            return fields;
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return true;
        }

        @Override
        public void clearCache() {
        }

        @Override
        public int getPriority() {
            return 10;
        }
    }

    static class SelfReferencingMetadataProvider implements MetadataProvider {

        @Override
        public EntityMetadata analyze(Class<?> entityClass) {
            Map<String, PropertyMetadata> properties = new LinkedHashMap<>();

            for (Field field : entityClass.getDeclaredFields()) {
                PropertyMetadata.Builder builder = PropertyMetadata.builder()
                        .name(field.getName())
                        .field(field)
                        .type(field.getType())
                        .declaringClass(field.getDeclaringClass());

                if (field.getType() == SelfReferencingEntity.class) {
                    builder.association(true)
                            .associationType(PropertyMetadata.AssociationType.MANY_TO_ONE)
                            .associatedEntityType(SelfReferencingEntity.class)
                            .required(true);
                }

                properties.put(field.getName(), builder.build());
            }

            return new EntityMetadata(entityClass, properties, false, null);
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return entityClass == SelfReferencingEntity.class;
        }

        @Override
        public void clearCache() {
        }

        @Override
        public int getPriority() {
            return 100;
        }
    }

    static class DeepNestingMetadataProvider implements MetadataProvider {

        @Override
        public EntityMetadata analyze(Class<?> entityClass) {
            Map<String, PropertyMetadata> properties = new LinkedHashMap<>();

            for (Field field : entityClass.getDeclaredFields()) {
                PropertyMetadata.Builder builder = PropertyMetadata.builder()
                        .name(field.getName())
                        .field(field)
                        .type(field.getType())
                        .declaringClass(field.getDeclaringClass());

                if (field.getType() == Level2.class) {
                    builder.association(true)
                            .associationType(PropertyMetadata.AssociationType.MANY_TO_ONE)
                            .associatedEntityType(Level2.class)
                            .required(true);
                } else if (field.getType() == Level3.class) {
                    builder.association(true)
                            .associationType(PropertyMetadata.AssociationType.MANY_TO_ONE)
                            .associatedEntityType(Level3.class)
                            .required(true);
                }

                properties.put(field.getName(), builder.build());
            }

            return new EntityMetadata(entityClass, properties, false, null);
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return entityClass == Level1.class
                    || entityClass == Level2.class
                    || entityClass == Level3.class;
        }

        @Override
        public void clearCache() {
        }

        @Override
        public int getPriority() {
            return 100;
        }
    }
}
