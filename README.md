# Spring Boot Test Data Builder

[![CI](https://github.com/ramilS/spring-boot-test-data-builder/actions/workflows/ci.yml/badge.svg)](https://github.com/ramilS/spring-boot-test-data-builder/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/projects/jdk/17/)

A fluent, type-safe test data builder for JPA, Spring Data JDBC, and Spring Boot applications. Auto-generates valid test entities respecting constraints, associations, and validation annotations — so you can focus on what matters in your tests.

## Features

- **Type-safe API** — use method references (`User::getEmail`) instead of strings
- **Auto-generation** — fills required fields with valid values based on JPA/validation annotations
- **Constraint-aware** — respects `@Size`, `@Min`, `@Max`, `@Pattern`, `@Email`, `@NotNull`, etc.
- **Association handling** — auto-creates related entities for `@ManyToOne`, `@OneToOne`, etc.
- **Sequence generation** — create lists of entities with sequential values
- **Derived values** — compute field values from other fields
- **Global configuration** — define defaults once, override per-test
- **Spring Boot auto-configuration** — zero setup with the starter
- **Multiple persistence support** — JPA (Jakarta & Javax), Spring Data JDBC

## Quick Start

### Add Dependency

Check the latest version on the [Releases](https://github.com/ramilS/spring-boot-test-data-builder/releases) page.

**Gradle:**
```groovy
testImplementation 'io.github.ramils:test-data-builder-spring-boot-starter:0.1.0'
```

**Maven:**

```xml
<dependency>
    <groupId>io.github.ramils</groupId>
    <artifactId>test-data-builder-spring-boot-starter</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```

### Use It

```java
// Create with all required fields auto-generated
User user = TestData.create(User.class);

// Customize specific fields
User user = TestData.of(User.class)
        .set(User::getFirstName, "John")
        .set(User::getEmail, "john@example.com")
        .create();

// Create a list with sequential values
List<User> users = TestData.of(User.class)
        .sequence(User::getEmail, i -> "user" + i + "@test.com")
        .createList(10);

// Derive values from other fields
User user = TestData.of(User.class)
        .set(User::getFirstName, "John")
        .set(User::getLastName, "Doe")
        .derive(User::getEmail, u ->
                u.getFirstName().toLowerCase() + "." +
                u.getLastName().toLowerCase() + "@company.com")
        .create();
```

### Spring Boot Integration

For `@DataJpaTest` or `@DataJdbcTest`:

```java
@DataJpaTest
@ImportAutoConfiguration(TestDataAutoConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindByEmail() {
        User user = TestData.of(User.class)
                .set(User::getEmail, "test@example.com")
                .create();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
    }
}
```

### Global Configuration

```java
@BeforeAll
static void configureTestData() {
    TestData.configure(config -> config
        .forType(User.class, defaults -> defaults
            .set(User::getRole, Role.USER)
            .sequence(User::getEmail, i -> "user" + i + "@test.com")
            .derive(User::getFullName, u ->
                u.getFirstName() + " " + u.getLastName()))
        .maxDepth(5)
    );
}
```

## Modules

| Module | Description |
|--------|-------------|
| `test-data-builder-core` | Core framework — no persistence dependencies |
| `test-data-builder-jpa` | JPA support (`jakarta.persistence` & `javax.persistence`) |
| `test-data-builder-spring-data-jdbc` | Spring Data JDBC support |
| `test-data-builder-spring-boot-starter` | Spring Boot 3.x auto-configuration |

## Documentation

See [USAGE.md](USAGE.md) for the full API reference including:
- All builder methods (`set`, `generate`, `sequence`, `derive`, `setNull`)
- Global configuration DSL
- Association handling
- Priority rules
- Reset and test isolation patterns

## Examples

Working examples are in the [`example/`](example/) directory:

- **[jpa-example](example/jpa-example)** — JPA with Spring Boot 3.x
- **[jdbc-example](example/jdbc-example)** — Spring Data JDBC
- **[jpa-spock-example](example/jpa-spock-example)** — Groovy/Spock integration
- **[jpa-spring-boot-2-example](example/jpa-spring-boot-2-example)** — Legacy Spring Boot 2.x

## Requirements

- Java 17+
- Gradle 8.5+

## Building

```bash
./gradlew build
```

Run tests only:

```bash
./gradlew test
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
