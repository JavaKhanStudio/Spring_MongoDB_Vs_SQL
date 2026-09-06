package com.formation.turtles.mongo.repo;

import java.util.Optional;

import com.formation.turtles.mongo.model.Program;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProgramMongoRepository extends MongoRepository<Program, String> {
    Optional<Program> findByName(String name);
}
