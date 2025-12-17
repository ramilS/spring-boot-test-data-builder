package com.example.jdbc.repository;

import com.example.jdbc.entity.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
