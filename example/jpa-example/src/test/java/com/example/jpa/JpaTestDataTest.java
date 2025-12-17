package com.example.jpa;

import com.example.jpa.entity.Department;
import com.example.jpa.entity.User;
import com.example.jpa.repository.DepartmentRepository;
import com.example.jpa.repository.UserRepository;
import io.github.ramils.core.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JpaTestDataTest extends BaseTestDataTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

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

        @Test
        void deriveEmailFromDepartmentName() {
            Department dept = TestData.of(Department.class)
                    .set(Department::getName, "Engineering")
                    .create();
            departmentRepository.save(dept);

            User user = TestData.of(User.class)
                    .set(User::getFirstName, "John")
                    .set(User::getDepartment, dept)
                    .derive(User::getEmail, u ->
                            u.getFirstName().toLowerCase() + "@" +
                            u.getDepartment().getName().toLowerCase() + ".company.com")
                    .create();

            userRepository.save(user);

            assertThat(user.getEmail()).isEqualTo("john@engineering.company.com");
        }

        @Test
        void deriveFromGeneratedDepartment() {
            Department dept = TestData.of(Department.class)
                    .set(Department::getName, "AutoDept")
                    .create();

            User user = TestData.of(User.class)
                    .set(User::getFirstName, "Alice")
                    .set(User::getDepartment, dept)
                    .derive(User::getEmail, u ->
                            u.getFirstName().toLowerCase() + "@" +
                            u.getDepartment().getName().toLowerCase() + ".auto.com")
                    .create();

            assertThat(user.getDepartment()).isNotNull();
            assertThat(user.getEmail()).isEqualTo("alice@autodept.auto.com");
        }

        @Test
        void deriveWithNullCheck() {
            User user = TestData.of(User.class)
                    .set(User::getFirstName, "Bob")
                    .derive(User::getEmail, u -> {
                        if (u.getDepartment() != null) {
                            return u.getFirstName().toLowerCase() + "@" +
                                   u.getDepartment().getName().toLowerCase() + ".company.com";
                        }
                        return u.getFirstName().toLowerCase() + "@company.com";
                    })
                    .create();

            assertThat(user.getDepartment()).isNull();
            assertThat(user.getEmail()).isEqualTo("bob@company.com");
        }
    }

    @Nested
    @DisplayName("Associations")
    class Associations {

        @Test
        void createWithDepartment() {
            Department dept = TestData.of(Department.class)
                    .set(Department::getName, "Engineering")
                    .create();
            departmentRepository.save(dept);

            User user = TestData.of(User.class)
                    .set(User::getDepartment, dept)
                    .create();
            userRepository.save(user);

            assertThat(user.getDepartment()).isNotNull();
            assertThat(user.getDepartment().getName()).isEqualTo("Engineering");
        }
    }

    @Nested
    @DisplayName("Consecutive Creates")
    class ConsecutiveCreates {

        @Test
        void createConsecutiveEntities() {
            long timestamp = System.nanoTime();

            User user1 = TestData.of(User.class)
                    .set(User::getEmail, "consecutive1_" + timestamp + "@test.com")
                    .create();
            User saved1 = userRepository.save(user1);

            User user2 = TestData.of(User.class)
                    .set(User::getEmail, "consecutive2_" + timestamp + "@test.com")
                    .create();
            User saved2 = userRepository.save(user2);

            User user3 = TestData.of(User.class)
                    .set(User::getEmail, "consecutive3_" + timestamp + "@test.com")
                    .create();
            User saved3 = userRepository.save(user3);

            assertThat(saved1.getId()).isNotNull();
            assertThat(saved2.getId()).isNotNull();
            assertThat(saved3.getId()).isNotNull();

            assertThat(saved1.getFirstName()).isEqualTo("Test");
            assertThat(saved2.getFirstName()).isEqualTo("Test");
            assertThat(saved3.getFirstName()).isEqualTo("Test");
        }

        @Test
        void mixDefaultsAndOverrides() {
            long timestamp = System.nanoTime();

            User user1 = TestData.of(User.class)
                    .set(User::getEmail, "mix1_" + timestamp + "@test.com")
                    .create();
            userRepository.save(user1);

            User user2 = TestData.of(User.class)
                    .set(User::getFirstName, "Custom")
                    .set(User::getEmail, "mix2_" + timestamp + "@test.com")
                    .create();
            userRepository.save(user2);

            User user3 = TestData.of(User.class)
                    .set(User::getEmail, "mix3_" + timestamp + "@test.com")
                    .create();
            userRepository.save(user3);

            assertThat(user1.getFirstName()).isEqualTo("Test");
            assertThat(user1.getLastName()).isEqualTo("User");

            assertThat(user2.getFirstName()).isEqualTo("Custom");
            assertThat(user2.getLastName()).isEqualTo("User");

            assertThat(user3.getFirstName()).isEqualTo("Test");
        }
    }
}
