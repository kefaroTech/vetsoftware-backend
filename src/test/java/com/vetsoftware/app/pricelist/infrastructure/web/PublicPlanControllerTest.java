package com.vetsoftware.app.pricelist.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.pricelist.application.dto.PublicPlanCapacityDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanCatalogDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanIncludedDto;
import com.vetsoftware.app.pricelist.application.port.in.GetPublicPlansUseCase;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La rodaja que {@code CONTROLLER_CON_RODAJA} exige, y que aqui vale por algo
 * mas que por la regla: lo que este endpoint publica lo ve cualquiera sin
 * autenticarse, asi que <b>el JSON es la superficie de exposicion</b>. Un campo
 * de mas aqui no es un fallo de contrato, es una fuga.
 *
 * <p>
 * Cubre la forma y la ausencia. La cobertura por casos —tarifas solapadas,
 * paquete sin ciclo mensual, componente sin precio suelto— es del caso de uso y
 * la escribe quien haga la suite; el SQL nativo del adaptador necesita ademas
 * su propia rodaja contra MySQL real, que hoy no existe.
 */
@WebMvcTest(PublicPlanController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PublicPlanController — contrato HTTP")
class PublicPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPublicPlansUseCase useCase;

    @Test
    @DisplayName("sin tarifa vigente devuelve 200 con la lista vacia, no un 404")
    void sin_tarifa_vigente_devuelve_doscientos_con_la_lista_vacia() throws Exception {
        when(useCase.get()).thenReturn(new PublicPlanCatalogDto(null, null, List.of()));

        mockMvc.perform(get("/plans")).andExpect(status().isOk()).andExpect(
                content().json("{\"currency\":null,\"priceValidFrom\":null,\"plans\":[]}"));
    }

    @Test
    @DisplayName("publica el precio de entrada y los dias de prueba por linea")
    void publica_el_precio_de_entrada_y_los_dias_de_prueba_por_linea() throws Exception {
        when(useCase.get()).thenReturn(new PublicPlanCatalogDto("COP", LocalDate.of(2026, 8, 1),
                List.of(new PublicPlanDto("ESENCIAL", "Esencial", "Para una clinica que empieza",
                        new BigDecimal("89000.00"), new BigDecimal("890000.00"),
                        new BigDecimal("0.00"), new BigDecimal("19.00"), TaxTreatment.TAXED,
                        List.of(new PublicPlanIncludedDto("AGENDA", "Agenda", 30),
                                new PublicPlanIncludedDto("CAJA", "Caja", null)),
                        List.of(new PublicPlanCapacityDto("EXTRA_USER", "Usuario adicional", "USER",
                                3, new BigDecimal("15000.00"), new BigDecimal("145000.00")))))));

        mockMvc.perform(get("/plans")).andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.priceValidFrom").value("2026-08-01"))
                .andExpect(jsonPath("$.plans[0].code").value("ESENCIAL"))
                .andExpect(jsonPath("$.plans[0].monthlyFromAmount").value(89000.00))
                .andExpect(jsonPath("$.plans[0].includes[0].trialDays").value(30))
                .andExpect(jsonPath("$.plans[0].includes[1].trialDays").doesNotExist())
                .andExpect(jsonPath("$.plans[0].capacities[0].unit").value("USER"))
                .andExpect(jsonPath("$.plans[0].capacities[0].included").value(3))
                // El JSON lleva el ciclo en el nombre de cada importe: sin eso, quien
                // pinta un plan anual solo puede extrapolar el mensual, y el servidor
                // cotiza contra la fila ANNUAL del articulo.
                .andExpect(
                        jsonPath("$.plans[0].capacities[0].monthlyExtraUnitAmount").value(15000.00))
                .andExpect(
                        jsonPath("$.plans[0].capacities[0].annualExtraUnitAmount").value(145000.00))
                .andExpect(jsonPath("$.plans[0].capacities[0].extraUnitAmount").doesNotExist());
    }

    /**
     * El nulo viaja como nulo y no se colapsa a cero: cero seria «la unidad
     * adicional es gratis en ese ciclo» y lo cierto es «no se vende en ese ciclo».
     */
    @Test
    @DisplayName("un contador sin precio anual publica el importe anual ausente, no un cero")
    void un_contador_sin_precio_anual_no_publica_un_cero() throws Exception {
        when(useCase.get()).thenReturn(new PublicPlanCatalogDto("COP", LocalDate.of(2026, 8, 1),
                List.of(new PublicPlanDto("ESENCIAL", "Esencial", null, new BigDecimal("89000.00"),
                        new BigDecimal("890000.00"), null, new BigDecimal("19.00"),
                        TaxTreatment.TAXED, List.of(),
                        List.of(new PublicPlanCapacityDto("EXTRA_BRANCH", "Sede adicional",
                                "BRANCH", 1, new BigDecimal("45000.00"), null))))));

        mockMvc.perform(get("/plans")).andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.plans[0].capacities[0].monthlyExtraUnitAmount").value(45000.00))
                .andExpect(
                        jsonPath("$.plans[0].capacities[0].annualExtraUnitAmount").doesNotExist())
                .andExpect(jsonPath("$.plans[0].capacities[0].included").value(1));
    }

    @Test
    @DisplayName("no publica ningun id ni la fecha de caducidad de la tarifa")
    void no_publica_ningun_id_ni_la_fecha_de_caducidad() throws Exception {
        when(useCase.get()).thenReturn(new PublicPlanCatalogDto("COP", LocalDate.of(2026, 8, 1),
                List.of(new PublicPlanDto("ESENCIAL", "Esencial", null, new BigDecimal("89000.00"),
                        null, null, new BigDecimal("19.00"), TaxTreatment.TAXED,
                        List.of(new PublicPlanIncludedDto("AGENDA", "Agenda", 30)), List.of()))));

        mockMvc.perform(get("/plans")).andExpect(status().isOk())
                // Un id es una llave de escritura; un code es un rotulo.
                .andExpect(jsonPath("$.plans[0].id").doesNotExist())
                .andExpect(jsonPath("$.priceListId").doesNotExist())
                .andExpect(jsonPath("$.plans[0].includes[0].id").doesNotExist())
                // Con la caducidad publicada, quien compara espera al ultimo dia.
                .andExpect(jsonPath("$.priceValidTo").doesNotExist())
                .andExpect(jsonPath("$.plans[0].validTo").doesNotExist())
                // Nada del modelo interno de administracion.
                .andExpect(jsonPath("$.plans[0].status").doesNotExist())
                .andExpect(jsonPath("$.plans[0].core").doesNotExist())
                .andExpect(jsonPath("$.plans[0].sortOrder").doesNotExist())
                .andExpect(jsonPath("$.plans[0].tierMax").doesNotExist())
                .andExpect(jsonPath("$.plans[0].discountPercent").doesNotExist())
                .andExpect(jsonPath("$.plans[0].companyId").doesNotExist());
    }
}
