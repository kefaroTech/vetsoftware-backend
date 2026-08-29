package com.vetsoftware.app.entitlement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.entitlement.application.command.MarkCapacityUsageReconciledCommand;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El sello del consumo: «este contador se comprobo contra las filas reales».
 *
 * <p>
 * <b>Este servicio no tenia ningun test.</b> Es el segundo destino de
 * {@code EntitlementCapacityCounterAdapter} y el unico escritor de
 * {@code usage_reconciled_at}, una columna que existe desde el changeset 314 y
 * cuyo valor iba a ser {@code null} para siempre.
 *
 * <p>
 * <b>Lo unico que decide es el booleano</b>, y decide bastante: el recuento lo
 * usa para saber si habia contador que sellar. Devolver siempre {@code true}
 * —tragarse el cero del {@code UPDATE}— dejaria al barrido afirmando que sello
 * filas que ya no existen, y el indicador de salud diria «comprobado» sobre una
 * empresa sin contador. Por eso la frontera {@code &gt; 0} se prueba en los dos
 * lados y en el propio cero, no solo en el caso feliz.
 *
 * <p>
 * Los cuatro argumentos van con valores distintos entre si —empresa 900, eje
 * 43, periodo {@code 2026-08}— para que un cruce entre los dos {@code Long} no
 * pueda pasar: {@code STRICT_STUBS} rechaza la llamada si el servicio los
 * intercambia.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarkCapacityUsageReconciledService — el sello del consumo")
class MarkCapacityUsageReconciledServiceTest {

    private static final Long EMPRESA_ID = 900L;
    private static final Long EJE_ID = 43L;
    private static final String PERIODO = "2026-08";
    private static final LocalDateTime SELLADO_EN = LocalDateTime.of(2026, 8, 28, 3, 15);

    @Mock
    private CompanyCapacityRepository repository;
    @InjectMocks
    private MarkCapacityUsageReconciledService service;

    @Nested
    @DisplayName("Que dice el booleano")
    class Resultado {

        /**
         * <b>El cero es el caso que importa.</b> El {@code UPDATE} no encuentra fila
         * cuando la empresa no tiene contador de ese eje en ese periodo —o cuando se le
         * paso la empresa de otro—, y ese «no habia nada que sellar» tiene que llegar
         * arriba tal cual.
         */
        @ParameterizedTest(name = "el motor movio {0} filas → sellado {1}")
        @CsvSource({"0, false", "1, true", "2, true"})
        @DisplayName("solo hay sello si el motor movio alguna fila")
        void solo_hay_sello_si_el_motor_movio_alguna_fila(int filas, boolean sellado) {
            when(repository.markUsageReconciled(EMPRESA_ID, EJE_ID, PERIODO, SELLADO_EN))
                    .thenReturn(filas);

            assertThat(service.execute(unSello())).isEqualTo(sellado);
        }
    }

    @Nested
    @DisplayName("Validaciones del comando")
    class Validaciones {

        /**
         * Las invariantes viven en el {@code record} compacto, asi que ni siquiera se
         * llega a construir un comando invalido: el repositorio no se toca. Es la mitad
         * del valor del caso —«no debe escribir»— y se afirma con
         * {@code verifyNoInteractions}.
         */
        @Test
        @DisplayName("un sello sin empresa no llega al repositorio")
        void un_sello_sin_empresa_no_llega_al_repositorio() {
            assertThatThrownBy(() -> service.execute(
                    new MarkCapacityUsageReconciledCommand(null, EJE_ID, PERIODO, SELLADO_EN)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un sello sin eje no llega al repositorio")
        void un_sello_sin_eje_no_llega_al_repositorio() {
            assertThatThrownBy(() -> service.execute(
                    new MarkCapacityUsageReconciledCommand(EMPRESA_ID, null, PERIODO, SELLADO_EN)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("limit dimension id is required");

            verifyNoInteractions(repository);
        }

        /**
         * <b>La cadena en blanco, no solo el nulo.</b> Un contador que no es de flujo
         * lleva el centinela {@code ALLTIME}; dejar pasar {@code ""} escribiria el
         * sello sobre una clave de periodo que no existe —cero filas, silencio— y el
         * barrido lo leeria como «no habia contador».
         */
        @ParameterizedTest(name = "periodo [{0}]")
        @CsvSource(value = {"NULO", "''", "'   '"}, nullValues = "NULO")
        @DisplayName("un sello sin clave de periodo real no llega al repositorio")
        void un_sello_sin_clave_de_periodo_real_no_llega_al_repositorio(String periodo) {
            assertThatThrownBy(
                    () -> service.execute(new MarkCapacityUsageReconciledCommand(EMPRESA_ID, EJE_ID,
                            periodo, SELLADO_EN)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("period key is required");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un sello sin fecha no llega al repositorio")
        void un_sello_sin_fecha_no_llega_al_repositorio() {
            assertThatThrownBy(() -> service.execute(
                    new MarkCapacityUsageReconciledCommand(EMPRESA_ID, EJE_ID, PERIODO, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reconciled at is required");

            verifyNoInteractions(repository);
        }
    }

    private static MarkCapacityUsageReconciledCommand unSello() {
        return new MarkCapacityUsageReconciledCommand(EMPRESA_ID, EJE_ID, PERIODO, SELLADO_EN);
    }
}
