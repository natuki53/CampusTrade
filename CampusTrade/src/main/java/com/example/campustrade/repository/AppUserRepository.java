package com.example.campustrade.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campustrade.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByStudentNumber(String studentNumber);

	boolean existsByStudentNumber(String studentNumber);
}
