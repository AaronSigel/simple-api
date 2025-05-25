package com.example.simpleapi.controller;

import com.example.simpleapi.model.Project;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final AtomicLong counter = new AtomicLong();
    private final Map<Long, Project> repo = new ConcurrentHashMap<>();

    @GetMapping
    public Iterable<Project> findAll() {
        return repo.values();
    }

    @GetMapping("/{id}")
    public Project findById(@PathVariable Long id) {
        return repo.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(@RequestBody Project body) {
        Long id = counter.incrementAndGet();
        Project created = new Project(id, body.title(), body.description());
        repo.put(id, created);
        return created;
    }

    @PutMapping("/{id}")
    public Project update(@PathVariable Long id, @RequestBody Project body) {
        Project updated = new Project(id, body.title(), body.description());
        repo.put(id, updated);
        return updated;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repo.remove(id);
    }
}
