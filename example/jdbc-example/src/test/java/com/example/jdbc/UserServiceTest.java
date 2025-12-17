package com.example.jdbc;

import com.example.jdbc.entity.User;
import com.example.jdbc.repository.UserRepository;
import io.github.ramils.core.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Another test class that extends BaseTestDataTest.
 * <p>
 * Demonstrates that global configuration from BaseTestDataTest
 * is shared across all test classes that extend it.
 */
class UserServiceTest extends BaseTestDataTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Find user by email")
    void findByEmail() {
        User user = TestData.of(User.class)
                .set(User::getEmail, "john@example.com")
                .create();
        userRepository.save(user);

        // Uses global defaults for firstName and lastName
        assertThat(user.getFirstName()).isEqualTo("Test");
        assertThat(user.getLastName()).isEqualTo("User");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Create users with unique emails")
    void createUsersWithUniqueEmails() {
        List<User> users = TestData.of(User.class).createList(5);

        users.forEach(userRepository::save);

        // All users have unique emails from sequence
        assertThat(users).extracting(User::getEmail)
                .containsExactly(
                        "user0@test.com",
                        "user1@test.com",
                        "user2@test.com",
                        "user3@test.com",
                        "user4@test.com"
                );

        // All users have same defaults for other fields
        assertThat(users).extracting(User::getFirstName)
                .containsOnly("Test");
        assertThat(users).extracting(User::getLastName)
                .containsOnly("User");
    }

    @Test
    @DisplayName("Override all defaults")
    void overrideAllDefaults() {
        User user = TestData.of(User.class)
                .set(User::getFirstName, "Alice")
                .set(User::getLastName, "Smith")
                .set(User::getEmail, "alice.smith@company.com")
                .create();

        userRepository.save(user);

        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getEmail()).isEqualTo("alice.smith@company.com");
    }
}
