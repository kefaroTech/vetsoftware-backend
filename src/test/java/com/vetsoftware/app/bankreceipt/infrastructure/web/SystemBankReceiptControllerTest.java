package com.vetsoftware.app.bankreceipt.infrastructure.web;

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
import com.vetsoftware.app.bankreceipt.application.command.DiscardBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.command.IdentifyBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.command.RegisterBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.in.DiscardBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.application.port.in.FindBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.application.port.in.IdentifyBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.application.port.in.ListBankReceiptsUseCase;
import com.vetsoftware.app.bankreceipt.application.port.in.ListUnidentifiedBankReceiptsUseCase;
import com.vetsoftware.app.bankreceipt.application.port.in.RegisterBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
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
 * Rodaja web del extracto bancario. <b>Es la unica de la feature porque no hay
 * controller de tenant</b>: el extracto es el cuadre interno de VetSoftware y
 * una entrada sin identificar no tiene todavia dueño.
 *
 * <p>
 * Dos cosas congela esta clase y no las ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Que el cuerpo NO rechaza un importe negativo.</b> El {@code @Positive}
 * que llevan las demas peticiones de dinero del proyecto seria aqui un defecto:
 * un cargo del banco entra en el extracto con signo, y quien copie el request
 * de al lado lo añadira por reflejo. El caso lo caza en el binder, que es donde
 * el defecto viviria.</li>
 * <li><b>Que {@code /unidentified} no cae en el mapeo de {@code /{id}}.</b> Son
 * dos rutas que compiten y la resolucion la decide Spring, no el orden en que
 * estan escritas: si algun dia cambiara, la bandeja contestaria un 400 de
 * conversion de tipo.</li>
 * </ul>
 */
@WebMvcTest(SystemBankReceiptController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemBankReceiptController — contrato HTTP de plataforma")
class SystemBankReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RegisterBankReceiptUseCase registerUseCase;
    @MockitoBean
    private IdentifyBankReceiptUseCase identifyUseCase;
    @MockitoBean
    private DiscardBankReceiptUseCase discardUseCase;
    @MockitoBean
    private FindBankReceiptUseCase findUseCase;
    @MockitoBean
    private ListBankReceiptsUseCase listUseCase;
    @MockitoBean
    private ListUnidentifiedBankReceiptsUseCase listUnidentifiedUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Carga del extracto")
    class CargaDelExtracto {

        @Test
        @DisplayName("responde 201 y traslada los cinco campos del cuerpo al command sin cruzarlos")
        void responde_201_y_traslada_los_cinco_campos_sin_cruzarlos() throws Exception {
            when(registerUseCase.execute(any())).thenReturn(unaEntrada());

            mockMvc.perform(post("/system/bank-receipts").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "bankAccountRef": "BANCOLOMBIA-AHORROS-00912",
                              "bankReference": "TRX-2026-03-0099A",
                              "receivedOn": "2026-03-05",
                              "amount": 217345.61,
                              "description": "Consignacion Clinica San Roque"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(8700))
                    .andExpect(jsonPath("$.amount").value(217345.61))
                    .andExpect(jsonPath("$.status").value("UNIDENTIFIED"))
                    .andExpect(jsonPath("$.identifiedAt").doesNotExist());

            ArgumentCaptor<RegisterBankReceiptCommand> command = ArgumentCaptor
                    .forClass(RegisterBankReceiptCommand.class);
            verify(registerUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.bankAccountRef()).isEqualTo("BANCOLOMBIA-AHORROS-00912");
                assertThat(cmd.bankReference()).isEqualTo("TRX-2026-03-0099A");
                assertThat(cmd.receivedOn()).isEqualTo(LocalDate.of(2026, 3, 5));
                assertThat(cmd.amount()).isEqualByComparingTo("217345.61");
                assertThat(cmd.description()).isEqualTo("Consignacion Clinica San Roque");
            });
        }

        @Test
        @DisplayName("un importe NEGATIVO pasa el binder y llega al caso de uso con su signo")
        void un_importe_negativo_pasa_el_binder() throws Exception {
            // El CHECK del esquema es `amount <> 0`. Un @Positive aqui rechazaria en el
            // binder la mitad de un extracto real —cargos, comisiones, devoluciones de
            // cheque— con un mensaje de campo invalido que el operario no puede
            // corregir, porque el fichero del banco dice eso.
            when(registerUseCase.execute(any())).thenReturn(unaEntrada());

            mockMvc.perform(post("/system/bank-receipts").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "bankAccountRef": "BANCOLOMBIA-AHORROS-00912",
                              "bankReference": "TRX-CARGO-001",
                              "receivedOn": "2026-03-05",
                              "amount": -45000.00
                            }
                            """)).andExpect(status().isCreated());

            ArgumentCaptor<RegisterBankReceiptCommand> command = ArgumentCaptor
                    .forClass(RegisterBankReceiptCommand.class);
            verify(registerUseCase).execute(command.capture());
            assertThat(command.getValue().amount()).isEqualByComparingTo("-45000.00");
            assertThat(command.getValue().description()).isNull();
        }

        @Test
        @DisplayName("un cuerpo sin referencia ni fecha sale 400 con los dos campos nombrados")
        void un_cuerpo_sin_referencia_ni_fecha_sale_400() throws Exception {
            // El @Valid del @RequestBody es lo unico que dispara el validador; sin el,
            // el @NotBlank del DTO esta escrito y no se evalua nunca (#135). Este caso
            // se pone rojo el dia que alguien lo quite.
            mockMvc.perform(post("/system/bank-receipts").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "bankAccountRef": "BANCOLOMBIA-AHORROS-00912",
                              "bankReference": "   ",
                              "amount": 1000.00
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field", org.hamcrest.Matchers
                            .containsInAnyOrder("bankReference", "receivedOn")));

            // La mitad del valor del caso: que la peticion invalida NO escribe.
            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("un tercer decimal en el importe sale 400 y no llega al caso de uso")
        void un_tercer_decimal_sale_400() throws Exception {
            // DECIMAL(19,2): sin esto la base redondearia el centavo en silencio.
            mockMvc.perform(post("/system/bank-receipts").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "bankAccountRef": "BANCOLOMBIA-AHORROS-00912",
                              "bankReference": "TRX-DECIMALES",
                              "receivedOn": "2026-03-05",
                              "amount": 100.005
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("amount"));

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("una referencia de mas de 120 caracteres sale 400")
        void una_referencia_demasiado_larga_sale_400() throws Exception {
            mockMvc.perform(post("/system/bank-receipts").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "bankAccountRef": "BANCOLOMBIA-AHORROS-00912",
                              "bankReference": "%s",
                              "receivedOn": "2026-03-05",
                              "amount": 1000.00
                            }
                            """.formatted("R".repeat(121)))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("bankReference"));

            verifyNoInteractions(registerUseCase);
        }
    }

    @Nested
    @DisplayName("Salida de la bandeja")
    class SalidaDeLaBandeja {

        @Test
        @DisplayName("identificar responde 200 con el id de la ruta y sin cuerpo de peticion")
        void identificar_responde_200_con_el_id_de_la_ruta() throws Exception {
            when(identifyUseCase.execute(any())).thenReturn(unaEntradaIdentificada());

            mockMvc.perform(patch("/system/bank-receipts/8700/identify")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IDENTIFIED"))
                    .andExpect(jsonPath("$.identifiedAt").value("2026-03-09T16:20:30"));

            ArgumentCaptor<IdentifyBankReceiptCommand> command = ArgumentCaptor
                    .forClass(IdentifyBankReceiptCommand.class);
            verify(identifyUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(8700L);
        }

        @Test
        @DisplayName("descartar responde 200 y NO es un DELETE: la fila se queda")
        void descartar_responde_200_y_no_es_un_delete() throws Exception {
            when(discardUseCase.execute(any())).thenReturn(unaEntradaDescartada());

            mockMvc.perform(patch("/system/bank-receipts/8701/discard")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DISCARDED"))
                    // El importe sigue en la respuesta: no se ha ocultado nada.
                    .andExpect(jsonPath("$.amount").value(217345.61));

            ArgumentCaptor<DiscardBankReceiptCommand> command = ArgumentCaptor
                    .forClass(DiscardBankReceiptCommand.class);
            verify(discardUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(8701L);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("la lectura por id devuelve la entrada")
        void la_lectura_por_id_devuelve_la_entrada() throws Exception {
            when(findUseCase.findById(8700L)).thenReturn(unaEntrada());

            mockMvc.perform(get("/system/bank-receipts/8700")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.bankReference").value("TRX-2026-03-0099A"))
                    .andExpect(jsonPath("$.receivedOn").value("2026-03-05"));
        }

        @Test
        @DisplayName("el listado completo pagina con los valores por defecto")
        void el_listado_completo_pagina_con_los_valores_por_defecto() throws Exception {
            when(listUseCase.listAll(anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unaEntrada()), 0, 20, 137L));

            mockMvc.perform(get("/system/bank-receipts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(8700))
                    .andExpect(jsonPath("$.totalElements").value(137));

            verify(listUseCase).listAll(0, 20);
        }

        @Test
        @DisplayName("la bandeja tiene su propia ruta y no cae en el mapeo por id")
        void la_bandeja_tiene_su_propia_ruta() throws Exception {
            // Si /unidentified resolviera contra /{id}, Spring intentaria convertir
            // "unidentified" a Long y la bandeja contestaria un 400 de conversion.
            when(listUnidentifiedUseCase.listUnidentified(anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unaEntrada()), 2, 5, 48L));

            mockMvc.perform(get("/system/bank-receipts/unidentified").param("page", "2")
                    .param("pageSize", "5")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("UNIDENTIFIED"))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5));

            verify(listUnidentifiedUseCase).listUnidentified(2, 5);
            verifyNoInteractions(findUseCase);
        }
    }

    private static BankReceiptDto unaEntrada() {
        return new BankReceiptDto(8700L, "BANCOLOMBIA-AHORROS-00912", "TRX-2026-03-0099A",
                LocalDate.of(2026, 3, 5), new BigDecimal("217345.61"),
                "Consignacion Clinica San Roque", BankReceiptStatus.UNIDENTIFIED, null,
                LocalDateTime.of(2026, 3, 7, 8, 45, 0));
    }

    private static BankReceiptDto unaEntradaIdentificada() {
        return new BankReceiptDto(8700L, "BANCOLOMBIA-AHORROS-00912", "TRX-2026-03-0099A",
                LocalDate.of(2026, 3, 5), new BigDecimal("217345.61"),
                "Consignacion Clinica San Roque", BankReceiptStatus.IDENTIFIED,
                LocalDateTime.of(2026, 3, 9, 16, 20, 30), LocalDateTime.of(2026, 3, 7, 8, 45, 0));
    }

    private static BankReceiptDto unaEntradaDescartada() {
        return new BankReceiptDto(8701L, "BANCOLOMBIA-AHORROS-00912", "TRX-NO-ES-DE-NADIE",
                LocalDate.of(2026, 3, 5), new BigDecimal("217345.61"), null,
                BankReceiptStatus.DISCARDED, LocalDateTime.of(2026, 3, 9, 16, 20, 30),
                LocalDateTime.of(2026, 3, 7, 8, 45, 0));
    }
}
