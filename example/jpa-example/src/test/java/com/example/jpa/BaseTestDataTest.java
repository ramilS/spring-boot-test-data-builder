package com.example.jpa;

import com.example.jpa.entity.Department;
import com.example.jpa.entity.User;
import io.github.ramils.core.TestData;
import io.github.ramils.jpa.JpaMetadataProvider;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public abstract class BaseTestDataTest {

    @BeforeAll
    static void configureTestData() {
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
}
