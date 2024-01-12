package com.cos.security2.repository;

import com.cos.security2.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

// CRUD 함수를 JpaRepository가 들고 있음
// @Repository 어노테이션이 없어도 IoC됨. JpaRepository를 상속 했기 때문
public interface UserRepository extends JpaRepository<User, Long> {
}
