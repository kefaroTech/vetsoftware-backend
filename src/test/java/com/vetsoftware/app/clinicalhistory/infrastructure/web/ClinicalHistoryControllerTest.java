package com.vetsoftware.app.clinicalhistory.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventTypeCountDto;
import com.vetsoftware.app.clinicalhistory.application.port.in.ExportClinicalHistoryUseCase;
import com.vetsoftware.app.clinicalhistory.application.port.in.GetClinicalHistorySummaryUseCase;
import com.vetsoftware.app.clinicalhistory.application.port.in.GetClinicalHistoryUseCase;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.clinicalhistory.testsupport.ClinicalHistoryMother;
import com.vetsoftware.app.shared.pagination.PageResult;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClinicalHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ClinicalHistoryController — contrato HTTP")
class ClinicalHistoryControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long ANIMAL_ID = ClinicalHistoryMother.ANIMAL_ID;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetClinicalHistoryUseCase getUseCase;
    @MockitoBean
    private ExportClinicalHistoryUseCase exportUseCase;
    @MockitoBean
    private GetClinicalHistorySummaryUseCase summaryUseCase;

    private static ClinicalEvent evento() {
        return ClinicalHistoryMother.consulta();
    }

    @Nested
    @DisplayName("GET /animals/{animalId}/clinical-history")
    class Get {

        @Test
        @DisplayName("sin params arma la query con la company del contexto y types vacío")
        void sin_params_arma_la_query_con_la_company_del_contexto() throws Exception {
            ClinicalEvent evt = evento();
            when(getUseCase.execute(any(), org.mockito.ArgumentMatchers.eq(0),
                    org.mockito.ArgumentMatchers.eq(20))).thenReturn(
                            new PageResult<>(List.of(ClinicalEventDto.from(evt)), 0, 20, 1L, 1));

            mockMvc.perform(get("/animals/{animalId}/clinical-history", ANIMAL_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].sourceId").value(evt.sourceId()))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(getUseCase).execute(new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID, List.of(),
                    null, null, null, null), 0, 20);
        }

        @Test
        @DisplayName("con todos los params arma la query completa")
        void con_todos_los_params_arma_la_query_completa() throws Exception {
            when(getUseCase.execute(any(), any(Integer.class), any(Integer.class)))
                    .thenReturn(PageResult.empty(1, 10));

            mockMvc.perform(get("/animals/{animalId}/clinical-history", ANIMAL_ID)
                    .param("types", "CONSULTATION").param("types", "SURGERY")
                    .param("from", "2026-08-01").param("to", "2026-08-31").param("q", "otitis")
                    .param("consultationId", "42").param("page", "1").param("pageSize", "10"))
                    .andExpect(status().isOk());

            verify(getUseCase).execute(
                    new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID,
                            List.of(ClinicalEventType.CONSULTATION, ClinicalEventType.SURGERY),
                            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "otitis", 42L),
                    1, 10);
        }

        @Test
        @DisplayName("un animalId no numérico responde 400 y no llega al caso de uso")
        void animal_id_no_numerico_responde_400() throws Exception {
            mockMvc.perform(get("/animals/{animalId}/clinical-history", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(getUseCase);
        }
    }

    @Nested
    @DisplayName("GET /animals/{animalId}/clinical-history/summary")
    class Summary {

        @Test
        @DisplayName("delega en el caso de uso con animalId y la company del contexto")
        void delega_con_animal_id_y_company() throws Exception {
            when(summaryUseCase.countByType(ANIMAL_ID, COMPANY_ID)).thenReturn(
                    List.of(new ClinicalEventTypeCountDto(ClinicalEventType.CONSULTATION, 3L)));

            mockMvc.perform(get("/animals/{animalId}/clinical-history/summary", ANIMAL_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].eventType").value("CONSULTATION"))
                    .andExpect(jsonPath("$[0].count").value(3));

            verify(summaryUseCase).countByType(ANIMAL_ID, COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("GET /animals/{animalId}/clinical-history/export.pdf")
    class Export {

        @Test
        @DisplayName("responde el PDF con content-disposition de adjunto")
        void responde_el_pdf_con_content_disposition() throws Exception {
            byte[] pdf = {1, 2, 3};
            when(exportUseCase.execute(any())).thenReturn(pdf);

            mockMvc.perform(get("/animals/{animalId}/clinical-history/export.pdf", ANIMAL_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition",
                            org.hamcrest.Matchers
                                    .containsString("historia-clinica-" + ANIMAL_ID + ".pdf")))
                    .andExpect(content().bytes(pdf));
        }

        @Test
        @DisplayName("el query que llega al caso de uso nunca lleva consultationId")
        void el_query_nunca_lleva_consultation_id() throws Exception {
            when(exportUseCase.execute(any())).thenReturn(new byte[0]);

            mockMvc.perform(get("/animals/{animalId}/clinical-history/export.pdf", ANIMAL_ID))
                    .andExpect(status().isOk());

            verify(exportUseCase)
                    .execute(org.mockito.ArgumentMatchers.argThat(q -> q.consultationId() == null));
        }
    }
}
