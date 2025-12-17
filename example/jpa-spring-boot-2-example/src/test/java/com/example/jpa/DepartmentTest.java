package com.example.jpa;

import com.example.jpa.entity.Department;
import com.example.jpa.repository.DepartmentRepository;
import io.github.ramils.core.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentTest extends BaseTestDataTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void shouldCreateDepartmentWithDefaultName() {
        Department dept = TestData.create(Department.class);
        Department saved = departmentRepository.save(dept);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Department");
    }

    @Test
    void shouldCreateDepartmentWithCustomName() {
        Department dept = TestData.of(Department.class)
                .set(Department::getName, "Marketing")
                .create();

        Department saved = departmentRepository.save(dept);

        assertThat(saved.getName()).isEqualTo("Marketing");
    }

    @Test
    void shouldCreateDepartmentHierarchy() {
        Department parent = TestData.of(Department.class)
                .set(Department::getName, "Company")
                .create();
        departmentRepository.save(parent);

        Department child = TestData.of(Department.class)
                .set(Department::getName, "Engineering")
                .set(Department::getParent, parent)
                .create();
        departmentRepository.save(child);

        assertThat(child.getParent()).isNotNull();
        assertThat(child.getParent().getName()).isEqualTo("Company");
    }
}
