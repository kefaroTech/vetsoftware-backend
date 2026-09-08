package com.vetsoftware.app.registration.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.registration.application.port.out.EligibleTrialCatalogItemsPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.testsupport.AbstractFullApplicationIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * El alta pública de punta a punta contra MySQL real, del lado de
 * {@code FreeTrialContractProvisioner}: que las cuatro escrituras —ventana,
 * concesiones, líneas {@code TRIAL} y entitlements— lleguen a la base sin que
 * ninguna FK ni ningún {@code CHECK} las rechace.
 *
 * <p>
 * <b>Por qué hace falta además de {@code FreeTrialContractProvisionerTest}.</b>
 * Ese test mockea los cuatro puertos de salida, así que nunca ejercita
 * {@code fk_subscription_items_trial_grant} ni
 * {@code chk_company_trial_grants_paper}: un doble no rechaza nada que el motor
 * rechazaría. Aquí la única aserción que importa la hace la base al aceptar el
 * 201; el resto son las filas que quedaron.
 *
 * <p>
 * Comparte {@code @TestPropertySource} con {@link OwnerBranchAssignmentIT}
 * —misma clave de contexto, mismo catálogo local/e2e sembrado, sin arranque
 * adicional— y, como ella, hace una sola alta: {@code /register} limita a 3 por
 * hora y por IP compartida entre toda la jerarquía de
 * {@link AbstractFullApplicationIT}.
 */
@TestPropertySource(properties = "spring.liquibase.contexts=local,e2e")
@DisplayName("Alta publica — la prueba gratuita se escribe entera y sin violar el esquema")
class FreeTrialGrantProvisioningIT extends AbstractFullApplicationIT {

    private static final String COMPANY_IDENTIFIER = "9005100520";
    private static final String OWNER_EMAIL = "dueno.trial520@vetsoftware.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EligibleTrialCatalogItemsPort eligibleTrialCatalogItemsPort;

    private static final ObjectMapper JSON = new ObjectMapper();

    private String altaJson(Long cityId) {
        return """
                {
                  "companyName": "Veterinaria IT 520",
                  "documentType": "NIT",
                  "companyIdentifier": "%s",
                  "companyAddress": "Calle 5 # 2-3",
                  "companyContactNumber": "3009876543",
                  "cityId": %d,
                  "employeeName": "Dueno IT 520",
                  "employeeEmail": "%s",
                  "password": "Orlando1997*",
                  "taxRegime": "RESPONSABLE_IVA",
                  "fiscalEmail": "fiscal.it520@vetsoftware.test"
                }
                """.formatted(COMPANY_IDENTIFIER, cityId, OWNER_EMAIL);
    }

    @Test
    @DisplayName("una concesión y una línea TRIAL por cada artículo elegible, más los entitlements")
    void una_concesion_y_una_linea_trial_por_cada_articulo_elegible() throws Exception {
        Long cityId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM cities", Long.class);
        int elegibles = eligibleTrialCatalogItemsPort.findAll(BillingCycle.MONTHLY).size();

        MvcResult result = mockMvc
                .perform(post("/api/v1/register").contextPath("/api/v1").servletPath("/register")
                        .contentType(MediaType.APPLICATION_JSON).content(altaJson(cityId)))
                .andExpect(status().isCreated()).andReturn();

        JsonNode body = JSON.readTree(result.getResponse().getContentAsString());
        Long companyId = body.get("companyId").asLong();

        assertThat(elegibles).as("el catálogo local/e2e trae al menos un artículo ELIGIBLE")
                .isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM company_trial_windows WHERE company_id = ? AND origin = 'SIGNUP'",
                Integer.class, companyId)).as("una sola ventana, sin cotización").isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM company_trial_grants WHERE company_id = ? AND origin = 'SIGNUP'",
                Integer.class, companyId)).as("una concesión por artículo elegible")
                .isEqualTo(elegibles);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subscription_items"
                        + " WHERE company_id = ? AND charge_mode = 'TRIAL'",
                Integer.class, companyId))
                .as("una línea TRIAL por artículo elegible, ligada a su concesión sin violar"
                        + " fk_subscription_items_trial_grant")
                .isEqualTo(elegibles);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM company_entitlements WHERE company_id = ?", Integer.class,
                companyId)).as("los entitlements se derivan del contrato recién firmado")
                .isGreaterThan(0);
    }
}
