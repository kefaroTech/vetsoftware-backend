package com.vetsoftware.app.aiproposal.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationRequest;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;
import com.vetsoftware.app.aiproposal.application.port.out.CatalogHintQueryPort;
import com.vetsoftware.app.aiproposal.application.port.out.SpendGuardPort;
import com.vetsoftware.app.aiproposal.application.port.out.SpendGuardPort.SpendReservation;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.ProspectText;
import com.vetsoftware.app.aiproposal.domain.ReasonRejection;
import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.math.BigDecimal;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El adaptador entero, sin red.
 *
 * <p>
 * <b>Que se pueda probar asi no es casualidad: es el objetivo de la costura
 * {@link ModelInvoker}.</b> El acceso al modelo en Bedrock no esta habilitado
 * —un formulario manual sin completar— y aun asi todo lo que decide dinero y
 * seguridad (las cuatro puertas, el tope de gasto, la reconciliacion, la
 * validacion, el saneador) queda fijado aqui.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BedrockProposalGenerator — las cuatro puertas y lo que pasa detras")
class BedrockProposalGeneratorTest {

    private static final Map<String, String> HINTS = Map.of("CORE",
            "El nucleo: clientes y mascotas. Va siempre y NUNCA se lista con motivo.",
            "CLINICAL_HISTORY", "Para quien atiende medicamente: consultas y evolucion.");

    @Mock
    private ModelInvoker invoker;

    @Mock
    private CatalogHintQueryPort hintQueryPort;

    @Mock
    private SpendGuardPort spendGuard;

    private BedrockProposalGenerator generator;

    private ProposalGenerationRequest peticion;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-08-30T10:00:00Z"), ZoneOffset.UTC);
        generator = new BedrockProposalGenerator(invoker, new ProposalPromptBuilder(),
                hintQueryPort, spendGuard, reloj, ObservationRegistry.create());
        peticion = new ProposalGenerationRequest(
                List.of(ProspectText.of("Clinica de barrio, consulta general y vacunas")),
                List.of(), SellableCatalogMother.completo());
    }

    private static ModelInvoker.ModelInvocation respuesta(String json) {
        return new ModelInvoker.ModelInvocation("claude-sonnet", json, 3200, 900, "end_turn");
    }

    @Nested
    @DisplayName("Las cuatro puertas, en orden")
    class Puertas {

        @Test
        @DisplayName("sin hints degrada y NO llega ni a preguntar por el modelo")
        void sin_hints_degrada() {
            when(hintQueryPort.findCurrentHints()).thenReturn(Map.of());

            ProposalGenerationResult resultado = generator.generate(peticion);

            assertThat(resultado.outcome()).isEqualTo(GenerationOutcome.DEGRADED_NO_HINTS);
            assertThat(resultado.seInvocoAlModelo()).isFalse();
            verifyNoInteractions(invoker, spendGuard);
        }

        @Test
        @DisplayName("sin modelo degrada SIN reservar gasto")
        void sin_modelo_no_reserva() {
            when(hintQueryPort.findCurrentHints()).thenReturn(HINTS);
            when(invoker.isAvailable()).thenReturn(false);

            ProposalGenerationResult resultado = generator.generate(peticion);

            assertThat(resultado.outcome()).isEqualTo(GenerationOutcome.DEGRADED_MODEL_UNAVAILABLE);
            verifyNoInteractions(spendGuard);
            verify(invoker, never()).invoke(any());
        }

        @Test
        @DisplayName("con el tope agotado degrada y NO invoca: fail-closed")
        void el_tope_agotado_degrada() {
            when(hintQueryPort.findCurrentHints()).thenReturn(HINTS);
            when(invoker.isAvailable()).thenReturn(true);
            when(spendGuard.reserve(any())).thenReturn(Optional.empty());

            ProposalGenerationResult resultado = generator.generate(peticion);

            assertThat(resultado.outcome()).isEqualTo(GenerationOutcome.DEGRADED_SPEND_CAP);
            assertThat(resultado.draft().tieneLineas()).isFalse();
            verify(invoker, never()).invoke(any());
        }

        @Test
        @DisplayName("ninguna degradacion es un fallo: todas traen borrador utilizable")
        void toda_degradacion_trae_borrador() {
            when(hintQueryPort.findCurrentHints()).thenReturn(Map.of());

            assertThat(generator.generate(peticion).draft()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Cuando si se invoca")
    class Invocacion {

        @BeforeEach
        void hayModeloYCupo() {
            when(hintQueryPort.findCurrentHints()).thenReturn(HINTS);
            when(invoker.isAvailable()).thenReturn(true);
            when(spendGuard.reserve(any()))
                    .thenReturn(Optional.of(new SpendReservation("r-1", new BigDecimal("0.0166"))));
        }

        @Test
        @DisplayName("una respuesta buena produce el borrador y reconcilia el coste real")
        void respuesta_buena() {
            when(invoker.invoke(any())).thenReturn(respuesta("""
                    {"understood": true, "out_of_domain": false,
                     "necesarios": [
                       {"code": "CORE", "motivo": "Es el nucleo y va siempre contigo"},
                       {"code": "CLINICAL_HISTORY",
                        "motivo": "Porque atiendes consultas y vacunas"}],
                     "recomendados": [
                       {"code": "SCHEDULING",
                        "motivo": "Porque agendas citas cada dia con tus clientes"}],
                     "usuarios": 3, "sedes": 1, "cajas": 1}
                    """));

            ProposalGenerationResult resultado = generator.generate(peticion);

            assertThat(resultado.outcome()).isEqualTo(GenerationOutcome.SUCCEEDED);
            assertThat(resultado.draft().necessaryCodes()).containsExactly("CORE",
                    "CLINICAL_HISTORY");
            assertThat(resultado.draft().recommendedCodes()).containsExactly("SCHEDULING");
            assertThat(resultado.usage().inputTokens()).isEqualTo(3200);
            // El motivo viaja DENTRO de cada elemento del array (anexo E §2), no en
            // un mapa aparte: leerlo de un "motivos" de primer nivel dejaria todas
            // las lineas sin prosa y el saneador las sustituiria todas en silencio.
            assertThat(resultado.draft().reasons().get("CLINICAL_HISTORY").text())
                    .isEqualTo("Porque atiendes consultas y vacunas");
            // "usuarios", no "personas".
            assertThat(resultado.draft().capacities().staff()).isEqualTo(3);
            assertThat(resultado.draft().capacities().branches()).isEqualTo(1);

            ArgumentCaptor<BigDecimal> coste = ArgumentCaptor.forClass(BigDecimal.class);
            verify(spendGuard).reconcile(any(), coste.capture());
            // 3200 x 2/1M + 900 x 10/1M = 0,0064 + 0,009 = 0,0154
            assertThat(coste.getValue()).isEqualByComparingTo("0.015400");
        }

        @Test
        @DisplayName("un motivo con cifra sale saneado del generador, no del front")
        void el_motivo_sale_saneado() {
            when(invoker.invoke(any())).thenReturn(respuesta("""
                    {"understood": true, "out_of_domain": false,
                     "necesarios": [
                       {"code": "CLINICAL_HISTORY",
                        "motivo": "Porque facturas 40 millones al mes"}],
                     "recomendados": []}
                    """));

            ProposalGenerationResult resultado = generator.generate(peticion);

            assertThat(resultado.draft().reasons().get("CLINICAL_HISTORY").rule())
                    .isEqualTo(ReasonRejection.R3_CIFRA);
            assertThat(resultado.draft().reasons().get("CLINICAL_HISTORY").text())
                    .doesNotContain("40");
        }

        @Test
        @DisplayName("si el proveedor no honra strict y manda cadenas, se lee el codigo igual")
        void la_forma_degradada_de_cadena_suelta() {
            // strict:true lo hace cumplir el proveedor, no nosotros. La alternativa a
            // leer el codigo sin motivo seria tirar una respuesta ya pagada.
            when(invoker.invoke(any())).thenReturn(respuesta("""
                    {"understood": true, "out_of_domain": false,
                     "necesarios": ["CORE"], "recomendados": []}
                    """));

            ProposalGenerationResult resultado = generator.generate(peticion);

            assertThat(resultado.draft().necessaryCodes()).containsExactly("CORE");
            assertThat(resultado.draft().reasons().get("CORE").rule())
                    .isEqualTo(ReasonRejection.R1_CORTO);
        }

        @Test
        @DisplayName("un elemento sin code se descarta sin tumbar la respuesta entera")
        void un_elemento_sin_code() {
            when(invoker.invoke(any())).thenReturn(respuesta("""
                    {"understood": true, "out_of_domain": false,
                     "necesarios": [{"motivo": "prosa sin codigo"}, 7,
                                    {"code": "CORE", "motivo": "Es el nucleo y va contigo"}],
                     "recomendados": []}
                    """));

            assertThat(generator.generate(peticion).draft().necessaryCodes())
                    .containsExactly("CORE");
        }

        @Test
        @DisplayName("una capacidad fuera del rango del esquema se lee como «no lo se»")
        void capacidad_desbocada() {
            when(invoker.invoke(any())).thenReturn(respuesta("""
                    {"understood": true, "out_of_domain": false,
                     "necesarios": [{"code": "CORE", "motivo": "Es el nucleo y va contigo"}],
                     "recomendados": [], "usuarios": 4000, "sedes": 9000, "cajas": 2}
                    """));

            var capacidades = generator.generate(peticion).draft().capacities();

            assertThat(capacidades.staff()).isZero();
            assertThat(capacidades.branches()).isZero();
            assertThat(capacidades.terminals()).isEqualTo(2);
        }

        @Test
        @DisplayName("«fuera de dominio» con codigos: se descartan y se CUENTA la contradiccion")
        void la_contradiccion_se_cuenta() {
            when(invoker.invoke(any())).thenReturn(respuesta("""
                    {"understood": true, "out_of_domain": true,
                     "necesarios": [{"code": "CORE", "motivo": "Es el nucleo y va contigo"},
                                    {"code": "SCHEDULING", "motivo": "Porque agendas citas"}],
                     "recomendados": []}
                    """));

            ProposalGenerationResult resultado = generator.generate(peticion);

            assertThat(resultado.draft().tieneLineas()).isFalse();
            assertThat(resultado.draft().seContradijo()).isTrue();
            assertThat(resultado.draft().contradictoryCodes()).isEqualTo(2);
        }

        @Test
        @DisplayName("un JSON ilegible es MODEL_FAILED, no un 500 y tampoco un «no entendi»")
        void json_ilegible() {
            when(invoker.invoke(any())).thenReturn(respuesta("esto no es json {{{"));

            ProposalGenerationResult resultado = generator.generate(peticion);

            // Confundirlo con understood=false mezclaria en la metrica al prospecto
            // que escribio poco con una averia nuestra que estamos pagando.
            assertThat(resultado.outcome()).isEqualTo(GenerationOutcome.MODEL_FAILED);
            assertThat(resultado.failureCode()).isEqualTo("MODEL_OUTPUT_UNREADABLE");
            assertThat(resultado.draft().tieneLineas()).isFalse();
        }

        @Test
        @DisplayName("un fallo del modelo se COBRA igual: la llamada ya se pago")
        void un_fallo_se_cobra() {
            when(invoker.invoke(any())).thenThrow(
                    new ModelInvoker.ModelInvocationException("MODEL_TIMEOUT", "tardo demasiado"));

            ProposalGenerationResult resultado = generator.generate(peticion);

            assertThat(resultado.outcome()).isEqualTo(GenerationOutcome.MODEL_FAILED);
            assertThat(resultado.failureCode()).isEqualTo("MODEL_TIMEOUT");
            assertThat(resultado.latencyMs()).isNotNull();
            verify(spendGuard).reconcile(any(), any());
            verify(spendGuard, never()).release(any());
        }

        @Test
        @DisplayName("un fallo inesperado tampoco escapa como excepcion")
        void un_fallo_inesperado() {
            when(invoker.invoke(any())).thenThrow(new IllegalStateException("boom"));

            ProposalGenerationResult resultado = generator.generate(peticion);

            assertThat(resultado.outcome()).isEqualTo(GenerationOutcome.MODEL_FAILED);
            assertThat(resultado.failureCode()).isEqualTo("MODEL_UNEXPECTED_ERROR");
        }

        @Test
        @DisplayName("un modelo que no declara tokens se cobra la estimacion, no cero")
        void sin_tokens_declarados_se_cobra_la_estimacion() {
            when(invoker.invoke(any())).thenReturn(new ModelInvoker.ModelInvocation("claude", """
                    {"understood": false, "out_of_domain": false}
                    """, null, null, "end_turn"));

            generator.generate(peticion);

            ArgumentCaptor<BigDecimal> coste = ArgumentCaptor.forClass(BigDecimal.class);
            verify(spendGuard).reconcile(any(), coste.capture());
            // 3800 x 2/1M + 1000 x 10/1M = 0,0076 + 0,01 = 0,0176
            assertThat(coste.getValue()).isEqualByComparingTo("0.017600");
        }

        @Test
        @DisplayName("el prompt que se manda NO se puede imprimir con el texto dentro")
        void el_prompt_no_imprime_el_texto() {
            when(invoker.invoke(any())).thenReturn(respuesta("""
                    {"understood": false, "out_of_domain": false}
                    """));

            generator.generate(peticion);

            ArgumentCaptor<ProposalPrompt> prompt = ArgumentCaptor.forClass(ProposalPrompt.class);
            verify(invoker).invoke(prompt.capture());
            assertThat(prompt.getValue().user()).contains("Clinica de barrio");
            assertThat(prompt.getValue().toString()).doesNotContain("Clinica de barrio");
        }
    }

    @Test
    @DisplayName("el invocador que se despliega hoy declara que no hay modelo")
    void el_invocador_de_hoy() {
        ModelAccessNotEnabledInvoker deHoy = new ModelAccessNotEnabledInvoker();

        assertThat(deHoy.isAvailable()).isFalse();
        assertThatThrownBy(() -> deHoy.invoke(null))
                .isInstanceOf(ModelInvoker.ModelInvocationException.class)
                .extracting(
                        fallo -> ((ModelInvoker.ModelInvocationException) fallo).getFailureCode())
                .isEqualTo(ModelAccessNotEnabledInvoker.FAILURE_CODE);
    }
}
