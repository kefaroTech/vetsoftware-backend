package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.port.out.ProposalRetentionPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El adaptador delega, y en la supresion <b>ordena</b>. Lo segundo es lo que
 * hace falta probar.
 *
 * <p>
 * &#9940; <b>El orden de los tres pasos de la supresion es al reves que el del
 * barrido, y es lo unico que hace que funcione.</b> El paso que borra
 * {@code contact_email} destruye {@code contact_email_hash} —columna
 * {@code GENERATED ALWAYS ... STORED}—, que es por lo que buscan los otros dos.
 * Invertirlo deja los motivos del titular escritos, con el metodo devolviendo
 * "una fila suprimida" y el informe de cumplimiento diciendo que se le borro.
 * Es un defecto que ninguna revision humana ve leyendo las tres llamadas,
 * porque las tres se leen bien.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaProposalRetentionPort — delegacion y, en la supresion, orden")
class JpaProposalRetentionPortTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 30, 3, 55);

    private static final LocalDateTime CORTE = LocalDateTime.of(2026, 6, 1, 0, 0);

    @Mock
    private AiProposalRetentionJpaRepository jpaRepository;

    @InjectMocks
    private JpaProposalRetentionPort port;

    @Nested
    @DisplayName("Barrido")
    class Barrido {

        @Test
        @DisplayName("cada paso pasa su corte y su lote tal cual y devuelve las filas movidas")
        void cada_paso_delega_con_sus_argumentos() {
            when(jpaRepository.anonymizeProposals(CORTE, AHORA, 50)).thenReturn(7);
            when(jpaRepository.redactTurns(50)).thenReturn(9);
            when(jpaRepository.redactLineReasons(AHORA, 50)).thenReturn(11);
            when(jpaRepository.purgeLines(CORTE, 50)).thenReturn(3);
            when(jpaRepository.purgeTurns(CORTE, 50)).thenReturn(2);
            when(jpaRepository.purgeProposals(CORTE, 50)).thenReturn(1);

            assertThat(port.anonymizeProposals(CORTE, AHORA, 50)).isEqualTo(7);
            assertThat(port.redactTurns(50)).isEqualTo(9);
            assertThat(port.redactLineReasons(AHORA, 50)).isEqualTo(11);
            assertThat(port.purgeLines(CORTE, 50)).isEqualTo(3);
            assertThat(port.purgeTurns(CORTE, 50)).isEqualTo(2);
            assertThat(port.purgeProposals(CORTE, 50)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Supresion dirigida")
    class SupresionDirigida {

        @Test
        @DisplayName("borra motivos y turnos ANTES que el correo, o el hash ya no existiria")
        void borra_los_motivos_antes_que_el_correo() {
            when(jpaRepository.suppressLinesByEmail("laura@vet.co", AHORA)).thenReturn(4);
            when(jpaRepository.suppressTurnsByEmail("laura@vet.co")).thenReturn(2);
            when(jpaRepository.suppressProposalsByEmail("laura@vet.co", AHORA)).thenReturn(1);

            ProposalRetentionPort.SuppressionResult resultado = port
                    .suppressByContactEmail("laura@vet.co", AHORA);

            var enOrden = inOrder(jpaRepository);
            enOrden.verify(jpaRepository).suppressLinesByEmail("laura@vet.co", AHORA);
            enOrden.verify(jpaRepository).suppressTurnsByEmail("laura@vet.co");
            enOrden.verify(jpaRepository).suppressProposalsByEmail("laura@vet.co", AHORA);

            assertThat(resultado.lines()).isEqualTo(4);
            assertThat(resultado.turns()).isEqualTo(2);
            assertThat(resultado.proposals()).isEqualTo(1);
            assertThat(resultado.total()).isEqualTo(7);
        }

        /**
         * Sin este corte, un correo vacio se convertiria en
         * {@code UNHEX(SHA2(LOWER(''), 256))}, que es un hash perfectamente valido: no
         * casaria con nada hoy, pero el metodo estaria preguntando a la base por una
         * cadena vacia en vez de negarse.
         */
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("un correo en blanco no llega a la base y devuelve el resultado vacio")
        void un_correo_en_blanco_no_llega_a_la_base(String correo) {
            ProposalRetentionPort.SuppressionResult resultado = port.suppressByContactEmail(correo,
                    AHORA);

            assertThat(resultado.total()).isZero();
            verifyNoInteractions(jpaRepository);
        }

        @Test
        @DisplayName("un correo que no esta devuelve ceros, no un fallo")
        void un_correo_que_no_esta_devuelve_ceros() {
            when(jpaRepository.suppressLinesByEmail(anyString(), any())).thenReturn(0);
            when(jpaRepository.suppressTurnsByEmail(anyString())).thenReturn(0);
            when(jpaRepository.suppressProposalsByEmail(anyString(), any())).thenReturn(0);

            assertThat(port.suppressByContactEmail("nadie@vet.co", AHORA).total()).isZero();
        }
    }

    @Nested
    @DisplayName("El resultado")
    class Resultado {

        @Test
        @DisplayName("el total es la suma de las tres tablas, no un contador aparte")
        void el_total_es_la_suma() {
            assertThat(new ProposalRetentionPort.SuppressionResult(1, 2, 3).total()).isEqualTo(6);
            assertThat(new ProposalRetentionPort.SuppressionResult(0, 0, 0).total()).isZero();
        }

        /**
         * El desglose no es cosmetico: un unico total deja indistinguibles "ese correo
         * no esta" y "el paso de motivos no toco nada porque su subconsulta esta rota".
         */
        @Test
        @DisplayName("el desglose separa lo que un total unico confundiria")
        void el_desglose_separa_lo_que_el_total_confunde() {
            var soloCabecera = new ProposalRetentionPort.SuppressionResult(1, 0, 0);

            assertThat(soloCabecera.lines())
                    .as("cabecera borrada y cero motivos es exactamente el defecto de esta fase")
                    .isZero();
            assertThat(soloCabecera.total()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Argumentos del barrido")
    class ArgumentosDelBarrido {

        @Test
        @DisplayName("el lote no se reinterpreta por el camino")
        void el_lote_no_se_reinterpreta() {
            when(jpaRepository.redactTurns(anyInt())).thenReturn(0);

            port.redactTurns(123);

            org.mockito.Mockito.verify(jpaRepository).redactTurns(123);
        }
    }
}
