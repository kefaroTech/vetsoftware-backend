package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.command.SuppressProposalDataCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalSuppressionDto;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalRetentionPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La supresion a peticion del titular (articulo 8, literal e, de la Ley 1581).
 *
 * <p>
 * &#9940; <b>Un borrado que deja la frase del titular escrita en la tabla de al
 * lado no es un borrado.</b> Por eso el resultado viene desglosado por tabla:
 * "una fila suprimida" con cero motivos es exactamente el estado que hay que
 * poder distinguir, y con un unico total no se distingue.
 *
 * <p>
 * &#9940; <b>Y un borrado del que no queda constancia tampoco sirve.</b> Este
 * caso de uso no escribe la evidencia —eso es del adaptador, que es quien tiene
 * la transaccion— pero si es quien le pasa las dos cosas que la fila necesita y
 * el servicio no puede inventarse: quien la atiende y cuando.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuppressProposalDataService — borrar lo que el titular pidio borrar")
class SuppressProposalDataServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-08-30T15:00:00Z"),
            ZoneOffset.UTC);

    private static final Long ACTOR = 6L;

    @Mock
    private ProposalRetentionPort retention;

    private SuppressProposalDataService service;

    @BeforeEach
    void montar() {
        service = new SuppressProposalDataService(retention, RELOJ);
    }

    private static SuppressProposalDataCommand comando(String correo) {
        return new SuppressProposalDataCommand(correo, ACTOR);
    }

    @Nested
    @DisplayName("Ejecucion")
    class Ejecucion {

        @Test
        @DisplayName("pasa el correo, el actor y el instante del reloj, y devuelve el desglose")
        void pasa_el_correo_y_devuelve_el_desglose() {
            when(retention.suppressByContactEmail(anyString(), any(), any()))
                    .thenReturn(new ProposalRetentionPort.SuppressionResult(1, 2, 5, null));

            ProposalSuppressionDto dto = service.execute(comando("laura@vetchapinero.co"));

            ArgumentCaptor<String> correo = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> actor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<LocalDateTime> momento = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(retention).suppressByContactEmail(correo.capture(), actor.capture(),
                    momento.capture());
            assertThat(correo.getValue()).isEqualTo("laura@vetchapinero.co");
            assertThat(actor.getValue()).isEqualTo(ACTOR);
            assertThat(momento.getValue()).isEqualTo(LocalDateTime.now(RELOJ));

            assertThat(dto.proposals()).isEqualTo(1);
            assertThat(dto.turns()).isEqualTo(2);
            assertThat(dto.lines()).isEqualTo(5);
            assertThat(dto.total()).isEqualTo(8);
        }

        /**
         * &#9940; El instante que va a la base y el que va al acuse tienen que ser el
         * mismo. Con dos lecturas del reloj, la constancia y la respuesta que se le
         * ensena al titular dirian horas distintas del mismo hecho —y ese hecho es
         * justo lo que hay que poder defender ante la SIC—.
         */
        @Test
        @DisplayName("el instante del acuse es el mismo que se le pasa al adaptador")
        void el_instante_del_acuse_es_el_que_se_le_pasa_al_adaptador() {
            when(retention.suppressByContactEmail(anyString(), any(), any()))
                    .thenReturn(new ProposalRetentionPort.SuppressionResult(1, 0, 0, null));

            ProposalSuppressionDto dto = service.execute(comando("laura@vetchapinero.co"));

            ArgumentCaptor<LocalDateTime> momento = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(retention).suppressByContactEmail(anyString(), any(), momento.capture());
            assertThat(dto.suppressedAt()).isEqualTo(momento.getValue());
            assertThat(dto.suppressedAt()).isEqualTo(LocalDateTime.now(RELOJ));
        }

        /**
         * Los dos ceros que sin esta fecha se leen igual: "ya se le habia borrado" y
         * "nunca hubo nada suyo". El primer borrado se lleva el hash por el que se
         * busca, asi que la segunda peticion del mismo titular devuelve ceros
         * exactamente como una direccion que no estuvo nunca.
         */
        @Test
        @DisplayName("la fecha de la peticion anterior llega al acuse sin tocarla")
        void la_fecha_de_la_peticion_anterior_llega_al_acuse() {
            LocalDateTime enJulio = LocalDateTime.of(2026, 7, 3, 9, 0);
            when(retention.suppressByContactEmail(anyString(), any(), any()))
                    .thenReturn(new ProposalRetentionPort.SuppressionResult(0, 0, 0, enJulio));

            ProposalSuppressionDto dto = service.execute(comando("laura@vetchapinero.co"));

            assertThat(dto.total()).isZero();
            assertThat(dto.previouslySuppressedAt())
                    .as("sin esto, este acuse se lee como 'aqui no habia nada'").isEqualTo(enJulio);
        }

        @Test
        @DisplayName("un correo sin propuestas devuelve ceros, no un fallo")
        void un_correo_sin_propuestas_devuelve_ceros() {
            when(retention.suppressByContactEmail(anyString(), any(), any()))
                    .thenReturn(new ProposalRetentionPort.SuppressionResult(0, 0, 0, null));

            assertThat(service.execute(comando("nadie@vet.co")).total()).isZero();
            assertThat(service.execute(comando("nadie@vet.co")).previouslySuppressedAt())
                    .as("nunca hubo peticion previa de esta direccion").isNull();
        }
    }

    @Nested
    @DisplayName("Invariantes del command")
    class InvariantesDelCommand {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("un command sin correo no llega a existir")
        void un_command_sin_correo_no_llega_a_existir(String correo) {
            assertThatThrownBy(() -> new SuppressProposalDataCommand(correo, ACTOR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("contactEmail is required");
        }

        /**
         * &#9940; Una constancia que no puede nombrar a quien atendio la peticion no
         * sirve para lo unico que se le va a pedir. Si el actor pudiera ser nulo, la
         * fila se escribiria igual y el informe diria que la supresion la hizo nadie.
         */
        @Test
        @DisplayName("un command sin quien lo ejecuta no llega a existir")
        void un_command_sin_actor_no_llega_a_existir() {
            assertThatThrownBy(() -> new SuppressProposalDataCommand("laura@vet.co", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("executedBySystemUserId is required");
        }
    }
}
