package com.education.school.service;

import com.education.school.entity.Discipline;
import com.education.school.repository.DisciplineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisciplineService {

    private final DisciplineRepository repository;

    public List<Discipline> findAll() {
        return repository.findAll();
    }

    public Discipline create(Discipline discipline) {
        return repository.save(discipline);
    }
}
