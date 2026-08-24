package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * El consecutivo contra MySQL real.
 *
 * <p>
 * <b>Por qué hace falta además del test unitario.</b>
 * {@code JpaSubscriptionNumberPortTest} dobla el {@code EntityManager} y afirma
 * sobre el texto del SQL: comprueba que la sentencia dice
 * {@code ON DUPLICATE KEY UPDATE} y que se escribe antes de leer, que es la
 * decisión de diseño. Lo que <b>no</b> puede comprobar es que MySQL haga con
 * esa sentencia lo que el javadoc afirma —el caso del primer número del año,
 * cuando no hay fila que bloquear, es justo el que hundió al consecutivo de
 * cotizaciones— porque ahí no se ejecuta ningún SQL. Esta rodaja sí lo ejecuta.
 *
 * <p>
 * Se usan años futuros y sin uso ({@code 2099}, {@code 2098}) para que la
 * primera reserva sea de verdad la primera: si el año ya tuviera fila, el caso
 * interesante no se probaría y el test pasaría igual.
 */
@Import(JpaSubscriptionNumberPort.class)
@DisplayName("JpaSubscriptionNumberPort — el consecutivo contra MySQL real")
class SubscriptionNumberPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSubscriptionNumberPort port;
    @PersistenceContext
    private EntityManager entityManager;

    private long valorSiguienteDe(String prefijo) {
        return ((Number) entityManager
                .createNativeQuery("SELECT next_value FROM billing_document_sequences "
                        + "WHERE prefix = :prefix")
                .setParameter("prefix", prefijo).getSingleResult()).longValue();
    }

    private long filasConPrefijo(String prefijo) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM billing_document_sequences "
                        + "WHERE prefix = :prefix")
                .setParameter("prefix", prefijo).getSingleResult()).longValue();
    }

    @Nested
    @DisplayName("El primer número del año")
    class ElPrimerNumeroDelAno {

        @Test
        @DisplayName("con el año vacío da el 00001 y deja la fila creada en 2")
        void conElAnoVacioDaElPrimero() {
            // El 1 de enero no hay fila que bloquear. Un SELECT ... FOR UPDATE previo no
            // bloquearia nada y dos altas simultaneas se llevarian el mismo numero; el
            // INSERT ... ON DUPLICATE KEY UPDATE crea la fila y la bloquea en la misma
            // sentencia. Esta asercion es la que prueba que MySQL lo hace de verdad.
            assertThat(filasConPrefijo("SUS-2099")).isZero();

            String primero = port.nextSubscriptionNumber(2099);

            assertThat(primero).isEqualTo("SUS-2099-00001");
            assertThat(valorSiguienteDe("SUS-2099")).isEqualTo(2L);
        }

        @Test
        @DisplayName("la reserva siguiente incrementa sin releer un máximo")
        void laReservaSiguienteIncrementa() {
            assertThat(port.nextSubscriptionNumber(2099)).isEqualTo("SUS-2099-00001");
            assertThat(port.nextSubscriptionNumber(2099)).isEqualTo("SUS-2099-00002");
            assertThat(port.nextSubscriptionNumber(2099)).isEqualTo("SUS-2099-00003");
            assertThat(valorSiguienteDe("SUS-2099")).isEqualTo(4L);
        }

        @Test
        @DisplayName("cada año arranca su propia serie desde el uno")
        void cadaAnoArrancaDesdeElUno() {
            port.nextSubscriptionNumber(2098);
            port.nextSubscriptionNumber(2098);

            assertThat(port.nextSubscriptionNumber(2099)).isEqualTo("SUS-2099-00001");
            assertThat(port.nextSubscriptionNumber(2098)).isEqualTo("SUS-2098-00003");
        }
    }

    @Nested
    @DisplayName("Las dos series no se pisan")
    class LasDosSeries {

        @Test
        @DisplayName("el contrato y el otrosí llevan contadores independientes del mismo año")
        void contratoYOtrosiSonIndependientes() {
            // Comparten tabla y año, y solo los separa el prefijo. Si la clave unica no
            // fuera por prefijo completo, el primer otrosi del ano heredaria el numero
            // del ultimo contrato y dos documentos distintos se citarian igual.
            assertThat(port.nextSubscriptionNumber(2099)).isEqualTo("SUS-2099-00001");
            assertThat(port.nextSubscriptionNumber(2099)).isEqualTo("SUS-2099-00002");

            assertThat(port.nextAmendmentNumber(2099)).isEqualTo("AMD-2099-00001");
            assertThat(valorSiguienteDe("SUS-2099")).isEqualTo(3L);
            assertThat(valorSiguienteDe("AMD-2099")).isEqualTo(2L);
        }

        @Test
        @DisplayName("el prefijo con el año cabe en la columna VARCHAR(10)")
        void elPrefijoCabeEnLaColumna() {
            // 8 caracteres en una columna de 10. Si algun dia la serie creciera, el
            // INSERT fallaria con truncamiento y el alta entera se caeria.
            port.nextAmendmentNumber(2099);

            assertThat(filasConPrefijo("AMD-2099")).isEqualTo(1L);
        }
    }
}
