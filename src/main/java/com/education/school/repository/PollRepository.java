package com.education.school.repository;

import com.education.school.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollRepository extends JpaRepository<Poll, Integer> {
}
