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
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuppressProposalDataService — borrar lo que el titular pidio borrar")
class SuppressProposalDataServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-08-30T15:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private ProposalRetentionPort retention;

    private SuppressProposalDataService service;

    @BeforeEach
    void montar() {
        service = new SuppressProposalDataService(retention, RELOJ);
    }

    @Nested
    @DisplayName("Ejecucion")
    class Ejecucion {

        @Test
        @DisplayName("pasa el correo y el instante del reloj, y devuelve el desglose")
        void pasa_el_correo_y_devuelve_el_desglose() {
            when(retention.suppressByContactEmail(anyString(), any()))
                    .thenReturn(new ProposalRetentionPort.SuppressionResult(1, 2, 5));

            ProposalSuppressionDto dto = service
                    .execute(new SuppressProposalDataCommand("laura@vetchapinero.co"));

            ArgumentCaptor<String> correo = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<LocalDateTime> momento = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(retention).suppressByContactEmail(correo.capture(), momento.capture());
            assertThat(correo.getValue()).isEqualTo("laura@vetchapinero.co");
            assertThat(momento.getValue()).isEqualTo(LocalDateTime.now(RELOJ));

            assertThat(dto.proposals()).isEqualTo(1);
            assertThat(dto.turns()).isEqualTo(2);
            assertThat(dto.lines()).isEqualTo(5);
            assertThat(dto.total()).isEqualTo(8);
        }

        @Test
        @DisplayName("un correo sin propuestas devuelve ceros, no un fallo")
        void un_correo_sin_propuestas_devuelve_ceros() {
            when(retention.suppressByContactEmail(anyString(), any()))
                    .thenReturn(new ProposalRetentionPort.SuppressionResult(0, 0, 0));

            assertThat(service.execute(new SuppressProposalDataCommand("nadie@vet.co")).total())
                    .isZero();
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
            assertThatThrownBy(() -> new SuppressProposalDataCommand(correo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("contactEmail is required");
        }
    }
}
