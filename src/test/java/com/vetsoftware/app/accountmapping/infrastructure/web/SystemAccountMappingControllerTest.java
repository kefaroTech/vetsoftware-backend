package com.vetsoftware.app.accountmapping.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.accountmapping.application.command.CreateAccountMappingCommand;
import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.in.CloseAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.application.port.in.CreateAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.application.port.in.FindAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.application.port.in.ListAccountMappingsUseCase;
import com.vetsoftware.app.accountmapping.application.port.in.ResolveAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
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
 * Rodaja web del puente concepto → cuenta.
 *
 * <p>
 * <b>Lo que esta clase congela es que los tres afinados opcionales viajan como
 * {@code null} y no como cadena vacia</b>, y que el {@code on} ausente <b>no lo
 * resuelve el controller</b>: el {@code null} llega intacto al caso de uso, que
 * es quien decide que dia es hoy con su {@code Clock} inyectado. Un
 * {@code LocalDate.now()} en la capa web es una fecha que ningun test puede
 * fijar, y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el build por ello.
 *
 * <p>
 * <b>Lo que NO cubre todavia:</b> el 404 de
 * {@code NoEffectiveAccountMappingException} y el de
 * {@code AccountMappingNotFoundException}; {@code GlobalExceptionHandler} aun
 * no las enumera y hoy saldrian como 500.
 */
@WebMvcTest(SystemAccountMappingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemAccountMappingController — contrato HTTP de plataforma")
class SystemAccountMappingControllerTest {

    private static final AccountMappingDto IVA = new AccountMappingDto(8410L,
            MappingKind.VAT_PAYABLE, "19", null, null, null, "13050501", "24080501", null,
            LocalDate.of(2026, 1, 1), null, LocalDateTime.of(2026, 1, 1, 8, 0), true);

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateAccountMappingUseCase createUseCase;
    @MockitoBean
    private CloseAccountMappingUseCase closeUseCase;
    @MockitoBean
    private FindAccountMappingUseCase findUseCase;
    @MockitoBean
    private ListAccountMappingsUseCase listUseCase;
    @MockitoBean
    private ResolveAccountMappingUseCase resolveUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("responde 201 y los tres afinados ausentes llegan como null")
        void responde_201_y_los_tres_afinados_ausentes_llegan_como_null() throws Exception {
            when(createUseCase.execute(any())).thenReturn(IVA);

            mockMvc.perform(post("/system/account-mappings").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "mappingKind": "VAT_PAYABLE",
                              "mappingKey": "19",
                              "debitAccountCode": "13050501",
                              "creditAccountCode": "24080501",
                              "validFrom": "2026-01-01"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mappingKey").value("19"));

            ArgumentCaptor<CreateAccountMappingCommand> command = ArgumentCaptor
                    .forClass(CreateAccountMappingCommand.class);
            verify(createUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.mappingKind()).isEqualTo(MappingKind.VAT_PAYABLE);
                assertThat(cmd.mappingKey()).isEqualTo("19");
                // Null y no cadena vacia: el adaptador los traduce a los centinelas de
                // las columnas generadas, y "" no es lo mismo que ausente.
                assertThat(cmd.catalogItemId()).isNull();
                assertThat(cmd.chargeType()).isNull();
                assertThat(cmd.taxTreatment()).isNull();
                assertThat(cmd.deferredAccountCode()).isNull();
                assertThat(cmd.debitAccountCode()).isEqualTo("13050501");
                assertThat(cmd.creditAccountCode()).isEqualTo("24080501");
            });
        }

        @Test
        @DisplayName("una subclave vacia responde 400 y no llega al caso de uso")
        void una_subclave_vacia_responde_400() throws Exception {
            mockMvc.perform(post("/system/account-mappings").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "mappingKind": "VAT_PAYABLE",
                              "mappingKey": "  ",
                              "debitAccountCode": "13050501",
                              "creditAccountCode": "24080501",
                              "validFrom": "2026-01-01"
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }
    }

    @Nested
    @DisplayName("Resolucion")
    class Resolucion {

        @Test
        @DisplayName("sin fecha, el null viaja al caso de uso: el controller no mira el reloj")
        void sin_fecha_el_null_viaja_al_caso_de_uso() throws Exception {
            when(resolveUseCase.resolve(any(), any(), any(), any(), any(), any())).thenReturn(IVA);

            mockMvc.perform(get("/system/account-mappings/effective")
                    .param("mappingKind", "VAT_PAYABLE").param("mappingKey", "19"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.debitAccountCode").value("13050501"));

            verify(resolveUseCase).resolve(eq(MappingKind.VAT_PAYABLE), eq("19"), isNull(),
                    isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("con fecha, la fecha del hecho economico llega intacta")
        void con_fecha_la_fecha_del_hecho_llega_intacta() throws Exception {
            when(resolveUseCase.resolve(any(), any(), any(), any(), any(), any())).thenReturn(IVA);

            mockMvc.perform(
                    get("/system/account-mappings/effective").param("mappingKind", "VAT_PAYABLE")
                            .param("mappingKey", "19").param("on", "2026-06-15"))
                    .andExpect(status().isOk());

            verify(resolveUseCase).resolve(eq(MappingKind.VAT_PAYABLE), eq("19"), isNull(),
                    isNull(), isNull(), eq(LocalDate.of(2026, 6, 15)));
        }
    }
}
