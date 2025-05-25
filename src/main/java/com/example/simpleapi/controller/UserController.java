package com.example.simpleapi.controller;

import com.example.simpleapi.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AtomicLong counter = new AtomicLong();
    private final Map<Long, User> repo = new ConcurrentHashMap<>();

    @GetMapping
    public Iterable<User> findAll() {
        return repo.values();
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        return repo.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@RequestBody User body) {
        Long id = counter.incrementAndGet();
        User created = new User(id, body.name(), body.email());
        repo.put(id, created);
        return created;
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User body) {
        User updated = new User(id, body.name(), body.email());
        repo.put(id, updated);
        return updated;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repo.remove(id);
    }
}
