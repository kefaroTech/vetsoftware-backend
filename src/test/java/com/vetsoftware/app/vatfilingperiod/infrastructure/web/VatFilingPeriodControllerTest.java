package com.vetsoftware.app.vatfilingperiod.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import com.vetsoftware.app.vatfilingperiod.application.dto.VatFilingPeriodDto;
import com.vetsoftware.app.vatfilingperiod.application.port.in.CreateVatFilingPeriodUseCase;
import com.vetsoftware.app.vatfilingperiod.application.port.in.FindVatFilingPeriodForYearUseCase;
import com.vetsoftware.app.vatfilingperiod.application.port.in.ListVatFilingPeriodsUseCase;
import com.vetsoftware.app.vatfilingperiod.domain.VatFilingFrequency;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VatFilingPeriodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("VatFilingPeriodController — contrato HTTP de la periodicidad de IVA")
class VatFilingPeriodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateVatFilingPeriodUseCase createUseCase;

    @MockitoBean
    private FindVatFilingPeriodForYearUseCase findUseCase;

    @MockitoBean
    private ListVatFilingPeriodsUseCase listUseCase;

    private static VatFilingPeriodDto bimestral2026() {
        return new VatFilingPeriodDto(7300L, 2026, VatFilingFrequency.BIMONTHLY,
                "Art. 600 num. 1 ET - responsable nuevo: el primer ano es bimestral",
                LocalDateTime.of(2026, 1, 2, 8, 0, 0), true);
    }

    @Nested
    @DisplayName("Publicacion")
    class Publicacion {

        @Test
        @DisplayName("responde 201 con la periodicidad y la norma que la fija")
        void responde_201_con_la_periodicidad() throws Exception {
            when(createUseCase.execute(any())).thenReturn(bimestral2026());

            mockMvc.perform(
                    post("/vat-filing-periods").contentType(MediaType.APPLICATION_JSON).content("""
                            {"fiscalYear":2026,"frequency":"BIMONTHLY",
                             "legalReference":"Art. 600 num. 1 ET"}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fiscalYear").value(2026))
                    .andExpect(jsonPath("$.frequency").value("BIMONTHLY"));
        }

        @Test
        @DisplayName("sin periodicidad sale 400 y NO llega al caso de uso")
        void sin_periodicidad_sale_400() throws Exception {
            mockMvc.perform(
                    post("/vat-filing-periods").contentType(MediaType.APPLICATION_JSON).content("""
                            {"fiscalYear":2026,"legalReference":"Art. 600 ET"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("un ano fuera del rango del CHECK sale 400")
        void un_ano_fuera_de_rango_sale_400() throws Exception {
            mockMvc.perform(
                    post("/vat-filing-periods").contentType(MediaType.APPLICATION_JSON).content("""
                            {"fiscalYear":1999,"frequency":"ANNUAL","legalReference":"Art. 600 ET"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }
    }

    @Nested
    @DisplayName("Lectura por ano")
    class LecturaPorAno {

        @Test
        @DisplayName("la ruta exige el ano: la periodicidad es un dato con vigencia")
        void la_ruta_exige_el_ano() throws Exception {
            when(findUseCase.findByYear(anyInt(), any())).thenReturn(bimestral2026());

            mockMvc.perform(get("/vat-filing-periods/years/2026")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.frequency").value("BIMONTHLY"));
        }

        @Test
        @DisplayName("el listado paginado usa el contrato unico de pagina")
        void el_listado_paginado_usa_el_contrato_unico() throws Exception {
            when(listUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(bimestral2026()), 0, 20, 1));

            mockMvc.perform(get("/vat-filing-periods")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
