package com.vetsoftware.app.supplierwithholding.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplierwithholding.application.command.PracticeSupplierWithholdingCommand;
import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import com.vetsoftware.app.supplierwithholding.application.port.in.FindSupplierWithholdingUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.in.IssueSupplierWithholdingCertificateUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.in.ListSupplierWithholdingsUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.in.PracticeSupplierWithholdingUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.in.RegisterSupplierWithholdingPaymentUseCase;
import com.vetsoftware.app.supplierwithholding.domain.SupplierDocumentKind;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingType;
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
 * Rodaja web de las retenciones que practicamos.
 *
 * <p>
 * <b>Lo que esta clase congela es que la tarifa cruza la frontera HTTP con sus
 * seis decimales.</b> Es el numero que se pierde si alguien lo declara
 * {@code double} o le pone un formato: {@code 0.690000} redondeado a
 * {@code 0.69} sobrevive, pero {@code 0.414000} se corta a {@code 0.41} y se
 * retiene casi un uno por ciento de menos por factura, sin un solo error.
 *
 * <p>
 * Lo segundo son los trece campos del cuerpo llegando al command en su
 * posicion: {@code taxableBase} y {@code amount} son dos {@code BigDecimal}
 * consecutivos y cruzarlos compila —el resultado seria una retencion mayor que
 * la base, que es exactamente lo que {@code chk_sw_amounts} rechaza mucho mas
 * tarde.
 */
@WebMvcTest(SystemSupplierWithholdingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemSupplierWithholdingController — contrato HTTP de plataforma")
class SystemSupplierWithholdingControllerTest {

    private static final SupplierWithholdingDto RETENCION = new SupplierWithholdingDto(8450L,
            "900123456-7", "Proveedor de andamio", SupplierDocumentKind.NIT, "FV-001",
            SupplierWithholdingType.INCOME_TAX, "Servicios profesionales",
            new BigDecimal("1000000.00"), new BigDecimal("4.000000"), new BigDecimal("40000.00"),
            null, 2026, "2026-M03", LocalDate.of(2026, 3, 15), null, null, null,
            LocalDateTime.of(2026, 3, 15, 10, 0));

    private static final SupplierWithholdingDto ICA = new SupplierWithholdingDto(8451L,
            "900123456-7", "Proveedor de andamio", SupplierDocumentKind.NIT, "FV-ICA-001",
            SupplierWithholdingType.ICA, "Servicios profesionales", new BigDecimal("1000000.00"),
            new BigDecimal("0.414000"), new BigDecimal("4140.00"), "11001", 2026, "2026-B02",
            LocalDate.of(2026, 3, 15), null, null, null, LocalDateTime.of(2026, 3, 15, 10, 0));

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PracticeSupplierWithholdingUseCase practiceUseCase;
    @MockitoBean
    private IssueSupplierWithholdingCertificateUseCase issueCertificateUseCase;
    @MockitoBean
    private RegisterSupplierWithholdingPaymentUseCase registerPaymentUseCase;
    @MockitoBean
    private FindSupplierWithholdingUseCase findUseCase;
    @MockitoBean
    private ListSupplierWithholdingsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("responde 201 y traslada los trece campos del cuerpo sin cruzarlos")
        void responde_201_y_traslada_los_trece_campos_sin_cruzarlos() throws Exception {
            when(practiceUseCase.execute(any())).thenReturn(RETENCION);

            mockMvc.perform(post("/system/supplier-withholdings")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "supplierTaxId": "900123456-7",
                              "supplierName": "Proveedor de andamio",
                              "supplierDocType": "NIT",
                              "supplierInvoiceRef": "FV-001",
                              "withholdingType": "INCOME_TAX",
                              "concept": "Servicios profesionales",
                              "taxableBase": 1000000.00,
                              "ratePercent": 4.000000,
                              "amount": 40000.00,
                              "fiscalYear": 2026,
                              "fiscalPeriodKey": "2026-M03",
                              "practicedOn": "2026-03-15"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(8450))
                    .andExpect(jsonPath("$.fiscalPeriodKey").value("2026-M03"));

            ArgumentCaptor<PracticeSupplierWithholdingCommand> command = ArgumentCaptor
                    .forClass(PracticeSupplierWithholdingCommand.class);
            verify(practiceUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.supplierTaxId()).isEqualTo("900123456-7");
                assertThat(cmd.supplierDocType()).isEqualTo(SupplierDocumentKind.NIT);
                assertThat(cmd.supplierInvoiceRef()).isEqualTo("FV-001");
                assertThat(cmd.withholdingType()).isEqualTo(SupplierWithholdingType.INCOME_TAX);
                // Base y retenido, sin cruzar: el error que compila y que produce una
                // retencion mayor que la base.
                assertThat(cmd.taxableBase()).isEqualByComparingTo("1000000.00");
                assertThat(cmd.amount()).isEqualByComparingTo("40000.00");
                assertThat(cmd.municipalityCode()).isNull();
                assertThat(cmd.fiscalYear()).isEqualTo(2026);
                assertThat(cmd.fiscalPeriodKey()).isEqualTo("2026-M03");
                assertThat(cmd.practicedOn()).isEqualTo(LocalDate.of(2026, 3, 15));
            });
        }

        @Test
        @DisplayName("el 4,14 por mil cruza la frontera HTTP con sus seis decimales")
        void el_414_por_mil_cruza_con_sus_seis_decimales() throws Exception {
            when(practiceUseCase.execute(any())).thenReturn(ICA);

            mockMvc.perform(post("/system/supplier-withholdings")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "supplierTaxId": "900123456-7",
                              "supplierName": "Proveedor de andamio",
                              "supplierDocType": "NIT",
                              "supplierInvoiceRef": "FV-ICA-001",
                              "withholdingType": "ICA",
                              "concept": "Servicios profesionales",
                              "taxableBase": 1000000.00,
                              "ratePercent": 0.414000,
                              "amount": 4140.00,
                              "municipalityCode": "11001",
                              "fiscalYear": 2026,
                              "fiscalPeriodKey": "2026-B02",
                              "practicedOn": "2026-03-15"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ratePercent").value(0.414000));

            ArgumentCaptor<PracticeSupplierWithholdingCommand> command = ArgumentCaptor
                    .forClass(PracticeSupplierWithholdingCommand.class);
            verify(practiceUseCase).execute(command.capture());
            assertThat(command.getValue().ratePercent()).isEqualByComparingTo("0.414000");
            assertThat(command.getValue().ratePercent().scale()).isEqualTo(6);
        }

        @Test
        @DisplayName("una retencion sin factura responde 400: sin soporte no hay deduccion")
        void una_retencion_sin_factura_responde_400() throws Exception {
            mockMvc.perform(post("/system/supplier-withholdings")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "supplierTaxId": "900123456-7",
                              "supplierName": "Proveedor de andamio",
                              "supplierDocType": "NIT",
                              "supplierInvoiceRef": "  ",
                              "withholdingType": "INCOME_TAX",
                              "concept": "Servicios profesionales",
                              "taxableBase": 1000000.00,
                              "ratePercent": 4.000000,
                              "amount": 40000.00,
                              "fiscalYear": 2026,
                              "fiscalPeriodKey": "2026-M03",
                              "practicedOn": "2026-03-15"
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(practiceUseCase);
        }
    }

    @Nested
    @DisplayName("Documentos que llegan tarde")
    class DocumentosQueLleganTarde {

        @Test
        @DisplayName("el certificado se emite sin fecha: la pone el caso de uso")
        void el_certificado_se_emite_sin_fecha() throws Exception {
            when(issueCertificateUseCase.execute(any())).thenReturn(RETENCION);

            mockMvc.perform(patch("/system/supplier-withholdings/8450/certificate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"certificateRef\": \"CERT-2026-0001\"}"))
                    .andExpect(status().isOk());

            verify(issueCertificateUseCase).execute(any());
        }

        @Test
        @DisplayName("el certificado sin numero responde 400 y no llega al caso de uso")
        void el_certificado_sin_numero_responde_400() throws Exception {
            mockMvc.perform(patch("/system/supplier-withholdings/8450/certificate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"certificateRef\": \"  \"}")).andExpect(status().isBadRequest());

            verifyNoInteractions(issueCertificateUseCase);
        }
    }

    @Nested
    @DisplayName("Lectura")
    class Lectura {

        @Test
        @DisplayName("el certificado anual del proveedor exige el año gravable")
        void el_certificado_anual_exige_el_ano_gravable() throws Exception {
            when(listUseCase.listBySupplierAndYear("900123456-7", 2026, 0, 20))
                    .thenReturn(PageResult.of(List.of(RETENCION), 0, 20, 1L));

            mockMvc.perform(get("/system/supplier-withholdings/by-supplier/900123456-7")
                    .param("fiscalYear", "2026")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));

            mockMvc.perform(get("/system/supplier-withholdings/by-supplier/900123456-7"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("la declaracion del mes va por periodo fiscal")
        void la_declaracion_del_mes_va_por_periodo_fiscal() throws Exception {
            when(listUseCase.listByFiscalPeriod("2026-M03", 0, 20))
                    .thenReturn(PageResult.of(List.of(RETENCION), 0, 20, 1L));

            mockMvc.perform(get("/system/supplier-withholdings/by-period/2026-M03"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].supplierInvoiceRef").value("FV-001"));

            verify(listUseCase).listByFiscalPeriod("2026-M03", 0, 20);
        }
    }
}
