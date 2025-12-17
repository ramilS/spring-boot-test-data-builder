package com.example.jpa;

import com.example.jpa.entity.Department;
import com.example.jpa.entity.User;
import io.github.ramils.core.TestData;
import io.github.ramils.jpa.JpaMetadataProvider;

/**
 * Java helper for TestData configuration in Spock tests.
 * Required because Groovy closures don't support method references.
 */
public final class TestDataConfiguration {

    private TestDataConfiguration() {
    }

    public static void configure() {
        TestData.reset();
        TestData.registerProvider(new JpaMetadataProvider());

        TestData.configure(config -> config
                .forType(User.class, defaults -> defaults
                        .set(User::getFirstName, "Test")
                        .set(User::getLastName, "User")
                        .sequence(User::getEmail, i -> "user" + i + "@test.com"))
                .forType(Department.class, defaults -> defaults
                        .set(Department::getName, "Test Department"))
                .maxDepth(5)
        );
    }

    public static void configureWithCustomDefaults(
            String defaultFirstName,
            String defaultLastName,
            String emailDomain) {

        TestData.reset();
        TestData.registerProvider(new JpaMetadataProvider());

        TestData.configure(config -> config
                .forType(User.class, defaults -> defaults
                        .set(User::getFirstName, defaultFirstName)
                        .set(User::getLastName, defaultLastName)
                        .sequence(User::getEmail, i -> "user" + i + "@" + emailDomain))
                .forType(Department.class, defaults -> defaults
                        .sequence(Department::getName, i -> "Department " + i))
                .maxDepth(5)
        );
    }

    public static void configureWithDerivedValues() {
        TestData.reset();
        TestData.registerProvider(new JpaMetadataProvider());

        TestData.configure(config -> config
                .forType(User.class, defaults -> defaults
                        .set(User::getFirstName, "John")
                        .set(User::getLastName, "Doe")
                        .derive(User::getEmail, user ->
                                user.getFirstName().toLowerCase() + "." +
                                user.getLastName().toLowerCase() + "@company.com"))
                .forType(Department.class, defaults -> defaults
                        .set(Department::getName, "Engineering"))
                .maxDepth(5)
        );
    }
}
