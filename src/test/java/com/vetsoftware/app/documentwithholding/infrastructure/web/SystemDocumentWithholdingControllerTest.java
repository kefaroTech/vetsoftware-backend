package com.vetsoftware.app.documentwithholding.infrastructure.web;

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
import com.vetsoftware.app.documentwithholding.application.command.LinkWithholdingCertificateCommand;
import com.vetsoftware.app.documentwithholding.application.command.RegisterDocumentWithholdingCommand;
import com.vetsoftware.app.documentwithholding.application.port.in.LinkWithholdingCertificateUseCase;
import com.vetsoftware.app.documentwithholding.application.port.in.ListAllDocumentWithholdingsUseCase;
import com.vetsoftware.app.documentwithholding.application.port.in.ListUncertifiedDocumentWithholdingsUseCase;
import com.vetsoftware.app.documentwithholding.application.port.in.RegisterDocumentWithholdingUseCase;
import com.vetsoftware.app.documentwithholding.domain.WithholdingType;
import com.vetsoftware.app.documentwithholding.testsupport.DocumentWithholdingMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
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
 * plataforma: registrar una retencion es declarar que una factura quedo saldada
 * por un importe que nunca entro a la caja.
 *
 * <p>
 * Lo que congela esta clase y no ve ningun test de servicio: <b>que los diez
 * campos del cuerpo llegan al {@code RegisterDocumentWithholdingCommand} en su
 * posicion, y que la empresa llega por parametro y no por el cuerpo.</b> El
 * command es un {@code record} de diez componentes con tres {@code BigDecimal}
 * seguidos; cruzar {@code taxableBase} con {@code ratePercent} compila sin una
 * queja y produce una retencion del millon doscientos mil por ciento. Por eso
 * el caso feliz captura el command y compara componente a componente con
 * valores todos distintos entre si.
 */
@WebMvcTest(SystemDocumentWithholdingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemDocumentWithholdingController — contrato HTTP de plataforma")
class SystemDocumentWithholdingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RegisterDocumentWithholdingUseCase registerUseCase;
    @MockitoBean
    private LinkWithholdingCertificateUseCase linkUseCase;
    @MockitoBean
    private ListAllDocumentWithholdingsUseCase listAllUseCase;
    @MockitoBean
    private ListUncertifiedDocumentWithholdingsUseCase listUncertifiedUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("responde 201 y traslada los diez campos del cuerpo al command sin cruzarlos")
        void responde_201_y_traslada_los_diez_campos_sin_cruzarlos() throws Exception {
            when(registerUseCase.execute(any()))
                    .thenReturn(DocumentWithholdingMother.dto(41L, null));

            mockMvc.perform(post("/system/document-withholdings").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billingDocumentId": 8400,
                              "type": "ICA",
                              "taxableBase": 1234567.89,
                              "ratePercent": 0.690000,
                              "amount": 8518.52,
                              "municipalityCode": "05001",
                              "fiscalYear": 2026,
                              "fiscalPeriodKey": "2026-B02",
                              "practicedOn": "2026-03-05"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(41))
                    .andExpect(jsonPath("$.type").value("ICA"))
                    .andExpect(jsonPath("$.amount").value(8518.52));

            ArgumentCaptor<RegisterDocumentWithholdingCommand> command = ArgumentCaptor
                    .forClass(RegisterDocumentWithholdingCommand.class);
            verify(registerUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                // La empresa llega del @RequestParam, que es la unica forma que
                // EMPRESA_NO_VIAJA_EN_EL_CUERPO permite en una ruta de plataforma.
                assertThat(cmd.companyId()).isEqualTo(900L);
                assertThat(cmd.billingDocumentId()).isEqualTo(8400L);
                assertThat(cmd.type()).isEqualTo(WithholdingType.ICA);
                assertThat(cmd.taxableBase()).isEqualByComparingTo("1234567.89");
                // 6,9 por mil son 0,69 %. Si el binding perdiera decimales, o alguien
                // cruzara esta linea con la de arriba, la retencion cambiaria de orden
                // de magnitud sin una sola queja del compilador.
                assertThat(cmd.ratePercent()).isEqualByComparingTo("0.690000");
                assertThat(cmd.amount()).isEqualByComparingTo("8518.52");
                assertThat(cmd.municipalityCode()).isEqualTo("05001");
                assertThat(cmd.fiscalYear()).isEqualTo(2026);
                assertThat(cmd.fiscalPeriodKey()).isEqualTo("2026-B02");
                assertThat(cmd.practicedOn()).isEqualTo(LocalDate.of(2026, 3, 5));
            });
        }

        @Test
        @DisplayName("apuntar al certificado lleva la retencion de la ruta y la empresa del parametro")
        void apuntar_al_certificado_lleva_la_ruta_y_el_parametro() throws Exception {
            when(linkUseCase.execute(any())).thenReturn(DocumentWithholdingMother.dto(41L, 8410L));

            mockMvc.perform(post("/system/document-withholdings/{id}/certificate", 41L)
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {"certificateId": 8410}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.certificateId").value(8410));

            ArgumentCaptor<LinkWithholdingCertificateCommand> command = ArgumentCaptor
                    .forClass(LinkWithholdingCertificateCommand.class);
            verify(linkUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                // Los tres son Long y van seguidos: cruzar el id de la retencion con el
                // del certificado certificaria la fila equivocada.
                assertThat(cmd.id()).isEqualTo(41L);
                assertThat(cmd.companyId()).isEqualTo(900L);
                assertThat(cmd.certificateId()).isEqualTo(8410L);
            });
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un cuerpo sin factura ni tipo sale 400 con los dos campos nombrados")
        void un_cuerpo_sin_factura_ni_tipo_sale_400() throws Exception {
            // El @Valid del @RequestBody es lo unico que dispara el validador; sin el,
            // los @NotNull del DTO estan escritos y no se evaluan nunca. Este caso se
            // pone rojo el dia que alguien lo quite.
            mockMvc.perform(post("/system/document-withholdings").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "taxableBase": 1234567.89,
                              "ratePercent": 2.500000,
                              "amount": 30864.20,
                              "fiscalYear": 2026,
                              "fiscalPeriodKey": "2026-A",
                              "practicedOn": "2026-03-05"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field",
                            org.hamcrest.Matchers.containsInAnyOrder("billingDocumentId", "type")));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("una tarifa mayor que 100 sale 400 y no llega al caso de uso")
        void una_tarifa_mayor_que_cien_sale_400() throws Exception {
            // 690 seria lo que escribiria quien confunda el por mil con una fraccion al
            // reves. No es una tarifa de nada, y la mitad del valor del caso es que la
            // peticion invalida NO escribe.
            mockMvc.perform(post("/system/document-withholdings").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billingDocumentId": 8400,
                              "type": "INCOME_TAX",
                              "taxableBase": 1234567.89,
                              "ratePercent": 690.000000,
                              "amount": 30864.20,
                              "fiscalYear": 2026,
                              "fiscalPeriodKey": "2026-A",
                              "practicedOn": "2026-03-05"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("ratePercent"));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("un periodo fiscal con forma imposible sale 400 antes de tocar el dominio")
        void un_periodo_fiscal_con_forma_imposible_sale_400() throws Exception {
            // 'B07' no existe: el ano fiscal tiene seis bimestres. Que ademas
            // corresponda al tipo y al ano lo decide el dominio; esta capa solo rechaza
            // lo que no es un periodo en ninguna lectura.
            mockMvc.perform(post("/system/document-withholdings").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billingDocumentId": 8400,
                              "type": "VAT",
                              "taxableBase": 1234567.89,
                              "ratePercent": 15.000000,
                              "amount": 185185.18,
                              "fiscalYear": 2026,
                              "fiscalPeriodKey": "2026-B07",
                              "practicedOn": "2026-03-05"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("fiscalPeriodKey"));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("un ano gravable de dos digitos sale 400 y no llega al caso de uso")
        void un_ano_gravable_de_dos_digitos_sale_400() throws Exception {
            mockMvc.perform(post("/system/document-withholdings").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billingDocumentId": 8400,
                              "type": "INCOME_TAX",
                              "taxableBase": 1234567.89,
                              "ratePercent": 2.500000,
                              "amount": 30864.20,
                              "fiscalYear": 26,
                              "fiscalPeriodKey": "2026-A",
                              "practicedOn": "2026-03-05"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("fiscalYear"));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("apuntar al certificado sin decir a cual sale 400")
        void apuntar_al_certificado_sin_decir_a_cual_sale_400() throws Exception {
            mockMvc.perform(post("/system/document-withholdings/{id}/certificate", 41L)
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON)
                    .content("{}")).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("certificateId"));

            verifyNoInteractions(linkUseCase);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("sin companyId el barrido recorre todas las empresas y lo dice pasando null")
        void sin_company_id_el_barrido_recorre_todas_las_empresas() throws Exception {
            when(listAllUseCase.listAll(any(), anyInt(), anyInt())).thenReturn(
                    PageResult.of(List.of(DocumentWithholdingMother.dto(41L, null)), 0, 20, 1L));

            mockMvc.perform(get("/system/document-withholdings")).andExpect(status().isOk())
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

            mockMvc.perform(get("/system/document-withholdings").param("companyId", "901")
                    .param("page", "3").param("pageSize", "7")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page").value(3));

            verify(listAllUseCase).listAll(901L, 3, 7);
        }

        @Test
        @DisplayName("la vigilancia de plataforma NO acepta companyId ni siquiera colado")
        void la_vigilancia_de_plataforma_no_acepta_company_id() throws Exception {
            when(listUncertifiedUseCase.listUncertified(anyInt(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/system/document-withholdings/uncertified")
                    .param("fiscalYear", "2026").param("companyId", "901"))
                    .andExpect(status().isOk());

            // La firma del puerto no tiene donde recibirlo: la version acotada por
            // empresa es otro caso de uso y vive en el controller de tenant, donde la
            // empresa la pone el token y no el que pregunta.
            verify(listUncertifiedUseCase).listUncertified(2026, 0, 20);
        }

        @Test
        @DisplayName("la vigilancia de plataforma exige el ano y sin el sale 400")
        void la_vigilancia_de_plataforma_exige_el_ano() throws Exception {
            mockMvc.perform(get("/system/document-withholdings/uncertified"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(listUncertifiedUseCase);
        }
    }
}
