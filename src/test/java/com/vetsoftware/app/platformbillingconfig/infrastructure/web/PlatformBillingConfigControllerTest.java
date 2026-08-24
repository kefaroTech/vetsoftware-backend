package com.vetsoftware.app.platformbillingconfig.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.platformbillingconfig.application.command.UpdatePlatformBillingConfigCommand;
import com.vetsoftware.app.platformbillingconfig.application.dto.PlatformBillingConfigDto;
import com.vetsoftware.app.platformbillingconfig.application.dto.PriceListSummaryDto;
import com.vetsoftware.app.platformbillingconfig.application.port.in.FindPlatformBillingConfigUseCase;
import com.vetsoftware.app.platformbillingconfig.application.port.in.UpdatePlatformBillingConfigUseCase;
import com.vetsoftware.app.platformbillingconfig.infrastructure.web.request.UpdatePlatformBillingConfigRequest;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
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
 * Rodaja HTTP del recurso singular de configuración. No usa {@code Authz}: la
 * configuración es global de plataforma y los dos casos de uso están cerrados a
 * {@code hasRole('SYSTEM')} en su {@code port/in}.
 */
@WebMvcTest(PlatformBillingConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PlatformBillingConfigController — contrato HTTP")
class PlatformBillingConfigControllerTest {

    private static final PlatformBillingConfigDto CONFIG = new PlatformBillingConfigDto(1L,
            new PriceListSummaryDto(7L, "LISTA-2026-01", "Tarifa 2026"), 5, 14, 1, 5, "SIIGO",
            LocalDateTime.of(2026, 1, 15, 10, 30));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindPlatformBillingConfigUseCase findUseCase;
    @MockitoBean
    private UpdatePlatformBillingConfigUseCase updateUseCase;

    @Nested
    @DisplayName("GET /platform-billing-config")
    class Lectura {

        @Test
        @DisplayName("devuelve las políticas con la tarifa por defecto anidada")
        void devuelve_las_politicas_con_la_tarifa_por_defecto_anidada() throws Exception {
            when(findUseCase.find()).thenReturn(CONFIG);

            mockMvc.perform(get("/platform-billing-config")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.defaultGraceDays").value(5))
                    .andExpect(jsonPath("$.defaultTrialDays").value(14))
                    .andExpect(jsonPath("$.invoiceDayOfMonth").value(1))
                    .andExpect(jsonPath("$.defaultPaymentTermDays").value(5))
                    .andExpect(jsonPath("$.externalBillingProvider").value("SIIGO"))
                    .andExpect(jsonPath("$.defaultPriceList.code").value("LISTA-2026-01"));
        }

        @Test
        @DisplayName("sin tarifa por defecto responde el campo en null")
        void sin_tarifa_por_defecto_responde_el_campo_en_null() throws Exception {
            when(findUseCase.find()).thenReturn(new PlatformBillingConfigDto(1L, null, 5, 14, 1, 5,
                    null, LocalDateTime.of(2026, 1, 15, 10, 30)));

            mockMvc.perform(get("/platform-billing-config")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.defaultPriceList").doesNotExist());
        }
    }

    @Nested
    @DisplayName("PUT /platform-billing-config")
    class Escritura {

        @Test
        @DisplayName("traslada el formulario al command sin inventarse campos")
        void traslada_el_formulario_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(CONFIG);

            mockMvc.perform(put("/platform-billing-config").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"defaultPriceListId":7,"defaultGraceDays":10,"defaultTrialDays":30,
                             "invoiceDayOfMonth":15,"defaultPaymentTermDays":0,
                             "externalBillingProvider":"ALEGRA"}
                            """)).andExpect(status().isOk());

            ArgumentCaptor<UpdatePlatformBillingConfigCommand> enviado = ArgumentCaptor
                    .forClass(UpdatePlatformBillingConfigCommand.class);
            verify(updateUseCase).execute(enviado.capture());
            assertThat(enviado.getValue().defaultPriceListId()).isEqualTo(7L);
            assertThat(enviado.getValue().defaultGraceDays()).isEqualTo(10);
            assertThat(enviado.getValue().defaultTrialDays()).isEqualTo(30);
            assertThat(enviado.getValue().invoiceDayOfMonth()).isEqualTo(15);
            assertThat(enviado.getValue().defaultPaymentTermDays()).isZero();
            assertThat(enviado.getValue().externalBillingProvider()).isEqualTo("ALEGRA");
        }

        @Test
        @DisplayName("acepta plazo de pago cero: es pago inmediato, no un campo sin rellenar")
        void acepta_plazo_de_pago_cero() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(CONFIG);

            mockMvc.perform(put("/platform-billing-config").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"defaultGraceDays":5,"defaultTrialDays":14,"invoiceDayOfMonth":1,
                             "defaultPaymentTermDays":0}
                            """)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("rechaza con 400 un día de emisión de 31 sin llegar al caso de uso")
        void rechaza_con_400_un_dia_de_emision_de_31() throws Exception {
            mockMvc.perform(put("/platform-billing-config").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"defaultGraceDays":5,"defaultTrialDays":14,"invoiceDayOfMonth":31,
                             "defaultPaymentTermDays":5}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("rechaza con 400 unos días de gracia negativos")
        void rechaza_con_400_unos_dias_de_gracia_negativos() throws Exception {
            mockMvc.perform(put("/platform-billing-config").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"defaultGraceDays":-1,"defaultTrialDays":14,"invoiceDayOfMonth":1,
                             "defaultPaymentTermDays":5}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("rechaza con 400 un formulario sin el día de emisión")
        void rechaza_con_400_un_formulario_sin_el_dia_de_emision() throws Exception {
            mockMvc.perform(put("/platform-billing-config").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"defaultGraceDays":5,"defaultTrialDays":14,"defaultPaymentTermDays":5}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Contrato del formulario")
    class ContratoDelFormulario {

        @Test
        @DisplayName("el request no declara companyId: la configuración es global de plataforma")
        void el_request_no_declara_company_id() {
            assertThat(Arrays.stream(UpdatePlatformBillingConfigRequest.class.getRecordComponents())
                    .map(RecordComponent::getName)).doesNotContain("companyId");
        }

        @Test
        @DisplayName("el request no declara ningún interruptor de corte de acceso")
        void el_request_no_declara_ningun_interruptor_de_corte_de_acceso() {
            assertThat(Arrays.stream(UpdatePlatformBillingConfigRequest.class.getRecordComponents())
                    .map(RecordComponent::getName))
                    .noneMatch(name -> name.toLowerCase().contains("suspend")
                            || name.toLowerCase().contains("block")
                            || name.toLowerCase().contains("cutoff"));
        }
    }
}
