package com.vetsoftware.app.registration.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.auth.application.port.out.BranchAccessResolver;
import com.vetsoftware.app.testsupport.AbstractFullApplicationIT;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * <b>La prueba que habria cazado el issue #510.</b>
 *
 * <p>
 * El alta creaba la sede "Principal" y creaba al dueño, y nadie escribia la
 * fila de {@code employee_branches} que las une. Ninguna de las pruebas que
 * existian podia verlo: {@code RegisterUserServiceTest} mockea los puertos —y
 * un puerto que nadie llama no falla, simplemente no aparece—, y
 * {@code RegistrationControllerTest} es una rodaja web con el caso de uso
 * mockeado. Es el «verde que miente» exacto: el defecto vivia en la
 * <em>distancia</em> entre dos piezas que por separado estaban bien.
 *
 * <p>
 * <b>Por eso esto es un {@code *IT} sobre MySQL 8.4 real y la aplicacion
 * entera</b>, no un test con dobles: la unica forma de comprobar que la fila
 * existe es mirar la fila. Se ejercita el endpoint publico
 * {@code POST /register} de punta a punta —controller, transaccion, Liquibase,
 * catalogo sembrado— y despues se pregunta a la base y al resolver.
 *
 * <p>
 * <b>Y no comprueba solo que "hay filas": comprueba la condicion exacta del
 * 403.</b> {@code Authz.requireAssignableBranches} —la que rechazaba invitar
 * personal— exige que la sede este en {@code currentBranchIds()}, que sale de
 * {@link BranchAccessResolver#resolveFor(Long)}. Afirmar sobre ese mismo
 * conjunto, y no sobre un {@code SELECT} parecido, es lo que hace que este test
 * hable del sintoma que se reporto y no de una aproximacion suya.
 *
 * <p>
 * <b>Escribe, al contrario que sus hermanas.</b> El javadoc de
 * {@link AbstractFullApplicationIT} lo permite explicitamente con la condicion
 * que aqui se cumple: datos propios que nadie mas mira. La empresa de este test
 * nace con un identificador fiscal y un correo suyos, fijos (no aleatorios: el
 * contenedor es nuevo en cada JVM y ninguna otra {@code *IT} registra
 * empresas), y ninguna asercion de otra clase los cuenta.
 *
 * <p>
 * <b>Lleva su propio {@code spring.liquibase.contexts}, y eso le cuesta un
 * contexto propio.</b> {@code application.yml} fija
 * {@code contexts: production}, asi que las semillas de laboratorio —el
 * catalogo comercial minimo de los changesets 262-265/269/270, marcadas
 * {@code local,e2e}— NO se aplican en el perfil de test. Sin ellas el alta no
 * llega a crear nada: muere en su primera guarda con <b>503
 * PLATFORM_CATALOG_NOT_CONFIGURED</b>, que es justamente la hermana de la
 * guarda que este test viene a cubrir. El {@code @TestPropertySource} fragmenta
 * la clave del contexto —cuesta un arranque mas, con el aviso que da
 * {@link AbstractFullApplicationIT} en su javadoc— y es el precio de ejercitar
 * el alta de verdad en vez de fingir el catalogo con INSERTs propios, que es lo
 * que esta incidencia lleva persiguiendo.
 *
 * <p>
 * <b>Una sola alta a proposito.</b> {@code /register} esta limitado a 3 por
 * hora y por IP, y todas las peticiones de MockMvc llegan de la misma. Repartir
 * el escenario en varios {@code @Test} gastaria el cupo y haria el resultado
 * dependiente del orden.
 */
@TestPropertySource(properties = "spring.liquibase.contexts=local,e2e")
@DisplayName("Alta publica — el dueño queda atado a su sede Principal (#510)")
class OwnerBranchAssignmentIT extends AbstractFullApplicationIT {

    private static final String COMPANY_IDENTIFIER = "9005100510";
    private static final String OWNER_EMAIL = "dueno.it510@vetsoftware.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BranchAccessResolver branchAccessResolver;

    /**
     * Propio, no inyectado: este contexto no expone ningun bean
     * {@link ObjectMapper}, y de todas formas aqui solo se leen dos numeros del
     * cuerpo de la respuesta — no se esta comprobando la serializacion de la
     * aplicacion, que es lo unico que justificaria usar el suyo.
     */
    private static final ObjectMapper JSON = new ObjectMapper();

    private String altaJson(Long cityId) {
        return """
                {
                  "companyName": "Veterinaria IT 510",
                  "documentType": "NIT",
                  "companyIdentifier": "%s",
                  "companyAddress": "Calle 1 # 2-3",
                  "companyContactNumber": "3001234567",
                  "cityId": %d,
                  "employeeName": "Dueno IT 510",
                  "employeeEmail": "%s",
                  "password": "Orlando1997*",
                  "taxRegime": "RESPONSABLE_IVA",
                  "fiscalEmail": "fiscal.it510@vetsoftware.test"
                }
                """.formatted(COMPANY_IDENTIFIER, cityId, OWNER_EMAIL);
    }

    @Test
    @DisplayName("tras el 201, la sede Principal esta en el conjunto de sedes del dueño"
            + " — sin ella, invitar personal devolvia 403 BRANCH_NOT_ALLOWED")
    void tras_el_alta_el_dueno_tiene_su_sede_principal_asignada() throws Exception {
        Long cityId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM cities", Long.class);

        // El contexto de la aplicacion es /api/v1 y MockMvc NO lo aplica solo: sin
        // contextPath/servletPath la peticion llega como "/register", AuthFilter no la
        // reconoce en PublicRoutes y responde 401 TOKEN_MISSING en vez de 201. Mismo
        // trio de llamadas que usa TraceparentPropagationIT.
        MvcResult result = mockMvc
                .perform(post("/api/v1/register").contextPath("/api/v1").servletPath("/register")
                        .contentType(MediaType.APPLICATION_JSON).content(altaJson(cityId)))
                .andExpect(status().isCreated()).andReturn();

        JsonNode body = JSON.readTree(result.getResponse().getContentAsString());
        Long companyId = body.get("companyId").asLong();
        Long employeeId = body.get("employeeId").asLong();

        Long principalBranchId = jdbcTemplate.queryForObject(
                "SELECT id FROM branches WHERE company_id = ? AND code = 'PRINCIPAL'", Long.class,
                companyId);

        // 1) La fila que nadie insertaba. Antes del arreglo esto era una lista vacia.
        List<Long> filas = jdbcTemplate.queryForList(
                "SELECT branch_id FROM employee_branches WHERE employee_id = ? AND enabled = true",
                Long.class, employeeId);
        assertThat(filas).as("employee_branches del dueño recien registrado")
                .containsExactly(principalBranchId);

        // 2) La condicion exacta que evaluaba requireAssignableBranches al devolver el
        // 403: la sede que se quiere asignar tiene que estar en el conjunto del caller.
        assertThat(branchAccessResolver.resolveFor(employeeId))
                .as("currentBranchIds() del dueño — la fuente del 403 BRANCH_NOT_ALLOWED")
                .contains(principalBranchId);
    }
}
