package com.gameflix.userservice.repository;

import com.gameflix.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Used only by the integration tests to clean up their own throwaway users
    @Transactional
    void deleteByEmailEndingWith(String suffix);
}
