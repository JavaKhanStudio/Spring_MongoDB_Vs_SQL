package com.formation.turtles.mongo.repo;

import java.util.Optional;

import com.formation.turtles.mongo.model.Habitat;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HabitatMongoRepository extends MongoRepository<Habitat, String> {
    Optional<Habitat> findByName(String name);
}
