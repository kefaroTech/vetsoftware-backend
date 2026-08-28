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
 *
 * <p>
 * <b>Por que se confia en el creador de rutinas.</b> El changeset 346 crea
 * siete {@code TRIGGER} —los que impiden escribir en un periodo contable
 * cerrado—, y MySQL rechaza un {@code CREATE TRIGGER} con el error 1419 cuando
 * el binlog esta activo y el usuario no es {@code SUPER}: <i>You do not have
 * the SUPER privilege and binary logging is enabled</i>. El usuario del
 * contenedor de Testcontainers no lo es, asi que sin este flag Liquibase muere
 * en el changeset 346, con el se lleva por delante al
 * {@code entityManagerFactory} y <b>TODAS</b> las rodajas de persistencia del
 * repositorio caen en cascada con el mismo
 * {@code ApplicationContext failure threshold (1) exceeded} — noventa y cuatro
 * errores con una sola causa, que es exactamente el patron que advierte el
 * CLAUDE.md.
 *
 * <p>
 * <b>Y no es solo un arreglo de test.</b> Un despliegue real contra RDS tiene
 * el binlog activo y su usuario maestro tampoco es {@code SUPER}: la misma
 * migracion fallara alli salvo que el grupo de parametros de la instancia fije
 * {@code log_bin_trust_function_creators = 1}. Este flag reproduce esa
 * configuracion en el contenedor; comprobar que existe en RDS es trabajo de
 * infraestructura, no de este fichero.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public abstract class AbstractDataJpaTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--max-connections=500", "--log-bin-trust-function-creators=1");

    static {
        MYSQL.start();
    }
}
