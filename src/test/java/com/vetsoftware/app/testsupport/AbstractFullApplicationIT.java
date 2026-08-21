package com.vetsoftware.app.testsupport;

import com.vetsoftware.app.VetSoftwareApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base de las pruebas de integracion que levantan la aplicacion ENTERA
 * ({@code VetSoftwareApplication}) sobre MySQL y Redis reales.
 *
 * <p>
 * <b>Por que existe: un solo contexto para todas.</b> Cada clase que repetia
 * esta configuracion a mano pagaba su propio arranque completo. La causa no era
 * el {@code @SpringBootTest} —esa parte si es identica entre ellas y Spring la
 * habria cacheado— sino el {@code @DynamicPropertySource}: el
 * {@code DynamicPropertiesContextCustomizer} entra en la clave del
 * {@code MergedContextConfiguration} y su {@code equals} compara el CONJUNTO DE
 * METODOS anotados. Dos metodos con el mismo cuerpo declarados en dos clases
 * distintas son dos claves distintas, asi que aquellos contextos no podian
 * compartirse <i>ni con cache infinita</i>. Declarando el metodo UNA vez aqui,
 * las subclases heredan el mismo {@code java.lang.reflect.Method} y la clave
 * coincide.
 *
 * <p>
 * Lo mismo vale para los contenedores: {@code ReflectionUtils.doWithFields} y
 * {@code MethodIntrospector.selectMethods} recorren la jerarquia, de modo que
 * el {@code @ServiceConnection} y el {@code @DynamicPropertySource} heredados
 * se descubren igual que si estuvieran en la subclase.
 *
 * <p>
 * El coste que se quita es real: un MySQL, un Redis y una pasada de los
 * changesets de Liquibase por cada clase que duplicaba esta configuracion.
 *
 * <p>
 * <b>El contenedor NO se para</b>, igual que en {@link AbstractDataJpaTest}: lo
 * recoge el <i>ryuk</i> de Testcontainers al terminar la JVM.
 *
 * <p>
 * <b>Por que MySQL propio y no el de {@link AbstractDataJpaTest}.</b> Aquel
 * sirve a rodajas {@code @DataJpaTest} que corren transaccionales y con
 * rollback; estas levantan la aplicacion entera. Se mantienen separados a
 * proposito para que un cambio en la configuracion de las rodajas no arrastre a
 * las pruebas de aplicacion completa, ni al reves.
 *
 * <p>
 * <b>Que pueden y que no pueden hacer las subclases.</b> Comparten base de
 * datos y Redis, asi que una subclase que ESCRIBA datos observables por otra
 * rompe el aislamiento y deja el resultado dependiente del orden. Las de hoy no
 * escriben: si una futura lo necesita, no se arregla con
 * {@code @DirtiesContext} —eso devuelve el arranque que este base viene a
 * quitar— sino con datos propios que nadie mas mire.
 */
@SpringBootTest(classes = VetSoftwareApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "openapi"})
public abstract class AbstractFullApplicationIT {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    /**
     * El limitador de las rutas publicas guarda sus cubos en Redis y se conecta al
     * crear el bean, asi que sin Redis no hay contexto que levantar. Es una imagen
     * de 15 MB: mas barato que fingir el bean y quedarse con un arranque que no es
     * el de produccion.
     */
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4.8-alpine")
            .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    /**
     * Unico {@code @DynamicPropertySource} de la jerarquia. Declararlo aqui y no en
     * cada subclase es lo que hace que el contexto se comparta — ver el javadoc de
     * la clase.
     */
    @DynamicPropertySource
    static void redisConnection(DynamicPropertyRegistry registry) {
        // RateLimitConfig lee la url completa, no host/puerto, asi que
        // @ServiceConnection no basta.
        registry.add("spring.data.redis.url",
                () -> "redis://" + REDIS.getHost() + ":" + REDIS.getFirstMappedPort());
    }
}
