package com.example.jdbc;

import com.example.jdbc.entity.User;
import io.github.ramils.core.TestData;
import io.github.ramils.jdbc.JdbcMetadataProvider;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;

@DataJdbcTest
public abstract class BaseTestDataTest {

    @BeforeAll
    static void configureTestData() {
        TestData.reset();
        TestData.registerProvider(new JdbcMetadataProvider());

        TestData.configure(config -> config
                .forType(User.class, defaults -> defaults
                        .set(User::getFirstName, "Test")
                        .set(User::getLastName, "User")
                        .sequence(User::getEmail, i -> "user" + i + "@test.com"))
                .maxDepth(5)
        );
    }
}
