package com.example.jdbc;

import com.example.jdbc.entity.User;
import com.example.jdbc.repository.UserRepository;
import io.github.ramils.core.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcTestDataTest extends BaseTestDataTest {

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("Basic Usage")
    class BasicUsage {

        @Test
        void createWithDefaults() {
            User user = TestData.create(User.class);
            User saved = userRepository.save(user);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getFirstName()).isEqualTo("Test");
            assertThat(saved.getLastName()).isEqualTo("User");
            assertThat(saved.getEmail()).isEqualTo("user0@test.com");
        }

        @Test
        void overrideDefaults() {
            User user = TestData.of(User.class)
                    .set(User::getFirstName, "John")
                    .set(User::getLastName, "Doe")
                    .create();

            User saved = userRepository.save(user);

            assertThat(saved.getFirstName()).isEqualTo("John");
            assertThat(saved.getLastName()).isEqualTo("Doe");
            assertThat(saved.getEmail()).contains("@");
        }

        @Test
        void createWithMap() {
            User user = TestData.of(User.class)
                    .setAll(Map.of("firstName", "Alice", "lastName", "Smith"))
                    .create();

            User saved = userRepository.save(user);

            assertThat(saved.getFirstName()).isEqualTo("Alice");
            assertThat(saved.getLastName()).isEqualTo("Smith");
        }
    }

    @Nested
    @DisplayName("Lists and Sequences")
    class ListsAndSequences {

        @Test
        void createListWithSequence() {
            List<User> users = TestData.of(User.class).createList(3);

            userRepository.saveAll(users);

            assertThat(users).extracting(User::getEmail)
                    .containsExactly("user0@test.com", "user1@test.com", "user2@test.com");
        }

        @Test
        void overrideSequence() {
            List<User> users = TestData.of(User.class)
                    .sequence(User::getEmail, i -> "custom" + i + "@example.com")
                    .createList(3);

            userRepository.saveAll(users);

            assertThat(users).extracting(User::getEmail)
                    .containsExactly("custom0@example.com", "custom1@example.com", "custom2@example.com");
        }
    }

    @Nested
    @DisplayName("Derived Values")
    class DerivedValues {

        @Test
        void deriveEmail() {
            User user = TestData.of(User.class)
                    .set(User::getFirstName, "John")
                    .set(User::getLastName, "Doe")
                    .derive(User::getEmail, u ->
                            u.getFirstName().toLowerCase() + "." +
                            u.getLastName().toLowerCase() + "@company.com")
                    .create();

            User saved = userRepository.save(user);

            assertThat(saved.getEmail()).isEqualTo("john.doe@company.com");
        }
    }
}
