package com.vetsoftware.app.taxreturn.infrastructure.web;

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
import com.vetsoftware.app.taxreturn.application.command.CreateTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.command.FileTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.in.AnnulTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.CorrectTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.CreateTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.FileTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.FindTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.ListTaxReturnsUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.UpdateTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.domain.TaxKind;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
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
 * Rodaja web de las declaraciones.
 *
 * <p>
 * Tres cosas que congela esta clase y que no ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Quien presenta sale del principal.</b> {@code filed_by_system_user_id}
 * es la firma de la presentacion; si el {@code FileTaxReturnRequest} declarara
 * ese campo, cualquiera podria firmar a nombre de otro.</li>
 * <li><b>La correccion es un {@code POST} que CREA</b>, no un {@code PATCH} que
 * edita. Una declaracion presentada no se edita: se sucede.</li>
 * <li><b>El barrido de firmeza exige la fecha.</b> Sin ella el endpoint
 * devolveria el archivo entero y dejaria de significar «lo que esta a punto de
 * quedar en firme», que es la consulta de la que sale la ventana de
 * conservacion de soportes.</li>
 * </ul>
 */
@WebMvcTest(SystemTaxReturnController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemTaxReturnController — contrato HTTP de plataforma")
class SystemTaxReturnControllerTest {

    private static final Long SYSTEM_USER_ID = 990L;

    private static final TaxReturnDto RENTA = new TaxReturnDto(8440L, TaxKind.INCOME_TAX, 2026,
            "2026-A", 1, null, null, TaxReturnStatus.DRAFT, null, null, null, null,
            new BigDecimal("4500000.00"), new BigDecimal("3300000.00"),
            new BigDecimal("1200000.00"), BigDecimal.ZERO, null, null,
            LocalDateTime.of(2026, 3, 1, 9, 0));

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateTaxReturnUseCase createUseCase;
    @MockitoBean
    private UpdateTaxReturnUseCase updateUseCase;
    @MockitoBean
    private FileTaxReturnUseCase fileUseCase;
    @MockitoBean
    private CorrectTaxReturnUseCase correctUseCase;
    @MockitoBean
    private AnnulTaxReturnUseCase annulUseCase;
    @MockitoBean
    private FindTaxReturnUseCase findUseCase;
    @MockitoBean
    private ListTaxReturnsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("responde 201 y traslada los nueve campos del cuerpo sin cruzarlos")
        void responde_201_y_traslada_los_nueve_campos_sin_cruzarlos() throws Exception {
            when(createUseCase.execute(any())).thenReturn(RENTA);

            mockMvc.perform(
                    post("/system/tax-returns").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "taxKind": "INCOME_TAX",
                              "fiscalYear": 2026,
                              "fiscalPeriodKey": "2026-A",
                              "totalGenerated": 4500000.00,
                              "totalDeductible": 3300000.00,
                              "balancePayable": 1200000.00,
                              "balanceCredit": 0.00
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sequenceNumber").value(1))
                    .andExpect(jsonPath("$.status").value("DRAFT"));

            ArgumentCaptor<CreateTaxReturnCommand> command = ArgumentCaptor
                    .forClass(CreateTaxReturnCommand.class);
            verify(createUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.taxKind()).isEqualTo(TaxKind.INCOME_TAX);
                assertThat(cmd.fiscalYear()).isEqualTo(2026);
                assertThat(cmd.fiscalPeriodKey()).isEqualTo("2026-A");
                assertThat(cmd.municipalityCode()).isNull();
                assertThat(cmd.vatFrequency()).isNull();
                // Los cuatro importes, sin cruzar: generado/descontable y pagar/favor
                // son dos parejas que compilan intercambiadas.
                assertThat(cmd.totalGenerated()).isEqualByComparingTo("4500000.00");
                assertThat(cmd.totalDeductible()).isEqualByComparingTo("3300000.00");
                assertThat(cmd.balancePayable()).isEqualByComparingTo("1200000.00");
                assertThat(cmd.balanceCredit()).isEqualByComparingTo("0.00");
            });
        }

        @Test
        @DisplayName("un año fuera de 2020..2100 responde 400 y no llega al caso de uso")
        void un_ano_fuera_de_rango_responde_400() throws Exception {
            mockMvc.perform(
                    post("/system/tax-returns").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "taxKind": "INCOME_TAX",
                              "fiscalYear": 1999,
                              "fiscalPeriodKey": "1999-A",
                              "totalGenerated": 0.00,
                              "totalDeductible": 0.00,
                              "balancePayable": 0.00,
                              "balanceCredit": 0.00
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }
    }

    @Nested
    @DisplayName("Presentacion")
    class Presentacion {

        @Test
        @DisplayName("la firma sale del principal y la firmeza llega como dato")
        void la_firma_sale_del_principal_y_la_firmeza_llega_como_dato() throws Exception {
            when(authz.currentSystemUserId()).thenReturn(SYSTEM_USER_ID);
            when(fileUseCase.execute(any())).thenReturn(RENTA);

            mockMvc.perform(patch("/system/tax-returns/8440/file")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "receiptRef": "RAD-0001",
                              "fileRef": "s3://dian/2026-A.pdf",
                              "firmezaUntil": "2029-04-10"
                            }
                            """)).andExpect(status().isOk());

            ArgumentCaptor<FileTaxReturnCommand> command = ArgumentCaptor
                    .forClass(FileTaxReturnCommand.class);
            verify(fileUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.id()).isEqualTo(8440L);
                assertThat(cmd.filedBySystemUserId()).isEqualTo(SYSTEM_USER_ID);
                assertThat(cmd.receiptRef()).isEqualTo("RAD-0001");
                assertThat(cmd.fileRef()).isEqualTo("s3://dian/2026-A.pdf");
                assertThat(cmd.firmezaUntil()).isEqualTo(LocalDate.of(2029, 4, 10));
            });
        }

        @Test
        @DisplayName("presentar sin radicado responde 400: sin el no hay prueba")
        void presentar_sin_radicado_responde_400() throws Exception {
            mockMvc.perform(patch("/system/tax-returns/8440/file")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "receiptRef": "  ",
                              "fileRef": "s3://dian/2026-A.pdf",
                              "firmezaUntil": "2029-04-10"
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(fileUseCase);
        }
    }

    @Nested
    @DisplayName("Correccion y conservacion")
    class CorreccionYConservacion {

        @Test
        @DisplayName("la correccion es un POST que crea, no un PATCH que edita")
        void la_correccion_es_un_post_que_crea() throws Exception {
            when(correctUseCase.execute(any())).thenReturn(RENTA);

            mockMvc.perform(post("/system/tax-returns/8440/corrections")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "totalGenerated": 4600000.00,
                              "totalDeductible": 3300000.00,
                              "balancePayable": 1300000.00,
                              "balanceCredit": 0.00
                            }
                            """)).andExpect(status().isCreated());

            verify(correctUseCase).execute(any());
        }

        @Test
        @DisplayName("el barrido de firmeza exige la fecha limite")
        void el_barrido_de_firmeza_exige_la_fecha_limite() throws Exception {
            when(listUseCase.listBecomingFinalBefore(LocalDate.of(2029, 12, 31), 0, 20))
                    .thenReturn(PageResult.of(List.of(RENTA), 0, 20, 1L));

            mockMvc.perform(get("/system/tax-returns/becoming-final").param("before", "2029-12-31"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

            // Sin la fecha el endpoint devolveria el archivo entero: no es un listado
            // mas, es de donde sale hasta cuando NO se puede purgar el detalle de uso.
            mockMvc.perform(get("/system/tax-returns/becoming-final"))
                    .andExpect(status().isBadRequest());
        }
    }
}
