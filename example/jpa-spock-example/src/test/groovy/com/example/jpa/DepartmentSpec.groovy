package com.example.jpa

import com.example.jpa.entity.Department
import com.example.jpa.repository.DepartmentRepository
import io.github.ramils.core.TestData
import org.springframework.beans.factory.annotation.Autowired

class DepartmentSpec extends BaseTestDataSpec {

    @Autowired
    DepartmentRepository departmentRepository

    def "should create department with global default name"() {
        when:
        def dept = TestData.create(Department)
        def saved = departmentRepository.save(dept)

        then:
        saved.id != null
        saved.name == "Test Department"
    }

    def "should override global default"() {
        when:
        def dept = TestData.of(Department)
                .setAll([name: "Custom Department"])
                .create()
        def saved = departmentRepository.save(dept)

        then:
        saved.name == "Custom Department"
    }

    def "should create multiple departments"() {
        when:
        def departments = (0..2).collect { i ->
            TestData.of(Department)
                    .setAll([name: "Department ${i}".toString()])
                    .create()
        }
        departmentRepository.saveAll(departments)

        then:
        departments*.name == ["Department 0", "Department 1", "Department 2"]
    }

    def "should create department hierarchy"() {
        given:
        def parent = TestData.of(Department)
                .setAll([name: "Company"])
                .create()
        departmentRepository.save(parent)

        when:
        def child = TestData.of(Department)
                .setAll([name: "Engineering", parent: parent])
                .create()
        departmentRepository.save(child)

        then:
        child.parent != null
        child.parent.name == "Company"
    }

    def "should create departments with data table"() {
        when:
        def dept = TestData.of(Department)
                .setAll([name: name])
                .create()
        departmentRepository.save(dept)

        then:
        dept.name == name

        where:
        name << ["Engineering", "Marketing", "Sales", "HR"]
    }
}
