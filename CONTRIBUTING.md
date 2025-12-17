# Contributing

Thank you for your interest in contributing to Spring Boot Test Data Builder!

## How to Contribute

1. **Fork** the repository
2. **Create a branch** from `dev` for your changes
3. **Make your changes** and add tests
4. **Run the full build** to ensure everything passes:
   ```bash
   ./gradlew build
   ```
5. **Submit a Pull Request** to the `dev` branch

## Development Setup

### Prerequisites

- Java 17+
- Gradle 8.5+ (or use the included Gradle wrapper)

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Code Style

This project uses [Checkstyle](https://checkstyle.org/) for code style enforcement. The build will fail if there are any checkstyle violations.

Check style issues before committing:
```bash
./gradlew checkstyleMain
```

## Pull Request Guidelines

- All PRs must target the `dev` branch (not `master`)
- All tests must pass
- Checkstyle must pass with zero warnings and zero errors
- Add tests for new functionality
- Keep changes focused — one feature or fix per PR
- Write clear commit messages

## Reporting Issues

- Use [GitHub Issues](https://github.com/ramilS/spring-boot-test-data-builder/issues)
- Include a minimal reproducible example when reporting bugs
- Describe expected vs. actual behavior

## Project Structure

```
test-data-builder-core/              # Core framework (no persistence deps)
test-data-builder-jpa/               # JPA metadata provider
test-data-builder-spring-data-jdbc/  # Spring Data JDBC metadata provider
test-data-builder-spring-boot-starter/  # Spring Boot auto-configuration
example/                             # Example applications
```

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
