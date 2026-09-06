package com.formation.turtles.mongo.repo;

import java.util.List;

import com.formation.turtles.mongo.model.TurtleWithDbRef;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Sert uniquement a la demonstration @DBRef du chapitre 2. */
public interface TurtleDbRefRepository extends MongoRepository<TurtleWithDbRef, String> {

    List<TurtleWithDbRef> findAllBy(Pageable pageable);
}
