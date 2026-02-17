package com.education.school.service;

import com.education.school.dto.DisciplineDto;
import com.education.school.entity.Discipline;
import com.education.school.repository.DisciplineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisciplineService {

    private final DisciplineRepository repository;

    @Transactional(readOnly = true)
    public List<DisciplineDto> findAll() {
        return repository.findAll().stream()
                .map(DisciplineDto::from)
                .collect(Collectors.toList());
    }

    public Discipline findById(Integer id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional
    public Discipline create(Discipline discipline) {
        return repository.save(discipline);
    }

    @Transactional
    public Discipline update(Integer id, Discipline disciplineDetails) {
        Discipline discipline = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found"));
        
        discipline.setName(disciplineDetails.getName());
        
        return repository.save(discipline);
    }

    @Transactional
    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
