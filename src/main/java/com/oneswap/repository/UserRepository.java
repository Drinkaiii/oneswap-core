package com.oneswap.repository;

import com.oneswap.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByAddress(String address);

}

