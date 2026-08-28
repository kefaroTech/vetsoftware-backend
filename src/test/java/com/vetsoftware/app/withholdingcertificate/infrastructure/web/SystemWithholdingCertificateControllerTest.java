package com.vetsoftware.app.withholdingcertificate.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import com.vetsoftware.app.withholdingcertificate.application.command.AttachSubstituteEvidenceCommand;
import com.vetsoftware.app.withholdingcertificate.application.command.ReceiveWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.application.command.RegisterWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.in.AttachSubstituteEvidenceUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListAllWithholdingCertificatesUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListMissingWithholdingCertificatesUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ReceiveWithholdingCertificateUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.RegisterWithholdingCertificateUseCase;
import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
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
 * Rodaja web del camino de <b>escritura</b>, que en este bloque es solo de
 * plataforma: el certificado lo expide un tercero y quien lo registra es
 * tesoreria al conciliar la cartera.
 *
 * <p>
 * Lo que congela esta clase y no ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Que los diez campos del cuerpo llegan al command en su posicion.</b>
 * Cruzar {@code issuedOn} con {@code legalDeadlineOn}, o {@code ratePercent}
 * con {@code certifiedAmount}, compila sin una queja y solo se descubre cuando
 * la retencion no cuadra. Por eso el caso feliz captura el command y compara
 * componente a componente con valores todos distintos entre si.</li>
 * <li><b>Que el {@code companyId} viaja como parametro y no en el cuerpo.</b>
 * Es la forma que permite {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}, y la unica
 * proteccion real es que el puerto este cerrado a {@code hasRole('SYSTEM')} a
 * secas.</li>
 * <li><b>Que las dos segundas escrituras toman el id de la ruta.</b> Un id en
 * el cuerpo y otro en la URL son dos fuentes de verdad para el mismo dato, y la
 * discrepancia no falla: escribe en la fila equivocada.</li>
 * </ul>
 */
@WebMvcTest(SystemWithholdingCertificateController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemWithholdingCertificateController — contrato HTTP de plataforma")
class SystemWithholdingCertificateControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RegisterWithholdingCertificateUseCase registerUseCase;
    @MockitoBean
    private ReceiveWithholdingCertificateUseCase receiveUseCase;
    @MockitoBean
    private AttachSubstituteEvidenceUseCase attachSubstituteUseCase;
    @MockitoBean
    private ListAllWithholdingCertificatesUseCase listAllUseCase;
    @MockitoBean
    private ListMissingWithholdingCertificatesUseCase listMissingUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Registro")
    class Registro {

        @Test
        @DisplayName("responde 201 y traslada los diez campos del cuerpo al command sin cruzarlos")
        void responde_201_y_traslada_los_diez_campos_sin_cruzarlos() throws Exception {
            when(registerUseCase.execute(any())).thenReturn(unCertificado());

            mockMvc.perform(post("/system/withholding-certificates").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "issuedByTaxId": "830012345",
                              "certificateNumber": "CERT-2025-0001",
                              "withholdingType": "ICA",
                              "fiscalYear": 2025,
                              "fiscalPeriodKey": "2025-B03",
                              "ratePercent": 0.690000,
                              "certifiedAmount": 1847320.55,
                              "issuedOn": "2026-02-10",
                              "legalDeadlineOn": "2026-03-31"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(41))
                    .andExpect(jsonPath("$.fiscalPeriodKey").value("2025-B03"));

            ArgumentCaptor<RegisterWithholdingCertificateCommand> command = ArgumentCaptor
                    .forClass(RegisterWithholdingCertificateCommand.class);
            verify(registerUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.companyId()).isEqualTo(900L);
                assertThat(cmd.issuedByTaxId()).isEqualTo("830012345");
                assertThat(cmd.certificateNumber()).isEqualTo("CERT-2025-0001");
                assertThat(cmd.withholdingType()).isEqualTo(WithholdingType.ICA);
                assertThat(cmd.fiscalYear()).isEqualTo(2025);
                assertThat(cmd.fiscalPeriodKey()).isEqualTo("2025-B03");
                assertThat(cmd.ratePercent()).isEqualByComparingTo("0.690000");
                assertThat(cmd.certifiedAmount()).isEqualByComparingTo("1847320.55");
                assertThat(cmd.issuedOn()).isEqualTo(LocalDate.of(2026, 2, 10));
                assertThat(cmd.legalDeadlineOn()).isEqualTo(LocalDate.of(2026, 3, 31));
            });
        }

        @Test
        @DisplayName("un cuerpo sin numero ni fecha limite sale 400 con los dos campos nombrados")
        void un_cuerpo_sin_numero_ni_fecha_limite_sale_400() throws Exception {
            // El @Valid del @RequestBody es lo unico que dispara el validador; sin el,
            // el @NotBlank del DTO esta escrito y no se evalua nunca (#135). Este caso
            // se pone rojo el dia que alguien lo quite.
            mockMvc.perform(post("/system/withholding-certificates").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "issuedByTaxId": "830012345",
                              "certificateNumber": "   ",
                              "withholdingType": "ICA",
                              "fiscalYear": 2025,
                              "fiscalPeriodKey": "2025-B03",
                              "ratePercent": 0.690000,
                              "certifiedAmount": 1847320.55,
                              "issuedOn": "2026-02-10"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field", org.hamcrest.Matchers
                            .containsInAnyOrder("certificateNumber", "legalDeadlineOn")));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("un ano gravable anterior a 2020 sale 400 y no llega al caso de uso")
        void un_ano_gravable_anterior_a_2020_sale_400() throws Exception {
            // Sin ano gravable valido la retencion no se puede imputar a ninguna
            // declaracion. El rango es el mismo del CHECK del changeset 328.
            mockMvc.perform(post("/system/withholding-certificates").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            cuerpoCon("\"fiscalYear\": 2019,", "\"fiscalPeriodKey\": \"2019-A\",")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("fiscalYear"));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("una tarifa con siete decimales sale 400 en vez de perderse redondeada")
        void una_tarifa_con_siete_decimales_sale_400() throws Exception {
            // DECIMAL(9,6) redondearia 0,6912345 a 0,691235 sin avisar, y base por
            // tarifa dejaria de dar el importe certificado.
            mockMvc.perform(post("/system/withholding-certificates").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "issuedByTaxId": "830012345",
                              "certificateNumber": "CERT-2025-0001",
                              "withholdingType": "ICA",
                              "fiscalYear": 2025,
                              "fiscalPeriodKey": "2025-B03",
                              "ratePercent": 0.6912345,
                              "certifiedAmount": 1847320.55,
                              "issuedOn": "2026-02-10",
                              "legalDeadlineOn": "2026-03-31"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("ratePercent"));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("un valor certificado negativo sale 400 y no escribe")
        void un_valor_certificado_negativo_sale_400() throws Exception {
            mockMvc.perform(post("/system/withholding-certificates").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "issuedByTaxId": "830012345",
                              "certificateNumber": "CERT-2025-0001",
                              "withholdingType": "ICA",
                              "fiscalYear": 2025,
                              "fiscalPeriodKey": "2025-B03",
                              "ratePercent": 0.690000,
                              "certifiedAmount": -1.00,
                              "issuedOn": "2026-02-10",
                              "legalDeadlineOn": "2026-03-31"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("certifiedAmount"));

            verifyNoInteractions(registerUseCase);
        }
    }

    @Nested
    @DisplayName("Recepcion")
    class Recepcion {

        @Test
        @DisplayName("cierra la expectativa tomando el id de la ruta y no del cuerpo")
        void cierra_la_expectativa_tomando_el_id_de_la_ruta() throws Exception {
            when(receiveUseCase.execute(any())).thenReturn(unCertificado());

            mockMvc.perform(patch("/system/withholding-certificates/{id}/receive", 41L)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "receivedOn": "2026-03-18",
                              "fileRef": "s3://certificados/CERT.pdf"
                            }
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.receivedOn").value("2026-03-18"));

            ArgumentCaptor<ReceiveWithholdingCertificateCommand> command = ArgumentCaptor
                    .forClass(ReceiveWithholdingCertificateCommand.class);
            verify(receiveUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(41L);
            assertThat(command.getValue().receivedOn()).isEqualTo(LocalDate.of(2026, 3, 18));
            assertThat(command.getValue().fileRef()).isEqualTo("s3://certificados/CERT.pdf");
        }

        @Test
        @DisplayName("recibirlo sin archivo sale 400: la fecha sola no prueba nada")
        void recibirlo_sin_archivo_sale_400() throws Exception {
            mockMvc.perform(patch("/system/withholding-certificates/{id}/receive", 41L)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "receivedOn": "2026-03-18",
                              "fileRef": "  "
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("fileRef"));

            verifyNoInteractions(receiveUseCase);
        }

        @Test
        @DisplayName("recibirlo sin fecha sale 400 y no llega al caso de uso")
        void recibirlo_sin_fecha_sale_400() throws Exception {
            mockMvc.perform(patch("/system/withholding-certificates/{id}/receive", 41L)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "fileRef": "s3://certificados/CERT.pdf"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("receivedOn"));

            verifyNoInteractions(receiveUseCase);
        }
    }

    @Nested
    @DisplayName("Sustituto")
    class Sustituto {

        @Test
        @DisplayName("adjunta el comprobante de pago con el id de la ruta")
        void adjunta_el_comprobante_de_pago_con_el_id_de_la_ruta() throws Exception {
            when(attachSubstituteUseCase.execute(any())).thenReturn(conSustituto());

            mockMvc.perform(patch("/system/withholding-certificates/{id}/substitute-evidence", 41L)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "evidenceKind": "PAYMENT_RECEIPT",
                              "evidenceRef": "s3://pagos/2025/REC-77120.pdf"
                            }
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.substituteEvidenceKind").value("PAYMENT_RECEIPT"))
                    .andExpect(jsonPath("$.substituteEvidenceRef")
                            .value("s3://pagos/2025/REC-77120.pdf"))
                    .andExpect(jsonPath("$.supported").value(true));

            ArgumentCaptor<AttachSubstituteEvidenceCommand> command = ArgumentCaptor
                    .forClass(AttachSubstituteEvidenceCommand.class);
            verify(attachSubstituteUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(41L);
            assertThat(command.getValue().evidenceKind())
                    .isEqualTo(SubstituteEvidenceKind.PAYMENT_RECEIPT);
            assertThat(command.getValue().evidenceRef()).isEqualTo("s3://pagos/2025/REC-77120.pdf");
        }

        @Test
        @DisplayName("un soporte que no existe en la lista cerrada sale 400")
        void un_soporte_que_no_existe_sale_400() throws Exception {
            // La lista cerrada tiene un solo valor porque el comprobante de pago es el
            // unico sustituto que la ley admite: la factura la emite un tercero y no
            // lleva la retencion.
            mockMvc.perform(patch("/system/withholding-certificates/{id}/substitute-evidence", 41L)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "evidenceKind": "INVOICE",
                              "evidenceRef": "s3://facturas/F-1.pdf"
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(attachSubstituteUseCase);
        }

        @Test
        @DisplayName("un sustituto sin referencia sale 400 y no escribe")
        void un_sustituto_sin_referencia_sale_400() throws Exception {
            mockMvc.perform(patch("/system/withholding-certificates/{id}/substitute-evidence", 41L)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "evidenceKind": "PAYMENT_RECEIPT",
                              "evidenceRef": "  "
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("evidenceRef"));

            verifyNoInteractions(attachSubstituteUseCase);
        }
    }

    @Nested
    @DisplayName("Barrido de plataforma")
    class BarridoDePlataforma {

        @Test
        @DisplayName("sin companyId barre todas las empresas y lo dice pasando null")
        void sin_company_id_barre_todas_las_empresas() throws Exception {
            when(listAllUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unCertificado()), 0, 20, 1L));

            mockMvc.perform(get("/system/withholding-certificates")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].companyId").value(900))
                    .andExpect(jsonPath("$.totalElements").value(1));

            // null y no 0L: un 0 filtraria por una empresa inexistente y el barrido
            // saldria vacio sin que nadie lo notara.
            verify(listAllUseCase).listAll(null, 0, 20);
        }

        @Test
        @DisplayName("con companyId acota el barrido a esa empresa")
        void con_company_id_acota_el_barrido() throws Exception {
            when(listAllUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(3, 7));

            mockMvc.perform(get("/system/withholding-certificates").param("companyId", "901")
                    .param("page", "3").param("pageSize", "7")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page").value(3))
                    .andExpect(jsonPath("$.pageSize").value(7));

            verify(listAllUseCase).listAll(901L, 3, 7);
        }

        @Test
        @DisplayName("el barrido de vencimientos traslada la fecha de corte tal cual")
        void el_barrido_de_vencimientos_traslada_la_fecha_de_corte() throws Exception {
            when(listMissingUseCase.listMissing(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unCertificado()), 0, 20, 1L));

            mockMvc.perform(get("/system/withholding-certificates/missing").param("deadlineBefore",
                    "2026-03-31")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(41));

            verify(listMissingUseCase).listMissing(LocalDate.of(2026, 3, 31), 0, 20);
        }

        @Test
        @DisplayName("el barrido de vencimientos sin fecha de corte es una peticion mal formada")
        void el_barrido_sin_fecha_de_corte_es_una_peticion_mal_formada() throws Exception {
            mockMvc.perform(get("/system/withholding-certificates/missing"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(listMissingUseCase);
        }
    }

    private static String cuerpoCon(String ano, String periodo) {
        return "{\"issuedByTaxId\": \"830012345\", \"certificateNumber\": \"CERT-0001\","
                + " \"withholdingType\": \"INCOME_TAX\", " + ano + " " + periodo
                + " \"ratePercent\": 2.5, \"certifiedAmount\": 1000.00,"
                + " \"issuedOn\": \"2026-02-10\", \"legalDeadlineOn\": \"2026-03-31\"}";
    }

    private static WithholdingCertificateDto unCertificado() {
        return new WithholdingCertificateDto(41L, 900L, "830012345", "CERT-2025-0001",
                WithholdingType.ICA, 2025, "2025-B03", new BigDecimal("0.690000"),
                new BigDecimal("1847320.55"), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 3, 18), "s3://certificados/CERT.pdf", null, null, true,
                LocalDateTime.of(2026, 2, 12, 9, 15, 30));
    }

    private static WithholdingCertificateDto conSustituto() {
        return new WithholdingCertificateDto(41L, 900L, "830012345", "CERT-2025-0001",
                WithholdingType.ICA, 2025, "2025-B03", new BigDecimal("0.690000"),
                new BigDecimal("1847320.55"), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 31),
                null, null, SubstituteEvidenceKind.PAYMENT_RECEIPT, "s3://pagos/2025/REC-77120.pdf",
                true, LocalDateTime.of(2026, 2, 12, 9, 15, 30));
    }
}
