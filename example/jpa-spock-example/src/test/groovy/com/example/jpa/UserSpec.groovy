package com.example.jpa

import com.example.jpa.entity.Department
import com.example.jpa.entity.User
import com.example.jpa.repository.DepartmentRepository
import com.example.jpa.repository.UserRepository
import io.github.ramils.core.TestData
import org.springframework.beans.factory.annotation.Autowired

class UserSpec extends BaseTestDataSpec {

    @Autowired
    UserRepository userRepository

    @Autowired
    DepartmentRepository departmentRepository

    def "should create user with global configured defaults"() {
        when:
        def user = TestData.create(User)
        def saved = userRepository.save(user)

        then:
        saved.id != null
        saved.firstName == "Test"
        saved.lastName == "User"
        saved.email.contains("@test.com")
    }

    def "should create users with sequential emails"() {
        when:
        def users = TestData.of(User).createList(3)
        userRepository.saveAll(users)

        then:
        users.every { it.email.endsWith("@test.com") }
        users*.email.unique().size() == 3
    }

    def "should create consecutive users"() {
        when:
        def user1 = TestData.of(User)
                .setAll([email: "consecutive1_${System.nanoTime()}@test.com".toString()])
                .create()
        def saved1 = userRepository.save(user1)

        def user2 = TestData.of(User)
                .setAll([email: "consecutive2_${System.nanoTime()}@test.com".toString()])
                .create()
        def saved2 = userRepository.save(user2)

        def user3 = TestData.of(User)
                .setAll([email: "consecutive3_${System.nanoTime()}@test.com".toString()])
                .create()
        def saved3 = userRepository.save(user3)

        then:
        saved1.id != null
        saved2.id != null
        saved3.id != null

        and:
        saved1.email != saved2.email
        saved2.email != saved3.email

        and:
        [saved1, saved2, saved3].every { it.firstName == "Test" }
        [saved1, saved2, saved3].every { it.lastName == "User" }
    }

    def "should mix defaults and overrides"() {
        when:
        def timestamp = System.nanoTime()

        def user1 = TestData.of(User)
                .setAll([email: "mix_user1_${timestamp}@test.com".toString()])
                .create()
        userRepository.save(user1)

        def user2 = TestData.of(User)
                .setAll([firstName: "Custom", email: "mix_user2_${timestamp}@test.com".toString()])
                .create()
        userRepository.save(user2)

        def user3 = TestData.of(User)
                .setAll([email: "mix_user3_${timestamp}@test.com".toString()])
                .create()
        userRepository.save(user3)

        then:
        user1.firstName == "Test"
        user1.lastName == "User"

        and:
        user2.firstName == "Custom"
        user2.lastName == "User"

        and:
        [user1, user2, user3]*.email.unique().size() == 3
    }

    def "should create user with custom values"() {
        when:
        def user = TestData.of(User)
                .setAll([
                    firstName: "John",
                    lastName: "Doe",
                    email: "john.doe@example.com"
                ])
                .create()
        def saved = userRepository.save(user)

        then:
        saved.firstName == "John"
        saved.lastName == "Doe"
        saved.email == "john.doe@example.com"
    }

    def "should create user with partial overrides"() {
        when:
        def user = TestData.of(User)
                .setAll([firstName: "Alice", lastName: "Smith"])
                .create()
        def saved = userRepository.save(user)

        then:
        saved.firstName == "Alice"
        saved.lastName == "Smith"
        saved.email.contains("@")
    }

    def "should create multiple users"() {
        when:
        def users = TestData.of(User).createList(3)
        userRepository.saveAll(users)

        then:
        users.size() == 3
        users*.email.unique().size() == 3
    }

    def "should create user with department"() {
        given:
        def dept = TestData.of(Department)
                .setAll([name: "Engineering"])
                .create()
        departmentRepository.save(dept)

        when:
        def user = TestData.of(User)
                .setAll([
                    firstName: "John",
                    lastName: "Doe",
                    email: "john@eng.com",
                    department: dept
                ])
                .create()
        userRepository.save(user)

        then:
        user.department != null
        user.department.name == "Engineering"
    }

    def "should create users with data table"() {
        when:
        def user = TestData.of(User)
                .setAll([firstName: firstName, lastName: lastName, email: email])
                .create()
        userRepository.save(user)

        then:
        user.firstName == firstName
        user.lastName == lastName
        user.email == email

        where:
        firstName | lastName  | email
        "Alice"   | "Admin"   | "alice@admin.com"
        "Bob"     | "Manager" | "bob@manager.com"
        "Charlie" | "User"    | "charlie@user.com"
    }

    def "should create user with Groovy map syntax"() {
        when:
        def user = TestData.of(User)
                .setAll(firstName: "Groovy", lastName: "Style", email: "groovy@test.com")
                .create()
        def saved = userRepository.save(user)

        then:
        saved.firstName == "Groovy"
        saved.lastName == "Style"
        saved.email == "groovy@test.com"
    }
}
