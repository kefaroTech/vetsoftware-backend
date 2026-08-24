package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>Este test dobla el {@code EntityManager}, asi que NO ejecuta una sola
 * sentencia.</b> Verifica <em>que SQL se emite</em> —que la reserva no sale de
 * un maximo mas uno, que va antes de la lectura—, que es lo que un doble puede
 * probar y una rodaja no. Quien ejecuta ese SQL contra MySQL real es
 * {@code SubscriptionNumberPersistenceIT}, en este mismo paquete.
 *
 * <p>
 * <b>Cuidado con esa pareja</b> (#425). Para JaCoCo los dos tests son
 * indistinguibles: los dos marcan el SQL nativo como cubierto, pero solo uno lo
 * ha ejecutado. Si alguien borra {@code SubscriptionNumberPersistenceIT} la
 * cobertura no se mueve ni un punto y el SQL vuelve a no ejecutarlo nadie, en
 * silencio. {@code ADAPTADOR_JPA_CON_RODAJA} no protege este caso: la regla
 * solo alcanza a los {@code Jpa<Algo>Repository}, y esto es un {@code …Port}.
 *
 * <p>
 * <b>Lo que no demuestra ninguno de los dos es la ausencia de carrera</b>: eso
 * exige dos transacciones concurrentes de verdad y hoy no lo hace ningun test
 * del repositorio (#61).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSubscriptionNumberPort - el consecutivo no sale de un maximo mas uno")
class JpaSubscriptionNumberPortTest {

    @Mock
    private EntityManager entityManager;
    @Mock(name = "reserva")
    private Query reserva;
    @Mock(name = "lectura")
    private Query lectura;

    @InjectMocks
    private JpaSubscriptionNumberPort port;

    /** Encadena los dos native queries en el orden en que los pide el adaptador. */
    private void conSecuenciaEn(long valorReservado) {
        when(entityManager.createNativeQuery(anyString())).thenReturn(reserva, lectura);
        when(reserva.setParameter(eq("prefix"), anyString())).thenReturn(reserva);
        when(lectura.setParameter(eq("prefix"), anyString())).thenReturn(lectura);
        when(lectura.setMaxResults(anyInt())).thenReturn(lectura);
        when(lectura.getResultList()).thenReturn(List.of(valorReservado));
    }

    private List<String> sqlEjecutado() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, org.mockito.Mockito.atLeastOnce())
                .createNativeQuery(captor.capture());
        return captor.getAllValues();
    }

    @Nested
    @DisplayName("La primera reserva de la serie")
    class PrimeraReserva {

        @Test
        @DisplayName("crea la fila e incrementa en una sola sentencia, sin leer antes")
        void creaOIncrementaSinLeerAntes() {
            conSecuenciaEn(1L);

            String numero = port.nextSubscriptionNumber(2027);

            // El 1 de enero no hay fila que bloquear: un SELECT ... FOR UPDATE previo
            // no bloquearia nada y dos altas simultaneas se llevarian el mismo numero.
            // Por eso la reserva es INSERT ... ON DUPLICATE KEY UPDATE, que crea o
            // incrementa sin leer, y deja la fila bloqueada en exclusiva hasta el commit.
            String reservaSql = sqlEjecutado().get(0);
            assertThat(reservaSql).contains("INSERT INTO billing_document_sequences")
                    .contains("ON DUPLICATE KEY UPDATE next_value = next_value + 1");
            assertThat(reservaSql).doesNotContain("SELECT");
            assertThat(numero).isEqualTo("SUS-2027-00001");
        }

        @Test
        @DisplayName("escribe primero y lee despues: la lectura no es una carrera nueva")
        void escribeAntesDeLeer() {
            conSecuenciaEn(1L);

            port.nextSubscriptionNumber(2027);

            InOrder orden = inOrder(reserva, lectura);
            orden.verify(reserva).executeUpdate();
            orden.verify(lectura).getResultList();
        }

        @Test
        @DisplayName("la lectura del valor reservado no vuelve a pedir el candado")
        void laLecturaNoPideCandado() {
            conSecuenciaEn(1L);

            port.nextSubscriptionNumber(2027);

            assertThat(sqlEjecutado().get(1)).doesNotContain("FOR UPDATE");
        }
    }

    @Nested
    @DisplayName("Las dos series")
    class Series {

        @Test
        @DisplayName("el contrato usa el prefijo SUS del ano")
        void serieDelContrato() {
            conSecuenciaEn(184L);

            assertThat(port.nextSubscriptionNumber(2026)).isEqualTo("SUS-2026-00184");
            verify(reserva).setParameter("prefix", "SUS-2026");
        }

        @Test
        @DisplayName("el otrosi usa el prefijo AMD del ano")
        void serieDelOtrosi() {
            conSecuenciaEn(12L);

            assertThat(port.nextAmendmentNumber(2026)).isEqualTo("AMD-2026-00012");
            verify(reserva).setParameter("prefix", "AMD-2026");
        }

        @Test
        @DisplayName("los dos prefijos caben en el VARCHAR(10) de la columna")
        void losPrefijosCaben() {
            assertThat("SUS-2026").hasSizeLessThanOrEqualTo(10);
            assertThat("AMD-2026").hasSizeLessThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Si el contador desaparece")
    class ContadorPerdido {

        @Test
        @DisplayName("falla en vez de arrancar la serie desde cero")
        void fallaEnVezDeReiniciar() {
            when(entityManager.createNativeQuery(anyString())).thenReturn(reserva, lectura);
            when(reserva.setParameter(eq("prefix"), anyString())).thenReturn(reserva);
            when(lectura.setParameter(eq("prefix"), anyString())).thenReturn(lectura);
            when(lectura.setMaxResults(anyInt())).thenReturn(lectura);
            when(lectura.getResultList()).thenReturn(List.of());

            // Reiniciar la serie duplicaria numeros ya citados en cobranza. Es peor que
            // fallar aqui.
            assertThatThrownBy(() -> port.nextSubscriptionNumber(2026))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("SUS-2026");
        }
    }
}
