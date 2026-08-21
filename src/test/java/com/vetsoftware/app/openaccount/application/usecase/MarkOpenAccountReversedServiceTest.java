package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.command.MarkOpenAccountReversedCommand;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El camino REAL del reverso de cartera (#124).
 *
 * <p>
 * Antes, quien escribia {@code reversed} era un adaptador de otra feature que
 * habia reimplementado la regla a mano: copio la idempotencia y omitio la
 * guarda de estado. {@code OpenAccountTest} probaba la guarda del dominio sobre
 * codigo que produccion no ejecutaba, asi que estaba verde y no cubria nada.
 * Esta clase es lo que faltaba: comprueba que la invariante se aplica donde se
 * escribe.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarkOpenAccountReversedService — la invariante del reverso en el camino que escribe")
class MarkOpenAccountReversedServiceTest {

    private static final LocalDateTime REVERSADA_EL = LocalDateTime.of(2026, 3, 1, 8, 0);

    @Mock
    private OpenAccountRepository repository;
    @InjectMocks
    private MarkOpenAccountReversedService service;

    private static MarkOpenAccountReversedCommand comando(LocalDateTime cuando) {
        return new MarkOpenAccountReversedCommand(OpenAccountMother.OPEN_ACCOUNT_ID,
                OpenAccountMother.COMPANY_ID, cuando);
    }

    private void cuentaEnLaEmpresa(OpenAccount cuenta) {
        when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                OpenAccountMother.COMPANY_ID)).thenReturn(Optional.ofNullable(cuenta));
    }

    @Nested
    @DisplayName("reverso aplicado")
    class ReversoAplicado {

        @Test
        @DisplayName("una cuenta cerrada se marca reversada con la fecha dada y se persiste")
        void cuenta_cerrada_se_marca_y_se_persiste() {
            cuentaEnLaEmpresa(OpenAccountMother.cerrada());

            service.execute(comando(REVERSADA_EL));

            ArgumentCaptor<OpenAccount> captor = ArgumentCaptor.forClass(OpenAccount.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().isReversed()).isTrue();
            assertThat(captor.getValue().getReversedAt()).isEqualTo(REVERSADA_EL);
        }

        @Test
        @DisplayName("sin fecha en el command la pone el dominio")
        void sin_fecha_la_pone_el_dominio() {
            cuentaEnLaEmpresa(OpenAccountMother.cerrada());

            service.execute(comando(null));

            ArgumentCaptor<OpenAccount> captor = ArgumentCaptor.forClass(OpenAccount.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getReversedAt()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("la guarda de estado se aplica aqui, no solo en el dominio")
    class GuardaDeEstado {

        static Stream<Arguments> cuentasNoFacturadas() {
            return Stream.of(Arguments.of("OPEN", OpenAccountMother.abierta()),
                    Arguments.of("CANCEL", OpenAccountMother.cancelada("incobrable")));
        }

        /**
         * Esta es la afirmacion que el issue pedia y que nadie hacia: por el camino de
         * produccion, reversar una cuenta que no esta CLOSE falla y no escribe. Con el
         * adaptador viejo —que solo miraba {@code isReversed()}— este test fallaba: la
         * cuenta OPEN quedaba con {@code reversed = true} y nadie protestaba.
         */
        @ParameterizedTest(name = "estado {0}")
        @MethodSource("cuentasNoFacturadas")
        @DisplayName("una cuenta que nunca se facturo no se reversa ni se persiste")
        void cuenta_no_facturada_no_se_reversa(String estado, OpenAccount cuenta) {
            cuentaEnLaEmpresa(cuenta);

            assertThatThrownBy(() -> service.execute(comando(REVERSADA_EL)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Solo se puede reversar una cuenta cerrada");

            verify(repository, never()).save(any());
            assertThat(cuenta.isReversed()).isFalse();
            assertThat(cuenta.getReversedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("lo que no debe escribir")
    class NoEscribe {

        @Test
        @DisplayName("una cuenta ya reversada es idempotente: no reescribe la fecha ni persiste")
        void cuenta_ya_reversada_no_se_vuelve_a_escribir() {
            OpenAccount cuenta = OpenAccountMother.cerradaYaReversada(REVERSADA_EL);
            cuentaEnLaEmpresa(cuenta);

            service.execute(comando(LocalDateTime.of(2026, 4, 1, 8, 0)));

            // Sin save no hay UPDATE y la version del bloqueo optimista no se mueve: un
            // webhook reentregado por la DIAN no puede provocar un 409 en la edicion
            // concurrente de la cuenta.
            verify(repository, never()).save(any());
            assertThat(cuenta.getReversedAt()).isEqualTo(REVERSADA_EL);
        }

        @Test
        @DisplayName("una cuenta inexistente o de otra empresa no se escribe y no rompe la nota")
        void cuenta_de_otra_empresa_no_se_toca() {
            // El lock pesimista va acotado por empresa: una cuenta ajena ni se bloquea ni
            // se lee. Y el no-op es deliberado — lanzar aqui reventaria la transaccion
            // que acaba de registrar una nota credito ya VALIDADA por la DIAN.
            cuentaEnLaEmpresa(null);

            service.execute(comando(REVERSADA_EL));

            verify(repository, never()).save(any());
            verify(repository, never()).findById(anyLong());
        }
    }
}
