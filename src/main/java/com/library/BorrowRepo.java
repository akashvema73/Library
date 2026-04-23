package com.library;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRepo extends JpaRepository<BorrowRecord, Integer> {

    List<BorrowRecord> findByUser(User user);
    List<BorrowRecord> findByStatus(String status);
    long countByStatus(String status);
}
