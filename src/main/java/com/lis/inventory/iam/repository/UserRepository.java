package com.lis.inventory.iam.repository;

import com.lis.inventory.iam.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByAuth0Sub(String auth0Sub);

    boolean existsByEmail(String email);
}
