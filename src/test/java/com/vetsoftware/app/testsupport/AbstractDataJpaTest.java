package com.vetsoftware.app.testsupport;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base de los tests de adaptador de persistencia: Hibernate y Liquibase reales
 * contra un MySQL real.
 *
 * <p>
 * <b>Por que MySQL y no H2.</b> El schema de este proyecto depende de
 * comportamientos concretos del motor: {@code TINYINT} pelado (un
 * {@code TINYINT(1)} lo reporta el driver como {@code BIT} y rompe
 * {@code ddl-auto: validate}), {@code AUTO_INCREMENT} con {@code IDENTITY}, y
 * el {@code SELECT ... FOR UPDATE} del consecutivo fiscal. H2 daria verde sobre
 * un motor que no es el que se despliega, que es peor que no tener prueba.
 *
 * <p>
 * <b>El contenedor es uno solo para toda la suite.</b> Se arranca de forma
 * perezosa en el bloque estatico y NO se para: lo recoge el <i>ryuk</i> de
 * Testcontainers al terminar la JVM. Arrancarlo por clase costaria los 220
 * changesets de Liquibase en cada una.
 *
 * <p>
 * Que verifica de regalo, sin escribir un solo caso: que las migraciones
 * aplican en limpio sobre una base vacia y que las entidades JPA validan contra
 * el schema resultante. Hoy ese desajuste solo se descubre desplegando.
 *
 * <p>
 * <b>Por que se sube {@code max_connections}.</b> Ese contenedor unico atiende
 * a TODOS los contextos de Spring que la suite mantiene vivos a la vez: cada
 * clase {@code *IT} y cada {@code @Nested} produce un
 * {@code MergedContextConfiguration} distinto —el {@code @Import} del adaptador
 * entra en la clave del {@code ImportsContextCustomizer}— y el
 * {@code DefaultContextCache} conserva 32 en paralelo, cada uno con su propio
 * HikariCP. Con el {@code max_connections} de fabrica (151) la suite se quedaba
 * sin conexiones a mitad de camino: Liquibase no arrancaba, el
 * {@code entityManagerFactory} fallaba y caian en cascada cientos de
 * {@code Failed to load ApplicationContext}. El arreglo de fondo es el pool
 * acotado de {@code application-test.yml}; esto es el margen que evita volver a
 * rozar el techo cuando se sumen rodajas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public abstract class AbstractDataJpaTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--max-connections=500");

    static {
        MYSQL.start();
    }
}
