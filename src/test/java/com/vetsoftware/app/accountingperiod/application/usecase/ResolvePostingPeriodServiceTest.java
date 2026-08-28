package com.vetsoftware.app.accountingperiod.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodKey;
import com.vetsoftware.app.accountingperiod.domain.NoOpenAccountingPeriodException;
import com.vetsoftware.app.accountingperiod.testsupport.AccountingPeriodMother;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * En que mes se registra un hecho.
 *
 * <p>
 * <b>Lo que estos casos congelan es la direccion de la busqueda.</b> El
 * servicio siempre pregunta «desde el mes de la fecha hacia adelante», nunca
 * hacia atras: el informe de marzo tiene que seguir dando lo que se declaro en
 * marzo. Un cambio que invirtiera el orden del {@code Sort} o que buscara el
 * ultimo abierto en vez del primero no rompe ninguna compilacion — lo unico que
 * lo caza es la clave con la que se consulta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResolvePostingPeriodService — el periodo de imputacion")
class ResolvePostingPeriodServiceTest {

    private static final LocalDate HECHO_DE_MARZO = LocalDate.of(2026, 3, 18);

    @Mock
    private AccountingPeriodRepository repository;
    @InjectMocks
    private ResolvePostingPeriodService service;

    @Nested
    @DisplayName("Resolucion")
    class Resolucion {

        @Test
        @DisplayName("consulta desde el mes de la fecha, no desde el mes en curso")
        void consulta_desde_el_mes_de_la_fecha() {
            when(repository.findFirstOpenFrom(AccountingPeriodKey.of("2026-03")))
                    .thenReturn(Optional.of(AccountingPeriodMother.persistidoAbierto(8800L)));

            AccountingPeriodDto resuelto = service.resolve(HECHO_DE_MARZO);

            assertThat(resuelto.periodKey()).isEqualTo("2026-03");
        }

        @Test
        @DisplayName("si el mes del hecho esta cerrado, devuelve el primer abierto POSTERIOR")
        void devuelve_el_primer_abierto_posterior() {
            // El puerto de salida resuelve las dos ramas con la misma consulta: la
            // primera clave >= la del hecho. Aqui marzo esta cerrado y lo que vuelve es
            // abril, que es la practica contable —el hecho se reconoce cuando se supo—.
            when(repository.findFirstOpenFrom(AccountingPeriodKey.of("2026-03"))).thenReturn(
                    Optional.of(AccountingPeriodMother.abiertoCon(AccountingPeriodMother.ABRIL)));

            assertThat(service.resolve(HECHO_DE_MARZO).periodKey()).isEqualTo("2026-04");
        }

        @Test
        @DisplayName("un hecho del 31 de diciembre se resuelve contra diciembre")
        void un_hecho_del_31_de_diciembre_se_resuelve_contra_diciembre() {
            // El caso que caza el patron de fecha equivocado: con YYYY en vez de yyyy
            // esta fecha consultaria 2027-12 y el hecho saltaria un ano entero.
            when(repository.findFirstOpenFrom(AccountingPeriodKey.of("2026-12")))
                    .thenReturn(Optional.of(
                            AccountingPeriodMother.abiertoCon(AccountingPeriodKey.of("2026-12"))));

            assertThat(service.resolve(LocalDate.of(2026, 12, 31)).periodKey())
                    .isEqualTo("2026-12");
        }
    }

    @Nested
    @DisplayName("Sin ningun mes abierto")
    class SinNingunMesAbierto {

        @Test
        @DisplayName("si no hay ningun mes abierto posterior, se rechaza en vez de ir hacia atras")
        void si_no_hay_mes_abierto_posterior_se_rechaza() {
            // La alternativa —imputarlo al ultimo mes abierto anterior— es lo intuitivo
            // y es exactamente lo que reescribiria un informe ya declarado. La salida es
            // abrir el periodo siguiente.
            when(repository.findFirstOpenFrom(AccountingPeriodKey.of("2026-03")))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(HECHO_DE_MARZO))
                    .isInstanceOf(NoOpenAccountingPeriodException.class)
                    .hasMessageContaining("No open accounting period on or after 2026-03");
        }
    }
}
