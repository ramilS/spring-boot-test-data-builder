package com.example.jpa

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import spock.lang.Specification

/**
 * Base Spock specification with global TestData configuration.
 * Uses Java helper class because Groovy closures don't support method references.
 */
@DataJpaTest
abstract class BaseTestDataSpec extends Specification {

    def setupSpec() {
        TestDataConfiguration.configure()
    }
}
