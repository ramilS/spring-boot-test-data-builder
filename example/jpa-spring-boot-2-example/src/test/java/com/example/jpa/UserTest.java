package com.example.jpa;

import com.example.jpa.entity.Department;
import com.example.jpa.entity.User;
import com.example.jpa.repository.DepartmentRepository;
import com.example.jpa.repository.UserRepository;
import io.github.ramils.core.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest extends BaseTestDataTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void shouldCreateUserWithDefaultValues() {
        User user = TestData.create(User.class);
        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("Test");
        assertThat(saved.getLastName()).isEqualTo("User");
        assertThat(saved.getEmail()).contains("@test.com");
    }

    @Test
    void shouldCreateUserWithCustomValues() {
        User user = TestData.of(User.class)
                .set(User::getFirstName, "John")
                .set(User::getLastName, "Doe")
                .set(User::getEmail, "john.doe@example.com")
                .create();

        User saved = userRepository.save(user);

        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void shouldCreateMultipleUsers() {
        List<User> users = TestData.of(User.class).createList(3);

        userRepository.saveAll(users);

        assertThat(users).hasSize(3);
        assertThat(users.stream().map(User::getEmail).distinct().count()).isEqualTo(3);
    }

    @Test
    void shouldCreateUserWithDepartment() {
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

    @Test
    void shouldCreateConsecutiveUsers() {
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
    void shouldMixDefaultsAndOverrides() {
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
