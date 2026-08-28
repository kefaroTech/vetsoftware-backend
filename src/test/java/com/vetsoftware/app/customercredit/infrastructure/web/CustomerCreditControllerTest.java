package com.vetsoftware.app.customercredit.infrastructure.web;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditBalanceDto;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.application.port.in.FindCustomerCreditBalanceUseCase;
import com.vetsoftware.app.customercredit.application.port.in.FindCustomerCreditEntryUseCase;
import com.vetsoftware.app.customercredit.application.port.in.ListCustomerCreditEntriesUseCase;
import com.vetsoftware.app.customercredit.domain.CreditEntryKind;
import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import com.vetsoftware.app.customercredit.domain.CustomerCreditBalanceNotFoundException;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntryNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja web del camino de lectura del tenant sobre su saldo a favor.
 *
 * <p>
 * <b>Ninguno de los tres endpoints acepta una empresa por parametro</b>, y eso
 * es el contrato: el saldo que ve una clinica es el suyo y la empresa la pone
 * el token. Si alguien añadiera un {@code @RequestParam Long companyId} «para
 * que la consola reutilice la ruta», los {@code verify} con el valor exacto de
 * {@code authz.currentCompanyId()} se ponen rojos. La consola tiene sus propias
 * rutas bajo {@code /system}, cerradas a {@code hasRole('SYSTEM')}.
 */
@WebMvcTest(CustomerCreditController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CustomerCreditController — contrato HTTP del tenant")
class CustomerCreditControllerTest {

    private static final Long EMPRESA_DEL_TOKEN = 77L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FindCustomerCreditEntryUseCase findEntryUseCase;
    @MockitoBean
    private ListCustomerCreditEntriesUseCase listEntriesUseCase;
    @MockitoBean
    private FindCustomerCreditBalanceUseCase findBalanceUseCase;
    @MockitoBean
    private Authz authz;

    @BeforeEach
    void empresaDelContexto() {
        when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_TOKEN);
    }

    @Nested
    @DisplayName("Saldo")
    class Saldo {

        @Test
        @DisplayName("devuelve el saldo de la empresa del token con su proxima caducidad")
        void devuelve_el_saldo_con_su_proxima_caducidad() throws Exception {
            when(findBalanceUseCase.findByCompanyId(EMPRESA_DEL_TOKEN))
                    .thenReturn(new CustomerCreditBalanceDto(5L, EMPRESA_DEL_TOKEN,
                            new BigDecimal("340500.25"), LocalDate.of(2026, 6, 30),
                            LocalDateTime.of(2026, 3, 15, 17, 42, 9), 12L));

            mockMvc.perform(get("/customer-credit/balance")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.companyId").value(77))
                    .andExpect(jsonPath("$.balanceAmount").value(340500.25))
                    .andExpect(jsonPath("$.nextExpiryOn").value("2026-06-30"))
                    .andExpect(jsonPath("$.recalculatedAt").value("2026-03-15T17:42:09"))
                    .andExpect(jsonPath("$.version").value(12));
        }

        @Test
        @DisplayName("una empresa sin cuenta de saldo abierta sale 404")
        void una_empresa_sin_cuenta_abierta_sale_404() throws Exception {
            when(findBalanceUseCase.findByCompanyId(EMPRESA_DEL_TOKEN))
                    .thenThrow(new CustomerCreditBalanceNotFoundException(EMPRESA_DEL_TOKEN));

            mockMvc.perform(get("/customer-credit/balance")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CUSTOMER_CREDIT_BALANCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("ignora la empresa que venga en la query y usa la del token")
        void ignora_la_empresa_que_venga_en_la_query() throws Exception {
            when(findBalanceUseCase.findByCompanyId(anyLong())).thenReturn(unSaldo());

            mockMvc.perform(get("/customer-credit/balance").param("companyId", "999"))
                    .andExpect(status().isOk());

            verify(findBalanceUseCase).findByCompanyId(EMPRESA_DEL_TOKEN);
        }
    }

    @Nested
    @DisplayName("Asientos")
    class Asientos {

        @Test
        @DisplayName("el asiento trae su clase, su signo y el lote del que sale")
        void el_asiento_trae_su_clase_su_signo_y_su_lote() throws Exception {
            when(findEntryUseCase.findById(31L, EMPRESA_DEL_TOKEN)).thenReturn(unConsumo());

            mockMvc.perform(get("/customer-credit/entries/{id}", 31L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(31))
                    .andExpect(jsonPath("$.entryKind").value("CONSUMPTION"))
                    // El consumo va en negativo: el remanente de un lote se calcula
                    // sumando. Un valor positivo aqui invertiria el saldo del cliente.
                    .andExpect(jsonPath("$.amount").value(-40000.00))
                    .andExpect(jsonPath("$.lotEntryId").value(30))
                    .andExpect(jsonPath("$.originKind").value("APPLICATION"))
                    .andExpect(jsonPath("$.originDocumentId").value(8300))
                    .andExpect(jsonPath("$.occurredAt").value("2026-02-14T11:22:33"))
                    .andExpect(jsonPath("$.valueDate").value("2026-02-20"));
        }

        @Test
        @DisplayName("no expone la llave de idempotencia del asiento")
        void no_expone_la_llave_de_idempotencia() throws Exception {
            when(findEntryUseCase.findById(31L, EMPRESA_DEL_TOKEN)).thenReturn(unConsumo());

            // La llave lleva el separador de operacion y el indice del lote: publicarla
            // enseñaria en cuantos trozos se partio un cobro y permitiria colisionar
            // con la siguiente. El dia que alguien la añada al DTO, esto se pone rojo.
            mockMvc.perform(get("/customer-credit/entries/{id}", 31L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientRequestId").doesNotExist());
        }

        @Test
        @DisplayName("un asiento inexistente sale 404 y no llega a listar nada")
        void un_asiento_inexistente_sale_404() throws Exception {
            when(findEntryUseCase.findById(404L, EMPRESA_DEL_TOKEN))
                    .thenThrow(new CustomerCreditEntryNotFoundException(404L));

            mockMvc.perform(get("/customer-credit/entries/{id}", 404L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CUSTOMER_CREDIT_ENTRY_NOT_FOUND"))
                    .andExpect(jsonPath("$.detail").value("CustomerCreditEntry not found: 404"));

            verifyNoInteractions(listEntriesUseCase);
        }

        @Test
        @DisplayName("el listado devuelve la pagina de la empresa del token con sus totales")
        void el_listado_devuelve_la_pagina_de_la_empresa_del_token() throws Exception {
            when(listEntriesUseCase.listByCompany(EMPRESA_DEL_TOKEN, 1, 4))
                    .thenReturn(PageResult.of(List.of(unConsumo()), 1, 4, 9L));

            mockMvc.perform(
                    get("/customer-credit/entries").param("page", "1").param("pageSize", "4"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].entryKind").value("CONSUMPTION"))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(4))
                    .andExpect(jsonPath("$.totalElements").value(9))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("sin parametros pagina desde la primera y de veinte en veinte")
        void sin_parametros_pagina_desde_la_primera() throws Exception {
            when(listEntriesUseCase.listByCompany(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/customer-credit/entries")).andExpect(status().isOk());

            verify(listEntriesUseCase).listByCompany(EMPRESA_DEL_TOKEN, 0, 20);
        }
    }

    private static CustomerCreditBalanceDto unSaldo() {
        return new CustomerCreditBalanceDto(5L, EMPRESA_DEL_TOKEN, new BigDecimal("340500.25"),
                LocalDate.of(2026, 6, 30), LocalDateTime.of(2026, 3, 15, 17, 42, 9), 12L);
    }

    private static CustomerCreditEntryDto unConsumo() {
        return new CustomerCreditEntryDto(31L, EMPRESA_DEL_TOKEN, CreditEntryKind.CONSUMPTION,
                new BigDecimal("-40000.00"), 30L, CreditOriginKind.APPLICATION, null, 8300L, null,
                LocalDateTime.of(2026, 2, 14, 11, 22, 33), LocalDate.of(2026, 2, 20), null,
                LocalDateTime.of(2026, 2, 15, 6, 5, 4));
    }
}
