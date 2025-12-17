package com.example.jpa;

import com.example.jpa.entity.Department;
import com.example.jpa.repository.DepartmentRepository;
import io.github.ramils.core.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentTest extends BaseTestDataTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void createWithDefaults() {
        Department dept = TestData.create(Department.class);
        Department saved = departmentRepository.save(dept);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Department");
    }

    @Test
    void overrideName() {
        Department dept = TestData.of(Department.class)
                .set(Department::getName, "Engineering")
                .create();

        Department saved = departmentRepository.save(dept);

        assertThat(saved.getName()).isEqualTo("Engineering");
    }

    @Test
    void createMultiple() {
        List<Department> departments = TestData.of(Department.class)
                .sequence(Department::getName, i -> "Department " + i)
                .createList(3);

        departmentRepository.saveAll(departments);

        assertThat(departments).extracting(Department::getName)
                .containsExactly("Department 0", "Department 1", "Department 2");
    }
}
