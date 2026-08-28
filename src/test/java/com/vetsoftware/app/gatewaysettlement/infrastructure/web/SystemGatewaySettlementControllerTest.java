package com.vetsoftware.app.gatewaysettlement.infrastructure.web;

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
import com.vetsoftware.app.gatewaysettlement.application.command.AttachProviderInvoiceCommand;
import com.vetsoftware.app.gatewaysettlement.application.command.LinkBankReceiptCommand;
import com.vetsoftware.app.gatewaysettlement.application.command.RegisterGatewaySettlementCommand;
import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementReconciliationDto;
import com.vetsoftware.app.gatewaysettlement.application.port.in.AttachProviderInvoiceUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.in.FindGatewaySettlementUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.in.LinkBankReceiptUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.in.ListGatewaySettlementsUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.in.ReconcileGatewaySettlementUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.in.RegisterGatewaySettlementUseCase;
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
 * Rodaja web de las liquidaciones. <b>Es la unica de la feature porque no hay
 * controller de tenant</b>: una fila agrupa los cobros de sesenta clinicas.
 *
 * <p>
 * Tres cosas congela esta clase y no las ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Que los cinco importes no se cruzan al armar el command.</b> Es el
 * defecto mas probable de esta rodaja y el mas silencioso: intercambiar
 * {@code feeTaxAmount} con {@code gmfAmount} no rompe nada —la identidad del
 * neto sigue cuadrando, porque es una suma— y deja el gravamen contabilizado
 * como impuesto de la comision para siempre. Por eso el fixture usa cinco
 * importes distintos y el caso los comprueba uno a uno.</li>
 * <li><b>Que el cuerpo SI rechaza importes negativos</b>, al reves que el del
 * extracto bancario de al lado: alli el {@code CHECK} es {@code amount <> 0} y
 * un cargo entra con signo; aqui es {@code gross > 0 AND net > 0}. Quien copie
 * el request del vecino se llevara el {@code @Positive} por delante.</li>
 * <li><b>Que la conciliacion devuelve numeros y no la lista de cobros.</b> Ese
 * array seria la fuga: que empresas cobraron y cuanto, en una respuesta que no
 * acota por empresa.</li>
 * </ul>
 */
@WebMvcTest(SystemGatewaySettlementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemGatewaySettlementController — contrato HTTP de plataforma")
class SystemGatewaySettlementControllerTest {

    private static final String RUTA = "/system/gateway-settlements";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RegisterGatewaySettlementUseCase registerUseCase;
    @MockitoBean
    private AttachProviderInvoiceUseCase attachProviderInvoiceUseCase;
    @MockitoBean
    private LinkBankReceiptUseCase linkBankReceiptUseCase;
    @MockitoBean
    private FindGatewaySettlementUseCase findUseCase;
    @MockitoBean
    private ListGatewaySettlementsUseCase listUseCase;
    @MockitoBean
    private ReconcileGatewaySettlementUseCase reconcileUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Carga de la liquidacion")
    class CargaDeLaLiquidacion {

        @Test
        @DisplayName("responde 201 y traslada los cinco importes al command SIN cruzarlos")
        void responde_201_y_traslada_los_cinco_importes_sin_cruzarlos() throws Exception {
            when(registerUseCase.execute(any())).thenReturn(unLote());

            mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                    {
                      "gateway": "WOMPI",
                      "settlementReference": "LOTE-2026-03-0042",
                      "grossAmount": 12450800.00,
                      "feeAmount": 373524.00,
                      "feeTaxAmount": 70969.56,
                      "gmfAmount": 46423.10,
                      "netAmount": 11959883.34,
                      "paymentCount": 37,
                      "settledOn": "2026-03-12"
                    }
                    """)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(8750))
                    .andExpect(jsonPath("$.feeTaxAmount").value(70969.56))
                    .andExpect(jsonPath("$.gmfAmount").value(46423.10))
                    .andExpect(jsonPath("$.totalCost").value(490916.66))
                    .andExpect(jsonPath("$.providerInvoiceRef").doesNotExist())
                    .andExpect(jsonPath("$.bankReceiptId").doesNotExist());

            ArgumentCaptor<RegisterGatewaySettlementCommand> command = ArgumentCaptor
                    .forClass(RegisterGatewaySettlementCommand.class);
            verify(registerUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.gateway()).isEqualTo("WOMPI");
                assertThat(cmd.settlementReference()).isEqualTo("LOTE-2026-03-0042");
                assertThat(cmd.grossAmount()).isEqualByComparingTo("12450800.00");
                assertThat(cmd.feeAmount()).isEqualByComparingTo("373524.00");
                assertThat(cmd.feeTaxAmount()).isEqualByComparingTo("70969.56");
                assertThat(cmd.gmfAmount()).isEqualByComparingTo("46423.10");
                assertThat(cmd.netAmount()).isEqualByComparingTo("11959883.34");
                assertThat(cmd.paymentCount()).isEqualTo(37);
                assertThat(cmd.settledOn()).isEqualTo(LocalDate.of(2026, 3, 12));
            });
        }

        @Test
        @DisplayName("un bruto negativo lo para el binder: aqui el CHECK es gross > 0")
        void un_bruto_negativo_lo_para_el_binder() throws Exception {
            mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                    {
                      "gateway": "WOMPI",
                      "settlementReference": "LOTE-NEGATIVO",
                      "grossAmount": -12450800.00,
                      "feeAmount": 373524.00,
                      "feeTaxAmount": 70969.56,
                      "gmfAmount": 46423.10,
                      "netAmount": 11959883.34,
                      "paymentCount": 37,
                      "settledOn": "2026-03-12"
                    }
                    """)).andExpect(status().isBadRequest());

            verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("un lote que declara cero cobros lo para el binder")
        void un_lote_que_declara_cero_cobros_lo_para_el_binder() throws Exception {
            // Sin cobros declarados no hay nada que contrastar despues, que es para lo
            // que la columna existe.
            mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                    {
                      "gateway": "WOMPI",
                      "settlementReference": "LOTE-VACIO",
                      "grossAmount": 12450800.00,
                      "feeAmount": 373524.00,
                      "feeTaxAmount": 70969.56,
                      "gmfAmount": 46423.10,
                      "netAmount": 11959883.34,
                      "paymentCount": 0,
                      "settledOn": "2026-03-12"
                    }
                    """)).andExpect(status().isBadRequest());

            verifyNoInteractions(registerUseCase);
        }
    }

    @Nested
    @DisplayName("Lo que se sabe despues")
    class LoQueSeSabeDespues {

        @Test
        @DisplayName("la factura del proveedor viaja con su NIT al command")
        void la_factura_del_proveedor_viaja_con_su_nit() throws Exception {
            when(attachProviderInvoiceUseCase.execute(any())).thenReturn(unLoteConSoporte());

            mockMvc.perform(patch(RUTA + "/8750/provider-invoice")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "providerInvoiceRef": "FE-WOMPI-88213",
                              "providerTaxId": "900123456-7"
                            }
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.providerInvoiceRef").value("FE-WOMPI-88213"))
                    .andExpect(jsonPath("$.providerTaxId").value("900123456-7"));

            ArgumentCaptor<AttachProviderInvoiceCommand> command = ArgumentCaptor
                    .forClass(AttachProviderInvoiceCommand.class);
            verify(attachProviderInvoiceUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.id()).isEqualTo(8750L);
                assertThat(cmd.providerInvoiceRef()).isEqualTo("FE-WOMPI-88213");
                assertThat(cmd.providerTaxId()).isEqualTo("900123456-7");
            });
        }

        @Test
        @DisplayName("media factura no entra: sin NIT lo para el binder")
        void media_factura_no_entra() throws Exception {
            // Sin el NIT no se puede armar el reporte anual de terceros, y el CHECK del
            // esquema es un bicondicional: o los dos o ninguno.
            mockMvc.perform(patch(RUTA + "/8750/provider-invoice")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            { "providerInvoiceRef": "FE-WOMPI-88213" }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(attachProviderInvoiceUseCase);
        }

        @Test
        @DisplayName("atar la entrada del extracto traslada el id al command")
        void atar_la_entrada_del_extracto_traslada_el_id() throws Exception {
            when(linkBankReceiptUseCase.execute(any())).thenReturn(unLoteConciliado());

            mockMvc.perform(patch(RUTA + "/8750/bank-receipt")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            { "bankReceiptId": 8700 }
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.bankReceiptId").value(8700));

            ArgumentCaptor<LinkBankReceiptCommand> command = ArgumentCaptor
                    .forClass(LinkBankReceiptCommand.class);
            verify(linkBankReceiptUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(8750L);
            assertThat(command.getValue().bankReceiptId()).isEqualTo(8700L);
        }
    }

    @Nested
    @DisplayName("Lectura y conciliacion")
    class LecturaYConciliacion {

        @Test
        @DisplayName("la conciliacion devuelve NUMEROS, nunca la lista de cobros")
        void la_conciliacion_devuelve_numeros_y_no_la_lista_de_cobros() throws Exception {
            // Enumerar los pagos seria enseñar en una respuesta que empresas cobraron y
            // cuanto: la fuga que esta rodaja existe para evitar. El caso congela la
            // forma, que es donde alguien la anadiria «para que se vea mejor».
            when(reconcileUseCase.reconcile(8750L)).thenReturn(unDescuadre());

            mockMvc.perform(get(RUTA + "/8750/reconciliation")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.declaredPayments").value(37))
                    .andExpect(jsonPath("$.linkedPayments").value(36))
                    .andExpect(jsonPath("$.difference").value(1))
                    .andExpect(jsonPath("$.balanced").value(false))
                    .andExpect(jsonPath("$.payments").doesNotExist())
                    .andExpect(jsonPath("$.content").doesNotExist());

            verifyNoInteractions(findUseCase);
        }

        @Test
        @DisplayName("la ruta de conciliacion no cae en el mapeo por id")
        void la_ruta_de_conciliacion_no_cae_en_el_mapeo_por_id() throws Exception {
            // Si cayera, «reconciliation» llegaria como {id} y saldria un 400 de
            // conversion de tipo en vez del informe.
            when(findUseCase.findById(8750L)).thenReturn(unLote());

            mockMvc.perform(get(RUTA + "/8750")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.settlementReference").value("LOTE-2026-03-0042"));

            verifyNoInteractions(reconcileUseCase);
        }

        @Test
        @DisplayName("el listado conserva los totales de la consulta")
        void el_listado_conserva_los_totales_de_la_consulta() throws Exception {
            when(listUseCase.listAll(anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unLote()), 0, 20, 137L));

            mockMvc.perform(get(RUTA)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(8750))
                    .andExpect(jsonPath("$.totalElements").value(137));

            verify(listUseCase).listAll(0, 20);
        }
    }

    private static GatewaySettlementDto unLote() {
        return new GatewaySettlementDto(8750L, "WOMPI", "LOTE-2026-03-0042", null, null,
                new BigDecimal("12450800.00"), new BigDecimal("373524.00"),
                new BigDecimal("70969.56"), new BigDecimal("46423.10"),
                new BigDecimal("11959883.34"), new BigDecimal("490916.66"), 37,
                LocalDate.of(2026, 3, 12), null, LocalDateTime.of(2026, 3, 14, 9, 30, 15));
    }

    private static GatewaySettlementDto unLoteConSoporte() {
        return new GatewaySettlementDto(8750L, "WOMPI", "LOTE-2026-03-0042", "FE-WOMPI-88213",
                "900123456-7", new BigDecimal("12450800.00"), new BigDecimal("373524.00"),
                new BigDecimal("70969.56"), new BigDecimal("46423.10"),
                new BigDecimal("11959883.34"), new BigDecimal("490916.66"), 37,
                LocalDate.of(2026, 3, 12), null, LocalDateTime.of(2026, 3, 14, 9, 30, 15));
    }

    private static GatewaySettlementDto unLoteConciliado() {
        return new GatewaySettlementDto(8750L, "WOMPI", "LOTE-2026-03-0042", "FE-WOMPI-88213",
                "900123456-7", new BigDecimal("12450800.00"), new BigDecimal("373524.00"),
                new BigDecimal("70969.56"), new BigDecimal("46423.10"),
                new BigDecimal("11959883.34"), new BigDecimal("490916.66"), 37,
                LocalDate.of(2026, 3, 12), 8700L, LocalDateTime.of(2026, 3, 14, 9, 30, 15));
    }

    private static GatewaySettlementReconciliationDto unDescuadre() {
        return new GatewaySettlementReconciliationDto(8750L, "WOMPI", "LOTE-2026-03-0042", 37, 36L,
                1L, false);
    }
}
