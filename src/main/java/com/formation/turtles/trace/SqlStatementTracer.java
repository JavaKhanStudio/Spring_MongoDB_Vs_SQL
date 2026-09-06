package com.formation.turtles.trace;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Intercepteur Hibernate declare dans exemple-application.yml
 * (hibernate.session_factory.statement_inspector).
 *
 * Hibernate l'appelle avec le SQL final, juste avant de le confier au pilote
 * JDBC. C'est ainsi qu'on montre aux etudiants les 1 + N SELECT qu'une simple
 * lecture d'entite peut declencher, la ou MongoDB n'a fait qu'un seul aller-retour.
 */
public class SqlStatementTracer implements StatementInspector {
    @Override
    public String inspect(String sql) {
        QueryTraces.add(QueryTrace.sql(sql.replaceAll("\\s+", " ").trim()));
        return sql; // on n'altere jamais la requete, on ne fait que la regarder passer
    }
}
