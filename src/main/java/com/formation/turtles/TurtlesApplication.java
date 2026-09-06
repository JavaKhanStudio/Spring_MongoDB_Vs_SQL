package com.formation.turtles;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 🐢 Spring Data MongoDB vs JPA — le même domaine, implémenté deux fois.
 *
 * Les deux mondes cohabitent dans la même application : Spring Boot configure
 * les repositories JPA (package sql) et les repositories Mongo (package
 * mongo) sans qu'on ait rien à déclarer — chacun est reconnu à son type de
 * base.
 */
@SpringBootApplication
public class TurtlesApplication {
    public static void main(String[] args) {
        SpringApplication.run(TurtlesApplication.class, args);
    }

    /**
     * Spring Boot ne fournit pas ce bean tout fait : on le declare une fois ici pour
     * que la demo console puisse ouvrir une transaction sans construire elle-meme son
     * TransactionTemplate.
     */
    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
