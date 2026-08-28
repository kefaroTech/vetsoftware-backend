package com.vetsoftware.app.withholdingraterule.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import com.vetsoftware.app.withholdingraterule.application.command.CloseWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.command.CreateWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.port.in.CloseWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.in.CreateWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import com.vetsoftware.app.withholdingraterule.testsupport.WithholdingRateRuleMother;
import java.time.LocalDate;
import org.hamcrest.Matchers;
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
 * plataforma: la tarifa de retencion la fija la ley, no la clinica.
 *
 * <p>
 * Lo que congela esta clase y no ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Los nueve campos del cuerpo llegan al command en su posicion.</b> Dos
 * son enumerados, tres son {@code BigDecimal} y dos son fechas: cruzar
 * {@code minimumBaseAmount} con {@code minimumBaseUvt}, o {@code validFrom} con
 * {@code validTo}, compila sin una queja. Por eso el caso feliz captura el
 * command y compara componente a componente con valores todos distintos.</li>
 * <li><b>La tarifa cruza la frontera HTTP con sus seis decimales.</b> Es el
 * numero que se pierde si alguien lo declara {@code double} o le pone un
 * formato: {@code 0.690000} redondeado a {@code 0.69} sobrevive, pero
 * {@code 0.414000} se corta a {@code 0.41} y retiene casi un uno por ciento de
 * menos por factura.</li>
 * </ul>
 *
 * <p>
 * <b>Lo que esta clase NO cubre todavia:</b> el 404 de
 * {@code WithholdingRateRuleNotFoundException} y el 409 de
 * {@code WithholdingRateRuleAlreadyClosedException}.
 * {@code GlobalExceptionHandler} aun no las enumera —son excepciones nuevas— y
 * hoy saldrian como 500. Cuando se cableen, aqui van sus dos casos.
 */
@WebMvcTest(SystemWithholdingRateRuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemWithholdingRateRuleController — contrato HTTP de plataforma")
class SystemWithholdingRateRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateWithholdingRateRuleUseCase createUseCase;
    @MockitoBean
    private CloseWithholdingRateRuleUseCase closeUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("responde 201 y traslada los nueve campos del cuerpo al command sin cruzarlos")
        void responde_201_y_traslada_los_nueve_campos_sin_cruzarlos() throws Exception {
            when(createUseCase.execute(any())).thenReturn(WithholdingRateRuleMother.dtoIca());

            mockMvc.perform(post("/system/withholding-rate-rules")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "withholdingType": "ICA",
                              "serviceNature": "CONSULTING",
                              "municipalityCode": "11001",
                              "ratePercent": 0.690000,
                              "minimumBaseAmount": 213010.00,
                              "minimumBaseUvt": 4.00,
                              "legalReference": "Acuerdo 65 de 2002",
                              "validFrom": "2026-01-01",
                              "validTo": null
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(8302))
                    .andExpect(jsonPath("$.ratePercent").value(0.690000));

            ArgumentCaptor<CreateWithholdingRateRuleCommand> command = ArgumentCaptor
                    .forClass(CreateWithholdingRateRuleCommand.class);
            verify(createUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.withholdingType()).isEqualTo(WithholdingType.ICA);
                assertThat(cmd.serviceNature()).isEqualTo(ServiceNature.CONSULTING);
                assertThat(cmd.municipalityCode()).isEqualTo("11001");
                // El 6,9 por mil entero: si el binder recortara la escala, aqui se
                // ve. Con dos decimales se retendria de menos en cada factura.
                assertThat(cmd.ratePercent()).isEqualByComparingTo("0.690000");
                assertThat(cmd.ratePercent().scale()).isEqualTo(6);
                assertThat(cmd.minimumBaseAmount()).isEqualByComparingTo("213010.00");
                assertThat(cmd.minimumBaseUvt()).isEqualByComparingTo("4.00");
                assertThat(cmd.legalReference()).isEqualTo("Acuerdo 65 de 2002");
                assertThat(cmd.validFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
                assertThat(cmd.validTo()).isNull();
            });
        }

        @Test
        @DisplayName("una retencion nacional entra sin municipio")
        void una_retencion_nacional_entra_sin_municipio() throws Exception {
            when(createUseCase.execute(any())).thenReturn(WithholdingRateRuleMother.dtoNacional());

            mockMvc.perform(post("/system/withholding-rate-rules")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "withholdingType": "INCOME_TAX",
                              "serviceNature": "TECHNICAL_SERVICE",
                              "ratePercent": 11.000000,
                              "minimumBaseUvt": 4.00,
                              "validFrom": "2026-01-01"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.municipalityCode").doesNotExist());

            ArgumentCaptor<CreateWithholdingRateRuleCommand> command = ArgumentCaptor
                    .forClass(CreateWithholdingRateRuleCommand.class);
            verify(createUseCase).execute(command.capture());
            assertThat(command.getValue().municipalityCode()).isNull();
            assertThat(command.getValue().minimumBaseAmount()).isNull();
        }

        @Test
        @DisplayName("un cuerpo sin tipo, sin naturaleza, sin tarifa ni fecha sale 400 nombrandolos")
        void un_cuerpo_sin_los_obligatorios_sale_400_nombrandolos() throws Exception {
            // El @Valid del @RequestBody es lo unico que dispara el validador; sin
            // el, los @NotNull del record estan escritos y no se evaluan nunca
            // (#135). Este caso se pone rojo el dia que alguien lo quite.
            mockMvc.perform(post("/system/withholding-rate-rules")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "minimumBaseUvt": 4.00
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field", Matchers.containsInAnyOrder(
                            "withholdingType", "serviceNature", "ratePercent", "validFrom")));

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una tarifa por encima de 100 sale 400: es un porcentaje, no una fraccion")
        void una_tarifa_por_encima_de_cien_sale_400() throws Exception {
            mockMvc.perform(post("/system/withholding-rate-rules")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "withholdingType": "INCOME_TAX",
                              "serviceNature": "TECHNICAL_SERVICE",
                              "ratePercent": 110.000000,
                              "minimumBaseUvt": 4.00,
                              "validFrom": "2026-01-01"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("ratePercent"));

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("un septimo decimal en la tarifa sale 400 y no se redondea callando")
        void un_septimo_decimal_sale_400() throws Exception {
            // La columna es DECIMAL(9,6): un septimo decimal no lo rechaza el
            // motor, lo REDONDEA, y la tarifa guardada deja de ser la escrita.
            mockMvc.perform(post("/system/withholding-rate-rules")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "withholdingType": "INCOME_TAX",
                              "serviceNature": "TECHNICAL_SERVICE",
                              "ratePercent": 0.1234567,
                              "minimumBaseUvt": 4.00,
                              "validFrom": "2026-01-01"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("ratePercent"));

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una naturaleza de servicio que no existe se rechaza en el binder")
        void una_naturaleza_de_servicio_que_no_existe_se_rechaza() throws Exception {
            // La lista se comparte con catalog_items y es cerrada en los dos
            // sitios: un CONSULTANCY por CONSULTING entra aqui o no entra en
            // ningun lado.
            mockMvc.perform(post("/system/withholding-rate-rules")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "withholdingType": "INCOME_TAX",
                              "serviceNature": "CONSULTANCY",
                              "ratePercent": 11.000000,
                              "minimumBaseUvt": 4.00,
                              "validFrom": "2026-01-01"
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("un municipio inexistente sale 400 y con detalle constante, no un oraculo")
        void un_municipio_inexistente_sale_400_con_detalle_constante() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Municipality not found: 99999"));

            mockMvc.perform(post("/system/withholding-rate-rules")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "withholdingType": "ICA",
                              "serviceNature": "CONSULTING",
                              "municipalityCode": "99999",
                              "ratePercent": 0.690000,
                              "minimumBaseUvt": 4.00,
                              "validFrom": "2026-01-01"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }
    }

    @Nested
    @DisplayName("Cierre de vigencia")
    class CierreDeVigencia {

        @Test
        @DisplayName("PATCH y no DELETE: la fila se queda y solo cambia hasta cuando vale")
        void patch_y_no_delete() throws Exception {
            when(closeUseCase.execute(any())).thenReturn(WithholdingRateRuleMother.dtoIca());

            mockMvc.perform(patch("/system/withholding-rate-rules/{id}/close", 8302L)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "validTo": "2027-01-01"
                            }
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(8302));

            ArgumentCaptor<CloseWithholdingRateRuleCommand> command = ArgumentCaptor
                    .forClass(CloseWithholdingRateRuleCommand.class);
            verify(closeUseCase).execute(command.capture());
            // El id sale de la ruta y la fecha del cuerpo: si alguien invirtiera
            // los dos argumentos del command, el record los aceptaria igual.
            assertThat(command.getValue().id()).isEqualTo(8302L);
            assertThat(command.getValue().validTo()).isEqualTo(LocalDate.of(2027, 1, 1));
        }

        @Test
        @DisplayName("cerrar sin fecha sale 400: cerrar es escribir una fecha, no borrar")
        void cerrar_sin_fecha_sale_400() throws Exception {
            mockMvc.perform(patch("/system/withholding-rate-rules/{id}/close", 8302L)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "validTo": null
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("validTo"));

            verifyNoInteractions(closeUseCase);
        }
    }
}
