# Test Data Builder - Usage Examples

## Quick Start

```java
// Simple creation - all required fields auto-generated
User user = TestData.create(User.class);

// With customizations
User user = TestData.of(User.class)
        .set(User::getFirstName, "John")
        .set(User::getEmail, "john@example.com")
        .create();
```

---

## Spring Boot Integration

### Dependencies (Gradle)

```gradle
testImplementation 'io.github.ramils:test-data-builder-spring-boot-starter:0.1.0-SNAPSHOT'
```

The starter includes both JPA and Spring Data JDBC support. Auto-configuration automatically detects which persistence technology you're using and registers the appropriate provider.

### Slice Tests

For `@DataJpaTest` or `@DataJdbcTest`, add the auto-configuration import:

```java
@DataJpaTest
@ImportAutoConfiguration(TestDataAutoConfiguration.class)
class MyRepositoryTest {
    // ...
}
```

---

## Global Configuration (DSL)

Configure defaults once (e.g., in `@BeforeAll` or a base test class):

```java
TestData.configure(config -> config
    .forType(User.class, defaults -> defaults
        .set(User::getFirstName, "Test")
        .set(User::getLastName, "User")
        .sequence(User::getEmail, i -> "user" + i + "@test.com")
        .derive(User::getFullName, u ->
            u.getFirstName() + " " + u.getLastName()))
    .forType(Order.class, defaults -> defaults
        .set(Order::getStatus, OrderStatus.PENDING)
        .generate(Order::getCreatedAt, () -> LocalDateTime.now()))
    .abstractDefault(Payment.class, CreditCardPayment.class)
    .maxDepth(5)
);

// All created Users now use these defaults
User user = TestData.create(User.class);
// user.getFirstName() -> "Test"
// user.getEmail() -> "user0@test.com"

// Builder overrides take priority over global config
User custom = TestData.of(User.class)
    .set(User::getFirstName, "Custom")
    .create();
// custom.getFirstName() -> "Custom" (not "Test")
```

### TypeDefaults Methods

| Method                       | Description                      | Example                                                             |
|------------------------------|----------------------------------|---------------------------------------------------------------------|
| `set(getter, value)`         | Fixed value for all instances    | `.set(User::getRole, Role.USER)`                                    |
| `generate(getter, supplier)` | Dynamic value (called each time) | `.generate(User::getCreatedAt, () -> Instant.now())`                |
| `sequence(getter, indexFn)`  | Index-based value (0, 1, 2...)   | `.sequence(User::getEmail, i -> "user" + i + "@test.com")`          |
| `derive(getter, entityFn)`   | Computed from other fields       | `.derive(User::getFullName, u -> u.getFirst() + " " + u.getLast())` |
| `setNull(getter)`            | Explicitly set to null           | `.setNull(User::getDeletedAt)`                                      |

### Configuration Options

```java
TestData.configure(config -> config
    // Fixed value for all instances
    .forType(User.class, d -> d.set(User::getRole, Role.USER))

    // Supplier - called each time (e.g., for timestamps, UUIDs)
    .forType(User.class, d -> d.generate(User::getCreatedAt, () -> Instant.now()))
    .forType(User.class, d -> d.generate(User::getId, () -> UUID.randomUUID()))

    // Sequence - index-based generation (0, 1, 2, ...)
    .forType(User.class, d -> d.sequence(User::getEmail, i -> "user" + i + "@test.com"))

    // Derived - computed from other fields after they are set
    .forType(User.class, d -> d.derive(User::getUsername, u ->
        u.getEmail().split("@")[0]))

    // Explicitly set to null (prevents auto-generation)
    .forType(User.class, d -> d.setNull(User::getMiddleName))

    // Default implementation for abstract types/interfaces
    .abstractDefault(Payment.class, CreditCardPayment.class)

    // Max depth for nested entity creation
    .maxDepth(10)
);
```

### Shorthand Methods

```java
// These are equivalent:
TestData.configure(c -> c.forType(User.class, d -> d.set(User::getName, "Test")));
TestData.configure(c -> c.defaultValue(User.class, User::getName, "Test"));

// Sequence shorthand
TestData.configure(c -> c.defaultSequence(User.class, User::getEmail, i -> "u" + i + "@test.com"));

// Derived shorthand
TestData.configure(c -> c.defaultDerived(User.class, User::getFullName, u -> u.getFirst() + " " + u.getLast()));

// Generator shorthand
TestData.configure(c -> c.defaultGenerator(User.class, User::getCreatedAt, () -> Instant.now()));
```

### Reset Configuration

```java
@BeforeEach
void setUp() {
    TestData.reset(); // Clears type defaults, keeps MetadataProvider
}
```

---

## Basic Object Creation

```java
// Create object with auto-generated values
User user = TestData.create(User.class);

// Create object with fluent builder
User user = TestData.of(User.class)
        .set(User::getFirstName, "John")
        .set(User::getLastName, "Doe")
        .create();
```

## Setting Values

```java
// Single field (type-safe via method reference)
User user = TestData.of(User.class)
        .set(User::getEmail, "john@example.com")
        .create();

// Multiple fields via Map (useful for dynamic/external data)
User user = TestData.of(User.class)
        .setAll(Map.of(
            "firstName", "Alice",
            "lastName", "Smith",
            "email", "alice@example.com"
        ))
        .create();

// Explicitly set to null
User user = TestData.of(User.class)
        .setNull(User::getMiddleName)
        .create();
```

## Derived Values

```java
// Derive field value from other fields
User user = TestData.of(User.class)
        .set(User::getFirstName, "John")
        .set(User::getLastName, "Doe")
        .derive(User::getEmail, u ->
                u.getFirstName().toLowerCase() + "." +
                u.getLastName().toLowerCase() + "@company.com")
        .create();
// email = "john.doe@company.com"
```

## Creating Multiple Objects

```java
// Create list with default values
List<User> users = TestData.of(User.class)
        .createList(5);

// Create list with sequential values
List<User> users = TestData.of(User.class)
        .sequence(User::getEmail, i -> "user" + i + "@test.com")
        .createList(3);
// emails: "user0@test.com", "user1@test.com", "user2@test.com"

// Combine sequence with other settings
List<User> users = TestData.of(User.class)
        .set(User::getDepartment, dept)
        .sequence(User::getFirstName, i -> "Employee" + i)
        .derive(User::getEmail, u -> u.getFirstName().toLowerCase() + "@company.com")
        .createList(10);
```

## Associations

```java
// Create related entities
Department dept = TestData.of(Department.class)
        .set(Department::getName, "Engineering")
        .create();

User user = TestData.of(User.class)
        .set(User::getDepartment, dept)
        .create();
```

---

## Priority Rules

When the same property is configured in multiple places, the priority is:

1. **Builder-level** (`TestData.of().set()`) - highest priority
2. **Global config** (`TestData.configure().forType()`) - lower priority
3. **Auto-generated** - lowest priority (used when no config exists)

```java
// Global config
TestData.configure(c -> c.forType(User.class, d -> d.set(User::getFirstName, "Global")));

// Builder override wins
User user = TestData.of(User.class)
        .set(User::getFirstName, "Builder")
        .create();
// user.getFirstName() -> "Builder"

// Without override, global config is used
User user2 = TestData.create(User.class);
// user2.getFirstName() -> "Global"
```
