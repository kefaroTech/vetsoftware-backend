package com.vetsoftware.app.clinicalhistory.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.port.in.ListCompanyClinicalEventsUseCase;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.clinicalhistory.testsupport.ClinicalHistoryMother;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClinicalHistoryCompanyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ClinicalHistoryCompanyController — contrato HTTP")
class ClinicalHistoryCompanyControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListCompanyClinicalEventsUseCase listByCompanyUseCase;

    @Nested
    @DisplayName("GET /clinical-history")
    class ListByCompany {

        @Test
        @DisplayName("sin params la query lleva la company del contexto y types vacío")
        void sin_params_la_query_lleva_la_company_del_contexto() throws Exception {
            when(listByCompanyUseCase.execute(any())).thenReturn(List.of());

            mockMvc.perform(get("/clinical-history")).andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$").isEmpty());

            verify(listByCompanyUseCase)
                    .execute(new ListCompanyClinicalEventsQuery(COMPANY_ID, List.of(), null, null));
        }

        @Test
        @DisplayName("con params arma la query completa y mapea el JSON de salida")
        void con_params_arma_la_query_completa() throws Exception {
            ClinicalEventDto dto = ClinicalEventDto.from(ClinicalHistoryMother.consulta());
            when(listByCompanyUseCase.execute(any())).thenReturn(List.of(dto));

            mockMvc.perform(get("/clinical-history").param("types", "CONSULTATION")
                    .param("from", "2026-08-01").param("to", "2026-08-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].sourceId").value(dto.sourceId()))
                    .andExpect(jsonPath("$[0].eventType").value("CONSULTATION"));

            verify(listByCompanyUseCase).execute(new ListCompanyClinicalEventsQuery(COMPANY_ID,
                    List.of(ClinicalEventType.CONSULTATION), LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 31)));
        }

        @Test
        @DisplayName("la company nunca llega desde el cliente: no hay parámetro companyId")
        void la_company_nunca_llega_desde_el_cliente() throws Exception {
            when(listByCompanyUseCase.execute(any())).thenReturn(List.of());

            // companyId=1 no es un @RequestParam declarado: Spring lo ignora sin fallar,
            // y el query que llega al caso de uso sigue usando la company del contexto
            // (9L).
            mockMvc.perform(get("/clinical-history").param("companyId", "1"))
                    .andExpect(status().isOk());

            verify(listByCompanyUseCase)
                    .execute(new ListCompanyClinicalEventsQuery(COMPANY_ID, List.of(), null, null));
        }
    }
}
