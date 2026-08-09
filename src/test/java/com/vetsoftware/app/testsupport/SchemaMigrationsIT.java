package com.vetsoftware.app.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El test mas barato de todo el proyecto y el que mas despliegues salva: si las
 * migraciones no aplican en limpio, o una entidad JPA no cuadra con el schema
 * que dejan, el contexto no arranca y esto se pone rojo. Hoy ese fallo solo
 * aparecia al desplegar.
 */
@DisplayName("Migraciones Liquibase + validacion de entidades")
class SchemaMigrationsIT extends AbstractDataJpaTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("los changesets aplican en limpio y Hibernate valida el schema")
    void los_changesets_aplican_y_hibernate_valida() {
        Number aplicados = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM DATABASECHANGELOG").getSingleResult();

        assertThat(aplicados.longValue()).as("changesets aplicados por Liquibase").isPositive();
    }
}
