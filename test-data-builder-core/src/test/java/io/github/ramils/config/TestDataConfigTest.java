package io.github.ramils.config;

import io.github.ramils.core.TestData;
import io.github.ramils.metadata.EntityMetadata;
import io.github.ramils.metadata.MetadataProvider;
import io.github.ramils.metadata.PropertyMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDataConfigTest {

    @BeforeEach
    void setUp() {
        TestData.reset();
        TestData.registerProvider(new SimpleMetadataProvider());
    }

    @Nested
    @DisplayName("forType().set() - Fixed values")
    class FixedValueTests {

        @Test
        @DisplayName("Should apply fixed value from global config")
        void shouldApplyFixedValue() {
            TestData.configure(config -> config
                    .forType(User.class, defaults -> defaults
                            .set(User::getFirstName, "GlobalDefault")));

            User user = TestData.create(User.class);

            assertThat(user.getFirstName()).isEqualTo("GlobalDefault");
        }

        @Test
        @DisplayName("Builder override should take priority over global config")
        void builderOverrideTakesPriority() {
            TestData.configure(config -> config
                    .forType(User.class, defaults -> defaults
                            .set(User::getFirstName, "GlobalDefault")));

            User user = TestData.of(User.class)
                    .set(User::getFirstName, "BuilderOverride")
                    .create();

            assertThat(user.getFirstName()).isEqualTo("BuilderOverride");
        }

        @Test
        @DisplayName("Shorthand defaultValue should work")
        void shorthandDefaultValue() {
            TestData.configure(config -> config
                    .defaultValue(User.class, User::getLastName, "ShorthandValue"));

            User user = TestData.create(User.class);

            assertThat(user.getLastName()).isEqualTo("ShorthandValue");
        }
    }

    @Nested
    @DisplayName("forType().generate() - Dynamic values with Supplier")
    class GenerateTests {

        @Test
        @DisplayName("Should call supplier each time")
        void shouldCallSupplierEachTime() {
            AtomicInteger counter = new AtomicInteger(0);

            TestData.configure(config -> config
                    .forType(User.class, defaults -> defaults
                            .generate(User::getFirstName, () -> "User" + counter.incrementAndGet())));

            User user1 = TestData.create(User.class);
            User user2 = TestData.create(User.class);

            assertThat(user1.getFirstName()).isEqualTo("User1");
            assertThat(user2.getFirstName()).isEqualTo("User2");
        }

        @Test
        @DisplayName("Should generate random phone numbers")
        void shouldGenerateRandomPhoneNumbers() {
            // Simulates: .generate(Client::getPhone, () -> "+7987654" + RandomStringUtils.randomNumeric(4))
            TestData.configure(config -> config
                    .forType(Client.class, defaults -> defaults
                            .generate(Client::getPhone, () -> "+7987654" + randomNumeric(4))));

            Client client1 = TestData.create(Client.class);
            Client client2 = TestData.create(Client.class);

            assertThat(client1.getPhone()).startsWith("+7987654");
            assertThat(client1.getPhone()).hasSize(12); // +7987654 + 4 digits
            assertThat(client2.getPhone()).startsWith("+7987654");
            // Random values should be different (with high probability)
            assertThat(client1.getPhone()).isNotEqualTo(client2.getPhone());
        }

        @Test
        @DisplayName("Should combine sequence and generate")
        void shouldCombineSequenceAndGenerate() {
            AtomicInteger emailCounter = new AtomicInteger(0);

            TestData.configure(config -> config
                    .forType(Client.class, defaults -> defaults
                            .sequence(Client::getEmail, i -> "client_" + i + "@setinbox.com")
                            .generate(Client::getPhone, () -> "+7987654" + randomNumeric(4))));

            List<Client> clients = TestData.of(Client.class).createList(3);

            // Emails should be sequential
            assertThat(clients).extracting(Client::getEmail)
                    .containsExactly("client_0@setinbox.com", "client_1@setinbox.com", "client_2@setinbox.com");

            // Phones should all start with prefix but be different
            assertThat(clients).allMatch(c -> c.getPhone().startsWith("+7987654"));
            assertThat(clients).extracting(Client::getPhone).doesNotHaveDuplicates();
        }

        // Helper method to simulate RandomStringUtils.randomNumeric
        private String randomNumeric(int length) {
            StringBuilder sb = new StringBuilder();
            java.util.Random random = new java.util.Random();
            for (int i = 0; i < length; i++) {
                sb.append(random.nextInt(10));
            }
            return sb.toString();
        }
    }

    @Nested
    @DisplayName("forType().sequence() - Index-based generation")
    class SequenceTests {

        @Test
        @DisplayName("Should apply sequence values")
        void shouldApplySequence() {
            TestData.configure(config -> config
                    .forType(User.class, defaults -> defaults
                            .sequence(User::getEmail, i -> "user" + i + "@test.com")));

            List<User> users = TestData.of(User.class).createList(3);

            assertThat(users).extracting(User::getEmail)
                    .containsExactly("user0@test.com", "user1@test.com", "user2@test.com");
        }

        @Test
        @DisplayName("Shorthand defaultSequence should work")
        void shorthandDefaultSequence() {
            TestData.configure(config -> config
                    .defaultSequence(User.class, User::getEmail, i -> "seq" + i + "@mail.com"));

            List<User> users = TestData.of(User.class).createList(2);

            assertThat(users).extracting(User::getEmail)
                    .containsExactly("seq0@mail.com", "seq1@mail.com");
        }

        @Test
        @DisplayName("Builder sequence should override global sequence")
        void builderSequenceOverridesGlobal() {
            TestData.configure(config -> config
                    .forType(User.class, defaults -> defaults
                            .sequence(User::getEmail, i -> "global" + i + "@test.com")));

            List<User> users = TestData.of(User.class)
                    .sequence(User::getEmail, i -> "builder" + i + "@test.com")
                    .createList(2);

            assertThat(users).extracting(User::getEmail)
                    .containsExactly("builder0@test.com", "builder1@test.com");
        }
    }

    @Nested
    @DisplayName("forType().derive() - Computed values")
    class DeriveTests {

        @Test
        @DisplayName("Should compute derived value from other fields")
        void shouldComputeDerivedValue() {
            TestData.configure(config -> config
                    .forType(User.class, defaults -> defaults
                            .set(User::getFirstName, "John")
                            .set(User::getLastName, "Doe")
                            .derive(User::getEmail, u ->
                                    u.getFirstName().toLowerCase() + "." +
                                    u.getLastName().toLowerCase() + "@company.com")));

            User user = TestData.create(User.class);

            assertThat(user.getEmail()).isEqualTo("john.doe@company.com");
        }

        @Test
        @DisplayName("Shorthand defaultDerived should work")
        void shorthandDefaultDerived() {
            TestData.configure(config -> config
                    .defaultValue(User.class, User::getFirstName, "Jane")
                    .defaultValue(User.class, User::getLastName, "Smith")
                    .defaultDerived(User.class, User::getEmail, u ->
                            u.getFirstName() + "@" + u.getLastName() + ".com"));

            User user = TestData.create(User.class);

            assertThat(user.getEmail()).isEqualTo("Jane@Smith.com");
        }

        @Test
        @DisplayName("Builder set should override global derive")
        void builderSetOverridesGlobalDerive() {
            TestData.configure(config -> config
                    .forType(User.class, defaults -> defaults
                            .derive(User::getEmail, u -> "derived@test.com")));

            User user = TestData.of(User.class)
                    .set(User::getEmail, "explicit@test.com")
                    .create();

            assertThat(user.getEmail()).isEqualTo("explicit@test.com");
        }

        @Test
        @DisplayName("Should derive phone based on country - RU gets +7 prefix")
        void shouldDerivePhoneBasedOnCountry_RU() {
            // Example: if country == 'RU' then phone starts with '+7'
            TestData.configure(config -> config
                    .forType(Client.class, defaults -> defaults
                            .set(Client::getCountry, "RU")
                            .derive(Client::getPhone, client -> {
                                if ("RU".equals(client.getCountry())) {
                                    return "+7" + "9876543210";
                                } else if ("US".equals(client.getCountry())) {
                                    return "+1" + "5551234567";
                                } else {
                                    return "+00" + "0000000000";
                                }
                            })));

            Client client = TestData.create(Client.class);

            assertThat(client.getCountry()).isEqualTo("RU");
            assertThat(client.getPhone()).startsWith("+7");
        }

        @Test
        @DisplayName("Should derive phone based on country - US gets +1 prefix")
        void shouldDerivePhoneBasedOnCountry_US() {
            TestData.configure(config -> config
                    .forType(Client.class, defaults -> defaults
                            .set(Client::getCountry, "US")
                            .derive(Client::getPhone, client -> {
                                if ("RU".equals(client.getCountry())) {
                                    return "+7" + "9876543210";
                                } else if ("US".equals(client.getCountry())) {
                                    return "+1" + "5551234567";
                                } else {
                                    return "+00" + "0000000000";
                                }
                            })));

            Client client = TestData.create(Client.class);

            assertThat(client.getCountry()).isEqualTo("US");
            assertThat(client.getPhone()).startsWith("+1");
        }

        @Test
        @DisplayName("Builder should override country and derive phone accordingly")
        void builderShouldOverrideCountryAndDerivePhone() {
            // Global config sets RU
            TestData.configure(config -> config
                    .forType(Client.class, defaults -> defaults
                            .set(Client::getCountry, "RU")
                            .derive(Client::getPhone, client -> {
                                if ("RU".equals(client.getCountry())) {
                                    return "+7" + "9876543210";
                                } else if ("US".equals(client.getCountry())) {
                                    return "+1" + "5551234567";
                                } else {
                                    return "+00" + "0000000000";
                                }
                            })));

            // Builder overrides to US
            Client client = TestData.of(Client.class)
                    .set(Client::getCountry, "US")
                    .create();

            assertThat(client.getCountry()).isEqualTo("US");
            assertThat(client.getPhone()).startsWith("+1");
        }

        @Test
        @DisplayName("Should derive multiple fields based on each other")
        void shouldDeriveMultipleFieldsBasedOnEachOther() {
            TestData.configure(config -> config
                    .forType(Client.class, defaults -> defaults
                            .set(Client::getName, "Ivan Petrov")
                            .set(Client::getCountry, "RU")
                            .derive(Client::getEmail, client ->
                                    client.getName().toLowerCase().replace(" ", ".") + "@mail.ru")
                            .derive(Client::getPhone, client ->
                                    "RU".equals(client.getCountry()) ? "+79876543210" : "+15551234567")));

            Client client = TestData.create(Client.class);

            assertThat(client.getName()).isEqualTo("Ivan Petrov");
            assertThat(client.getCountry()).isEqualTo("RU");
            assertThat(client.getEmail()).isEqualTo("ivan.petrov@mail.ru");
            assertThat(client.getPhone()).isEqualTo("+79876543210");
        }
    }

    @Nested
    @DisplayName("forType().setNull() - Explicit nulls")
    class SetNullTests {

        @Test
        @DisplayName("Should set field to null explicitly")
        void shouldSetNull() {
            TestData.configure(config -> config
                    .forType(User.class, defaults -> defaults
                            .setNull(User::getMiddleName)));

            User user = TestData.create(User.class);

            assertThat(user.getMiddleName()).isNull();
        }
    }

    @Nested
    @DisplayName("abstractDefault() - Abstract type resolution")
    class AbstractDefaultTests {

        @Test
        @DisplayName("Should use concrete type for abstract class")
        void shouldUseConcreteType() {
            TestData.configure(config -> config
                    .abstractDefault(Payment.class, CreditCardPayment.class));

            Payment payment = TestData.create(Payment.class);

            assertThat(payment).isInstanceOf(CreditCardPayment.class);
        }

        @Test
        @DisplayName("Should throw for invalid abstract default")
        void shouldThrowForInvalidDefault() {
            assertThatThrownBy(() ->
                    TestData.configure(config -> config
                            .abstractDefault(Payment.class, Payment.class)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be different");
        }

        @Test
        @DisplayName("Should throw for non-subtype")
        void shouldThrowForNonSubtype() {
            assertThatThrownBy(() ->
                    TestData.configure(config -> config
                            .abstractDefault(Payment.class, User.class)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("is not a subtype");
        }
    }

    @Nested
    @DisplayName("maxDepth() - Nested entity depth limit")
    class MaxDepthTests {

        @Test
        @DisplayName("Should throw for invalid max depth")
        void shouldThrowForInvalidMaxDepth() {
            assertThatThrownBy(() ->
                    TestData.configure(config -> config.maxDepth(0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 1");
        }

        @Test
        @DisplayName("Should accept valid max depth")
        void shouldAcceptValidMaxDepth() {
            TestData.configure(config -> config.maxDepth(5));

            assertThat(TestData.getConfig().getMaxDepth()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("reset() - Configuration reset")
    class ResetTests {

        @Test
        @DisplayName("Reset should clear type defaults")
        void resetShouldClearDefaults() {
            TestData.configure(config -> config
                    .forType(User.class, defaults -> defaults
                            .set(User::getFirstName, "Configured")));

            TestData.reset();
            TestData.registerProvider(new SimpleMetadataProvider());

            User user = TestData.create(User.class);

            assertThat(user.getFirstName()).isNotEqualTo("Configured");
        }
    }

    // Test entities

    public static class User {
        private String firstName;
        private String lastName;
        private String middleName;
        private String email;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getMiddleName() { return middleName; }
        public void setMiddleName(String middleName) { this.middleName = middleName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class Client {
        private String name;
        private String email;
        private String phone;
        private String country;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
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

    // Simple MetadataProvider for testing

    static class SimpleMetadataProvider implements MetadataProvider {

        @Override
        public EntityMetadata analyze(Class<?> entityClass) {
            Map<String, PropertyMetadata> properties = new HashMap<>();

            for (Field field : getAllFields(entityClass)) {
                PropertyMetadata prop = PropertyMetadata.builder()
                        .name(field.getName())
                        .field(field)
                        .type(field.getType())
                        .declaringClass(field.getDeclaringClass())
                        .required(true)
                        .nullable(true)
                        .build();
                properties.put(field.getName(), prop);
            }

            return new EntityMetadata(entityClass, properties, false, null);
        }

        private List<Field> getAllFields(Class<?> clazz) {
            List<Field> fields = new java.util.ArrayList<>();
            while (clazz != null && clazz != Object.class) {
                fields.addAll(java.util.Arrays.asList(clazz.getDeclaredFields()));
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
}
