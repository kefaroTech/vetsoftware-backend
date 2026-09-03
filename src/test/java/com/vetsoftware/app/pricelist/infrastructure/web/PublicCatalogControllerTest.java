package com.vetsoftware.app.pricelist.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.pricelist.application.dto.PublicCatalogAreaDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogCapacityDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogRequirementDto;
import com.vetsoftware.app.pricelist.application.port.in.GetPublicCatalogUseCase;
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
 * Los casos —tarifas solapadas, articulo sin precio en un ciclo, reparto por
 * tipo— son del caso de uso y viven en {@code GetPublicCatalogServiceTest}.
 * Aqui se fija la forma que sale por el cable, incluido que un importe ausente
 * viaje como {@code null} y no como cero.
 */
@WebMvcTest(PublicCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PublicCatalogController — contrato HTTP")
class PublicCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPublicCatalogUseCase useCase;

    @Test
    @DisplayName("sin tarifa vigente devuelve 200 con todas las listas vacias, no un 404")
    void sin_tarifa_vigente_devuelve_doscientos_con_las_listas_vacias() throws Exception {
        when(useCase.get()).thenReturn(new PublicCatalogDto(null, null, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()));

        mockMvc.perform(get("/catalog")).andExpect(status().isOk())
                .andExpect(content().json("{\"currency\":null,\"priceValidFrom\":null,"
                        + "\"modules\":[],\"capacities\":[],\"oneTimeItems\":[],"
                        + "\"packs\":[],\"areas\":[]}"));
    }

    @Test
    @DisplayName("publica el precio suelto de cada pieza y la composicion del paquete")
    void publica_el_precio_suelto_y_la_composicion_del_paquete() throws Exception {
        when(useCase.get()).thenReturn(new PublicCatalogDto("COP", LocalDate.of(2026, 8, 1),
                List.of(new PublicCatalogItemDto("CORE", "Nucleo", "Clientes y mascotas", true,
                        null, new BigDecimal("49000.00"), new BigDecimal("490000.00"),
                        new BigDecimal("0.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, true,
                        null, "Nucleo"),
                        new PublicCatalogItemDto("SURGERY", "Cirugia", null, false, 30,
                                new BigDecimal("38000.00"), null, new BigDecimal("0.00"),
                                new BigDecimal("19.00"), TaxTreatment.TAXED, true, "HOSPITAL",
                                "Cirugia")),
                List.of(new PublicCatalogCapacityDto("CAPACITY_USER", "Usuario adicional", null,
                        true, "USER", 3, 5, new BigDecimal("15000.00"), new BigDecimal("145000.00"),
                        new BigDecimal("19.00"), TaxTreatment.TAXED, true)),
                List.of(new PublicCatalogItemDto("DATA_MIGRATION", "Migracion de datos", null,
                        false, null, new BigDecimal("0.00"), new BigDecimal("0.00"),
                        new BigDecimal("450000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED,
                        false, null, null)),
                List.of(new PublicCatalogPackDto("PACK_CLINIC", "Clinica", "Para una clinica",
                        new BigDecimal("89000.00"), new BigDecimal("890000.00"),
                        new BigDecimal("150000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED,
                        List.of("CORE", "SURGERY"), true)),
                List.of(new PublicCatalogRequirementDto("ELECTRONIC_INVOICING", "CASH_REGISTER"),
                        new PublicCatalogRequirementDto("EXTRA_STORAGE", "LAB_IMAGING")),
                List.of(new PublicCatalogAreaDto("PATIENT_CARE", "Atencion a pacientes"),
                        new PublicCatalogAreaDto("HOSPITAL", "Hospital y quirofano"))));

        mockMvc.perform(get("/catalog")).andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.priceValidFrom").value("2026-08-01"))
                .andExpect(jsonPath("$.modules[0].code").value("CORE"))
                .andExpect(jsonPath("$.modules[0].mandatory").value(true))
                .andExpect(jsonPath("$.modules[1].monthlyAmount").value(38000.00))
                .andExpect(jsonPath("$.modules[1].trialDays").value(30))
                .andExpect(jsonPath("$.capacities[0].unit").value("USER"))
                .andExpect(jsonPath("$.capacities[0].monthlyIncludedQuantity").value(3))
                .andExpect(jsonPath("$.capacities[0].annualIncludedQuantity").value(5))
                .andExpect(jsonPath("$.oneTimeItems[0].selfServiceEligible").value(false))
                .andExpect(jsonPath("$.packs[0].componentCodes")
                        .value(org.hamcrest.Matchers.contains("CORE", "SURGERY")))
                .andExpect(jsonPath("$.requirements[0].itemCode").value("ELECTRONIC_INVOICING"))
                .andExpect(jsonPath("$.requirements[0].requiredItemCode").value("CASH_REGISTER"))
                .andExpect(jsonPath("$.requirements[1].requiredItemCode").value("LAB_IMAGING"))
                .andExpect(jsonPath("$.modules[0].areaCode").doesNotExist())
                .andExpect(jsonPath("$.modules[1].areaCode").value("HOSPITAL"))
                .andExpect(jsonPath("$.modules[1].shortLabel").value("Cirugia"))
                .andExpect(jsonPath("$.packs[0].recommended").value(true))
                .andExpect(jsonPath("$.areas[0].code").value("PATIENT_CARE"))
                .andExpect(jsonPath("$.areas[0].name").value("Atencion a pacientes"))
                .andExpect(jsonPath("$.areas[1].code").value("HOSPITAL"))
                .andExpect(jsonPath("$.areas[0].sortOrder").doesNotExist());
    }

    /**
     * {@code null} y no {@code 0}: el cero se leeria como «gratis» y el gate de la
     * contratacion rechaza ese ciclo. Se comprueba con {@code doesNotExist} porque
     * la serializacion omite el nulo, que es exactamente lo que el front distingue.
     */
    @Test
    @DisplayName("un importe ausente viaja como null, jamas como cero")
    void un_importe_ausente_viaja_como_null() throws Exception {
        when(useCase.get()).thenReturn(new PublicCatalogDto("COP", LocalDate.of(2026, 8, 1),
                List.of(new PublicCatalogItemDto("GROOMING", "Peluqueria", null, false, null,
                        new BigDecimal("29000.00"), null, new BigDecimal("0.00"),
                        new BigDecimal("19.00"), TaxTreatment.TAXED, true, "PATIENT_CARE", null)),
                List.of(), List.of(), List.of(), List.of(), List.of()));

        mockMvc.perform(get("/catalog")).andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].monthlyAmount").value(29000.00))
                .andExpect(jsonPath("$.modules[0].annualAmount").doesNotExist())
                .andExpect(jsonPath("$.modules[0].trialDays").doesNotExist())
                .andExpect(jsonPath("$.modules[0].shortLabel").doesNotExist());
    }

    /**
     * Ni el id del articulo, ni el de la tarifa, ni su codigo, ni su estado, ni la
     * escalera de tramos. Un id es una llave de escritura y esto lo lee un anonimo.
     */
    @Test
    @DisplayName("no publica ningun id ni la fecha de caducidad de la tarifa")
    void no_publica_ningun_id_ni_la_fecha_de_caducidad() throws Exception {
        when(useCase.get()).thenReturn(new PublicCatalogDto("COP", LocalDate.of(2026, 8, 1),
                List.of(new PublicCatalogItemDto("SURGERY", "Cirugia", null, false, null,
                        new BigDecimal("38000.00"), null, new BigDecimal("0.00"),
                        new BigDecimal("19.00"), TaxTreatment.TAXED, true, "HOSPITAL", "Cirugia")),
                List.of(), List.of(), List.of(), List.of(), List.of()));

        mockMvc.perform(get("/catalog")).andExpect(status().isOk())
                .andExpect(jsonPath("$.priceListId").doesNotExist())
                .andExpect(jsonPath("$.priceValidTo").doesNotExist())
                .andExpect(jsonPath("$.modules[0].id").doesNotExist())
                .andExpect(jsonPath("$.modules[0].tierMax").doesNotExist())
                .andExpect(jsonPath("$.modules[0].areaId").doesNotExist());
    }
}
