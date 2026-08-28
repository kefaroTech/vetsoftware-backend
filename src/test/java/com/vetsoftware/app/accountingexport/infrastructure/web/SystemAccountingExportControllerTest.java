package com.vetsoftware.app.accountingexport.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.accountingexport.application.command.GenerateAccountingExportCommand;
import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.accountingexport.application.port.in.FindAccountingExportUseCase;
import com.vetsoftware.app.accountingexport.application.port.in.GenerateAccountingExportUseCase;
import com.vetsoftware.app.accountingexport.application.port.in.ListAccountingExportsUseCase;
import com.vetsoftware.app.accountingexport.application.port.in.ResolveAccountingExportUseCase;
import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import com.vetsoftware.app.accountingexport.domain.AccountingExportStatus;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja web de la bandeja de exportaciones.
 *
 * <p>
 * <b>Lo que esta clase congela es que la firma sale del principal y no del
 * cuerpo.</b> {@code generated_by_system_user_id} es lo que sostiene la
 * trazabilidad de quien le entrego que al contador; si el
 * {@code GenerateAccountingExportRequest} llegara a declarar ese campo,
 * cualquiera podria firmar un fichero a nombre de otro superadministrador. El
 * caso captura el command y comprueba que el id es el que devolvio
 * {@code authz.currentSystemUserId()}.
 *
 * <p>
 * Lo segundo es la huella: sesenta y cuatro hexadecimales que el
 * {@code @Pattern} rechaza si no lo son. Sin ella no se puede demostrar que el
 * fichero que tiene el contador es el que se genero.
 */
@WebMvcTest(SystemAccountingExportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemAccountingExportController — contrato HTTP de plataforma")
class SystemAccountingExportControllerTest {

    private static final String HUELLA = "a".repeat(64);
    private static final Long SYSTEM_USER_ID = 990L;

    private static final AccountingExportDto EXPORTACION = new AccountingExportDto(8430L, "2028-05",
            AccountingExportKind.JOURNAL_SUMMARY, 1, AccountingExportStatus.GENERATED,
            LocalDateTime.of(2028, 6, 1, 3, 0), SYSTEM_USER_ID, new BigDecimal("980000.00"),
            new BigDecimal("980000.00"), HUELLA, "s3://contable/2028-05.csv", null, null, null,
            LocalDateTime.of(2028, 6, 1, 3, 0));

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private GenerateAccountingExportUseCase generateUseCase;
    @MockitoBean
    private ResolveAccountingExportUseCase resolveUseCase;
    @MockitoBean
    private FindAccountingExportUseCase findUseCase;
    @MockitoBean
    private ListAccountingExportsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Generacion")
    class Generacion {

        @Test
        @DisplayName("responde 201 y la firma la pone el principal, no el cuerpo")
        void responde_201_y_la_firma_la_pone_el_principal() throws Exception {
            when(authz.currentSystemUserId()).thenReturn(SYSTEM_USER_ID);
            when(generateUseCase.execute(any())).thenReturn(EXPORTACION);

            mockMvc.perform(post("/system/accounting-exports")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "periodKey": "2028-05",
                              "exportKind": "JOURNAL_SUMMARY",
                              "totalDebit": 980000.00,
                              "totalCredit": 980000.00,
                              "totalsHash": "%s",
                              "fileRef": "s3://contable/2028-05.csv"
                            }
                            """.formatted(HUELLA))).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.attemptNumber").value(1))
                    .andExpect(jsonPath("$.status").value("GENERATED"));

            ArgumentCaptor<GenerateAccountingExportCommand> command = ArgumentCaptor
                    .forClass(GenerateAccountingExportCommand.class);
            verify(generateUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.periodKey()).isEqualTo("2028-05");
                assertThat(cmd.exportKind()).isEqualTo(AccountingExportKind.JOURNAL_SUMMARY);
                // La firma sale de authz, nunca del JSON.
                assertThat(cmd.generatedBySystemUserId()).isEqualTo(SYSTEM_USER_ID);
                assertThat(cmd.totalDebit()).isEqualByComparingTo("980000.00");
                assertThat(cmd.totalCredit()).isEqualByComparingTo("980000.00");
                assertThat(cmd.totalsHash()).isEqualTo(HUELLA);
            });
        }

        @Test
        @DisplayName("una huella que no es SHA-256 responde 400 y no llega al caso de uso")
        void una_huella_invalida_responde_400() throws Exception {
            mockMvc.perform(post("/system/accounting-exports")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "periodKey": "2028-05",
                              "exportKind": "JOURNAL_SUMMARY",
                              "totalDebit": 980000.00,
                              "totalCredit": 980000.00,
                              "totalsHash": "NO-ES-UNA-HUELLA",
                              "fileRef": "s3://contable/2028-05.csv"
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(generateUseCase);
        }

        @Test
        @DisplayName("un periodo mal formado responde 400")
        void un_periodo_mal_formado_responde_400() throws Exception {
            mockMvc.perform(post("/system/accounting-exports")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "periodKey": "2028-13",
                              "exportKind": "JOURNAL_SUMMARY",
                              "totalDebit": 980000.00,
                              "totalCredit": 980000.00,
                              "totalsHash": "%s",
                              "fileRef": "s3://contable/2028-05.csv"
                            }
                            """.formatted(HUELLA))).andExpect(status().isBadRequest());

            verifyNoInteractions(generateUseCase);
        }
    }

    @Nested
    @DisplayName("Desenlaces")
    class Desenlaces {

        @Test
        @DisplayName("entregar no lleva cuerpo: la fecha la pone el caso de uso")
        void entregar_no_lleva_cuerpo() throws Exception {
            when(resolveUseCase.markDelivered(8430L)).thenReturn(EXPORTACION);

            mockMvc.perform(patch("/system/accounting-exports/8430/deliver"))
                    .andExpect(status().isOk());

            verify(resolveUseCase).markDelivered(8430L);
        }

        @Test
        @DisplayName("rechazar sin motivo responde 400: un rechazo a ciegas no sirve")
        void rechazar_sin_motivo_responde_400() throws Exception {
            mockMvc.perform(patch("/system/accounting-exports/8430/reject")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"rejectionReason\": \"  \"}")).andExpect(status().isBadRequest());

            verifyNoInteractions(resolveUseCase);
        }

        @Test
        @DisplayName("rechazar con motivo llega al caso de uso con el motivo dentro")
        void rechazar_con_motivo_llega_al_caso_de_uso() throws Exception {
            when(resolveUseCase.markRejected(any())).thenReturn(EXPORTACION);

            mockMvc.perform(patch("/system/accounting-exports/8430/reject")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"rejectionReason\": \"Faltan terceros\"}"))
                    .andExpect(status().isOk());

            verify(resolveUseCase).markRejected(any());
        }
    }
}
