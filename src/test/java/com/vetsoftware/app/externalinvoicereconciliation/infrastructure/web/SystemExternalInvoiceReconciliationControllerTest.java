package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.externalinvoicereconciliation.application.command.MatchExternalInvoiceCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.command.OpenExternalInvoiceReconciliationCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.command.ResolveExternalInvoiceReconciliationCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.FindExternalInvoiceReconciliationUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.ListExternalInvoiceReconciliationsUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.ListMissingExternalInvoicesUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.MatchExternalInvoiceUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.OpenExternalInvoiceReconciliationUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.ResolveExternalInvoiceReconciliationUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
 * Rodaja web de la conciliacion. <b>Es la unica que hay, y esa es la mitad de
 * lo que congela esta clase</b>: en este bloque no existe controller de tenant,
 * asi que no hay gemelo bajo {@code /external-invoice-reconciliations} que
 * probar. La conciliacion es el cuadre entre VetSoftware y su facturador
 * externo, y ensenarsela al cliente es ensenarle el margen y los datos de
 * terceros.
 *
 * <p>
 * Lo que congela y no ve ningun test de servicio: <b>que los campos del cuerpo
 * llegan al command en su posicion</b>. Los tres commands mezclan varios
 * {@code Long}, dos {@code Integer} y cuatro importes; cruzar
 * {@code externalTotal} con {@code externalTax}, o {@code externalRangeFrom}
 * con {@code externalRangeTo}, compila sin una queja y solo se descubre
 * cuadrando la caja. Por eso cada caso feliz captura el command y compara
 * componente a componente con valores todos distintos entre si.
 *
 * <p>
 * <b>Lo que esta clase NO comprueba</b>: el mapeo de las excepciones de dominio
 * a su codigo HTTP. Las cuatro
 * —{@code ExternalInvoiceReconciliationNotFoundException} (404),
 * {@code ExternalInvoiceReconciliationAlreadyExistsException},
 * {@code ExternalInvoiceAlreadyMatchedException} y
 * {@code ExternalInvoiceReconciliationAlreadyResolvedException} (409)— todavia
 * no estan cableadas en {@code GlobalExceptionHandler}. Cuando lo esten, aqui
 * van sus casos.
 */
@WebMvcTest(SystemExternalInvoiceReconciliationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemExternalInvoiceReconciliationController — contrato HTTP de plataforma")
class SystemExternalInvoiceReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OpenExternalInvoiceReconciliationUseCase openUseCase;
    @MockitoBean
    private MatchExternalInvoiceUseCase matchUseCase;
    @MockitoBean
    private ResolveExternalInvoiceReconciliationUseCase resolveUseCase;
    @MockitoBean
    private FindExternalInvoiceReconciliationUseCase findUseCase;
    @MockitoBean
    private ListExternalInvoiceReconciliationsUseCase listUseCase;
    @MockitoBean
    private ListMissingExternalInvoicesUseCase listMissingUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("responde 201 y toma la empresa del parametro, nunca del cuerpo")
        void responde_201_y_toma_la_empresa_del_parametro() throws Exception {
            when(openUseCase.execute(any())).thenReturn(abierta());

            mockMvc.perform(post("/system/external-invoice-reconciliations")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billingDocumentId": 8600,
                              "computedTotal": 119000.00,
                              "computedTax": 19000.00
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("MISSING_EXTERNAL"))
                    .andExpect(jsonPath("$.computedTotal").value(119000.00));

            ArgumentCaptor<OpenExternalInvoiceReconciliationCommand> command = ArgumentCaptor
                    .forClass(OpenExternalInvoiceReconciliationCommand.class);
            verify(openUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.companyId()).isEqualTo(900L);
                assertThat(cmd.billingDocumentId()).isEqualTo(8600L);
                assertThat(cmd.computedTotal()).isEqualByComparingTo("119000.00");
                assertThat(cmd.computedTax()).isEqualByComparingTo("19000.00");
            });
        }

        @Test
        @DisplayName("un companyId escrito en el cuerpo no manda: la empresa viaja en el parametro")
        void un_company_id_escrito_en_el_cuerpo_no_manda() throws Exception {
            // EMPRESA_NO_VIAJA_EN_EL_CUERPO: el request ni siquiera declara el campo,
            // asi que un cliente que lo mande no consigue nada. Manda el @RequestParam.
            when(openUseCase.execute(any())).thenReturn(abierta());

            mockMvc.perform(post("/system/external-invoice-reconciliations")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "companyId": 901,
                              "billingDocumentId": 8600,
                              "computedTotal": 119000.00,
                              "computedTax": 19000.00
                            }
                            """)).andExpect(status().isCreated());

            ArgumentCaptor<OpenExternalInvoiceReconciliationCommand> command = ArgumentCaptor
                    .forClass(OpenExternalInvoiceReconciliationCommand.class);
            verify(openUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(900L);
        }

        @Test
        @DisplayName("un cuerpo sin documento ni total sale 400 con los dos campos nombrados")
        void un_cuerpo_sin_documento_ni_total_sale_400() throws Exception {
            // El @Valid del @RequestBody es lo unico que dispara el validador; sin el,
            // el @NotNull del DTO esta escrito y no se evalua nunca (#135). Este caso se
            // pone rojo el dia que alguien lo quite.
            mockMvc.perform(post("/system/external-invoice-reconciliations")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "computedTax": 19000.00
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field", org.hamcrest.Matchers
                            .containsInAnyOrder("billingDocumentId", "computedTotal")));

            verifyNoInteractions(openUseCase);
        }

        @Test
        @DisplayName("un total negativo sale 400 y no llega al caso de uso")
        void un_total_negativo_sale_400_y_no_llega_al_caso_de_uso() throws Exception {
            mockMvc.perform(post("/system/external-invoice-reconciliations")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billingDocumentId": 8600,
                              "computedTotal": -1.00,
                              "computedTax": 19000.00
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("computedTotal"));

            verifyNoInteractions(openUseCase);
        }
    }

    @Nested
    @DisplayName("Factura del tercero")
    class FacturaDelTercero {

        @Test
        @DisplayName("traslada los ocho campos al command sin cruzarlos y sin mandar el estado")
        void traslada_los_ocho_campos_sin_cruzarlos() throws Exception {
            when(matchUseCase.execute(any())).thenReturn(conFactura());

            mockMvc.perform(post("/system/external-invoice-reconciliations/41/external-invoice")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "externalInvoiceId": "FE-1043",
                              "externalCufe": "CUFE-0011",
                              "externalTotal": 118998.00,
                              "externalTax": 18998.31,
                              "externalResolutionNumber": "18764000000123",
                              "externalRangeFrom": 1000,
                              "externalRangeTo": 5000,
                              "resolutionValidUntil": "2027-01-31"
                            }
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("WITHIN_TOLERANCE"))
                    .andExpect(jsonPath("$.difference").value(2.00));

            ArgumentCaptor<MatchExternalInvoiceCommand> command = ArgumentCaptor
                    .forClass(MatchExternalInvoiceCommand.class);
            verify(matchUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.id()).isEqualTo(41L);
                assertThat(cmd.externalInvoiceId()).isEqualTo("FE-1043");
                assertThat(cmd.externalCufe()).isEqualTo("CUFE-0011");
                assertThat(cmd.externalTotal()).isEqualByComparingTo("118998.00");
                assertThat(cmd.externalTax()).isEqualByComparingTo("18998.31");
                assertThat(cmd.externalResolutionNumber()).isEqualTo("18764000000123");
                assertThat(cmd.externalRangeFrom()).isEqualTo(1000);
                assertThat(cmd.externalRangeTo()).isEqualTo(5000);
                assertThat(cmd.resolutionValidUntil()).isEqualTo(LocalDate.of(2027, 1, 31));
            });
        }

        @Test
        @DisplayName("sin numero de factura externa sale 400 y no llega al caso de uso")
        void sin_numero_de_factura_externa_sale_400() throws Exception {
            mockMvc.perform(post("/system/external-invoice-reconciliations/41/external-invoice")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "externalInvoiceId": "   ",
                              "externalTotal": 118998.00,
                              "externalTax": 18998.31
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("externalInvoiceId"));

            verifyNoInteractions(matchUseCase);
        }
    }

    @Nested
    @DisplayName("Resolucion")
    class Resolucion {

        @Test
        @DisplayName("traslada firma, nota y periodo, y no acepta ninguna fecha del cliente")
        void traslada_firma_nota_y_periodo() throws Exception {
            when(resolveUseCase.execute(any())).thenReturn(resuelta());

            mockMvc.perform(post("/system/external-invoice-reconciliations/41/resolution")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "resolvedBySystemUserId": 990,
                              "resolutionNote": "Ajuste por redondeo del impuesto",
                              "postingPeriod": "2026-03"
                            }
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.postingPeriod").value("2026-03"))
                    // La fecha es la del reloj del servidor: el request ni siquiera
                    // declara el campo, asi que un intento de antedatar no llega a
                    // ninguna parte.
                    .andExpect(jsonPath("$.resolvedAt").value("2026-04-11T09:20:45"));

            ArgumentCaptor<ResolveExternalInvoiceReconciliationCommand> command = ArgumentCaptor
                    .forClass(ResolveExternalInvoiceReconciliationCommand.class);
            verify(resolveUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.id()).isEqualTo(41L);
                assertThat(cmd.resolvedBySystemUserId()).isEqualTo(990L);
                assertThat(cmd.resolutionNote()).isEqualTo("Ajuste por redondeo del impuesto");
                assertThat(cmd.postingPeriod()).isEqualTo("2026-03");
            });
        }

        @Test
        @DisplayName("el request de resolucion no declara ninguna fecha que el cliente pueda enviar")
        void el_request_no_declara_ninguna_fecha() {
            // resolvedAt es la mitad del par que decide en que cierre queda el ajuste;
            // si el cliente lo pudiera escribir, un descuadre de abril se antedataria a
            // marzo y se colaria en un periodo ya cerrado. La forma de impedirlo es que
            // el tipo no lo admita.
            assertThat(
                    com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web.request.ResolveExternalInvoiceReconciliationRequest.class
                            .getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .doesNotContain("resolvedAt");
        }

        @Test
        @DisplayName("un periodo con mes 13 lo para el binder, antes del dominio")
        void un_periodo_con_mes_13_lo_para_el_binder() throws Exception {
            // El @Pattern del request es el mismo REGEXP de chk_eir_resolved. Que lo
            // pare aqui significa que el operador lee «AAAA-MM» y no un 500.
            mockMvc.perform(post("/system/external-invoice-reconciliations/41/resolution")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "resolvedBySystemUserId": 990,
                              "resolutionNote": "Ajuste",
                              "postingPeriod": "2026-13"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("postingPeriod"));

            verifyNoInteractions(resolveUseCase);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("la bandeja de MISSING_EXTERNAL tiene ruta propia y no la come el /{id}")
        void la_bandeja_tiene_ruta_propia() throws Exception {
            // /missing-external es literal y /{id} es plantilla: Spring prefiere el
            // literal. Si algun dia dejara de hacerlo, la bandeja intentaria resolver
            // «missing-external» como un Long y saldria un 400 sin explicacion.
            when(listMissingUseCase.listMissing(anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(abierta()), 0, 20, 214L));

            mockMvc.perform(get("/system/external-invoice-reconciliations/missing-external"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("MISSING_EXTERNAL"))
                    .andExpect(jsonPath("$.totalElements").value(214));

            verify(listMissingUseCase).listMissing(0, 20);
            verifyNoInteractions(findUseCase);
        }

        @Test
        @DisplayName("sin companyId el barrido pasa null, no cero")
        void sin_company_id_el_barrido_pasa_null() throws Exception {
            // Un 0L filtraria por una empresa inexistente y el barrido saldria vacio sin
            // que nadie lo notara.
            when(listUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(conFactura()), 0, 20, 1L));

            mockMvc.perform(get("/system/external-invoice-reconciliations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].companyId").value(900));

            verify(listUseCase).listAll(null, 0, 20);
        }

        @Test
        @DisplayName("con companyId, pagina y tamano, los traslada tal cual")
        void con_company_id_pagina_y_tamano_los_traslada() throws Exception {
            when(listUseCase.listAll(any(), anyInt(), anyInt())).thenReturn(PageResult.empty(3, 7));

            mockMvc.perform(get("/system/external-invoice-reconciliations")
                    .param("companyId", "901").param("page", "3").param("pageSize", "7"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page").value(3))
                    .andExpect(jsonPath("$.pageSize").value(7));

            verify(listUseCase).listAll(901L, 3, 7);
        }

        @Test
        @DisplayName("la lectura por id publica los cuatro numeros enfrentados")
        void la_lectura_por_id_publica_los_cuatro_numeros() throws Exception {
            // Estos cuatro campos son la razon por la que este bloque NO tiene camino de
            // tenant: publicarlos al cliente es ensenarle el margen y el detalle fiscal
            // de un tercero.
            when(findUseCase.findById(41L)).thenReturn(conFactura());

            mockMvc.perform(get("/system/external-invoice-reconciliations/41"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(41))
                    .andExpect(jsonPath("$.computedTotal").value(119000.00))
                    .andExpect(jsonPath("$.computedTax").value(19000.00))
                    .andExpect(jsonPath("$.externalTotal").value(118998.00))
                    .andExpect(jsonPath("$.externalTax").value(18998.31))
                    .andExpect(jsonPath("$.difference").value(2.00))
                    .andExpect(jsonPath("$.status").value("WITHIN_TOLERANCE"));
        }
    }

    // --- andamio ------------------------------------------------------------

    private static ExternalInvoiceReconciliationDto abierta() {
        return new ExternalInvoiceReconciliationDto(41L, 900L, 8600L, null, null, null, null, null,
                null, new BigDecimal("119000.00"), new BigDecimal("19000.00"), null, null, null,
                ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL, null, null, null, null,
                LocalDateTime.of(2026, 3, 5, 14, 30, 15));
    }

    private static ExternalInvoiceReconciliationDto conFactura() {
        return new ExternalInvoiceReconciliationDto(41L, 900L, 8600L, "18764000000123", 1000, 5000,
                LocalDate.of(2027, 1, 31), "FE-1043", "CUFE-0011", new BigDecimal("119000.00"),
                new BigDecimal("19000.00"), new BigDecimal("118998.00"), new BigDecimal("18998.31"),
                new BigDecimal("2.00"), ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE, null,
                null, null, null, LocalDateTime.of(2026, 3, 5, 14, 30, 15));
    }

    private static ExternalInvoiceReconciliationDto resuelta() {
        return new ExternalInvoiceReconciliationDto(41L, 900L, 8600L, null, null, null, null,
                "FE-1043", "CUFE-0011", new BigDecimal("119000.00"), new BigDecimal("19000.00"),
                new BigDecimal("118998.00"), new BigDecimal("18998.31"), new BigDecimal("2.00"),
                ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE, 990L,
                LocalDateTime.of(2026, 4, 11, 9, 20, 45), "Ajuste por redondeo del impuesto",
                "2026-03", LocalDateTime.of(2026, 3, 5, 14, 30, 15));
    }
}
