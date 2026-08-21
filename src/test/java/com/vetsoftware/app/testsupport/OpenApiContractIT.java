package com.vetsoftware.app.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

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
 * contexto web real no hay 564 endpoints que inspeccionar. El MySQL y el Redis
 * los pone {@link AbstractFullApplicationIT} —el schema real es parte de lo que
 * se prueba, y el limitador de rutas publicas se conecta a Redis al crear su
 * bean—, compartidos con las demas pruebas de aplicacion completa.
 *
 * <p>
 * <b>Para regenerar</b> tras un cambio de API deliberado:
 * {@code ./mvnw verify -Dit.test=OpenApiContractIT -Dopenapi.write=true}.
 */
class OpenApiContractIT extends AbstractFullApplicationIT {

    /** Ruta versionada del contrato. La consumen los dos frontends. */
    private static final Path SPEC = Path.of("api", "openapi.json");

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
        // Se normalizan los finales de linea. Git reescribe el fichero a CRLF en una
        // copia
        // de trabajo de Windows, asi que comparar el texto crudo rompia el build en
        // cualquier
        // clon nuevo aunque el contrato fuese identico, y el mensaje invitaba a
        // regenerar,
        // que no arreglaba nada porque el diff seguia vacio.
        assertThat(normalizar(Files.readString(SPEC, StandardCharsets.UTF_8))).as("""
                El contrato de la API cambio y %s no se actualizo.

                Si el cambio es deliberado, regenera y commitea:
                  ./mvnw verify -Dit.test=OpenApiContractIT -Dopenapi.write=true
                y regenera despues los tipos de los dos frontends:
                  npm run api:types
                """, SPEC).isEqualTo(normalizar(actual));
    }

    /**
     * Finales de linea a LF: lo que se compara es el contrato, no como lo guardo
     * git.
     */
    private static String normalizar(String texto) {
        return texto.replace("\r\n", "\n");
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
