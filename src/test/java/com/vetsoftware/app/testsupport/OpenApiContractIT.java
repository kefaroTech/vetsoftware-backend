package com.vetsoftware.app.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vetsoftware.app.VetSoftwareApplication;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

/**
 * Exporta el contrato OpenAPI del backend y falla si el fichero versionado se
 * quedo atras.
 *
 * <p>
 * TR-01: los dos frontends declaran a mano ~565 tipos que espejan los DTOs de
 * este backend. Cambiar un campo en un record compila, despliega y falla en el
 * navegador del veterinario como {@code undefined}. Este test es el eslabon que
 * faltaba: convierte un cambio de contrato en un fallo del build, aqui, antes
 * de que salga.
 *
 * <p>
 * <b>Por que arranca la aplicacion entera.</b> springdoc construye el esquema
 * inspeccionando los handlers registrados y los tipos que devuelven; sin el
 * contexto web real no hay 564 endpoints que inspeccionar. Se reutiliza el
 * contenedor MySQL de {@link AbstractDataJpaTest} por la misma razon que aquel:
 * el schema real es parte de lo que se prueba.
 *
 * <p>
 * <b>Para regenerar</b> tras un cambio de API deliberado:
 * {@code ./mvnw verify -Dit.test=OpenApiContractIT -Dopenapi.write=true}.
 */
@SpringBootTest(classes = VetSoftwareApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "openapi"})
class OpenApiContractIT {

    /** Ruta versionada del contrato. La consumen los dos frontends. */
    private static final Path SPEC = Path.of("api", "openapi.json");

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

    @DynamicPropertySource
    static void redisConnection(DynamicPropertyRegistry registry) {
        // RateLimitConfig lee la url completa, no host/puerto, asi que
        // @ServiceConnection no basta.
        registry.add("spring.data.redis.url",
                () -> "redis://" + REDIS.getHost() + ":" + REDIS.getFirstMappedPort());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("el contrato versionado coincide con el que expone la aplicacion")
    void el_contrato_versionado_coincide() throws Exception {
        String actual = exportSpec();

        if (Boolean.getBoolean("openapi.write")) {
            Files.createDirectories(SPEC.getParent());
            Files.writeString(SPEC, actual, StandardCharsets.UTF_8);
            return;
        }

        assertThat(Files.exists(SPEC)).as("falta %s; generalo con -Dopenapi.write=true", SPEC)
                .isTrue();
        assertThat(Files.readString(SPEC, StandardCharsets.UTF_8)).as("""
                El contrato de la API cambio y %s no se actualizo.

                Si el cambio es deliberado, regenera y commitea:
                  ./mvnw verify -Dit.test=OpenApiContractIT -Dopenapi.write=true
                y regenera despues los tipos de los dos frontends:
                  npm run api:types
                """, SPEC).isEqualTo(actual);
    }

    /**
     * Vuelca {@code /v3/api-docs} con las claves ordenadas. Sin ese orden, dos
     * ejecuciones del mismo codigo producen ficheros distintos y el diff deja de
     * significar «cambio de contrato».
     */
    private String exportSpec() throws Exception {
        // servletPath explicito: AuthFilter decide si la ruta es publica con
        // getServletPath(), y en MockMvc no se deriva solo del contextPath.
        String raw = mockMvc
                .perform(get("/api/v1/v3/api-docs").contextPath("/api/v1")
                        .servletPath("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(mapper.readTree(raw)) + "\n";
    }
}
