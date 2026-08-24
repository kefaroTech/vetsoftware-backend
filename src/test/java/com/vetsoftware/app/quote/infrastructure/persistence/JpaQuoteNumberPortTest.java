package com.vetsoftware.app.quote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lo que este test SI demuestra: que el camino "el ano todavia no tiene fila"
 * no lanza y devuelve el 1, que es la mitad que estaba rota; y que la forma de
 * la reserva es un upsert atomico y no un maximo mas uno.
 *
 * <p>
 * <b>Este test dobla el {@code EntityManager}, asi que NO ejecuta una sola
 * sentencia.</b> Verifica <em>que SQL se emite</em> —que no hay {@code MAX},
 * que la reserva va antes de la lectura— y eso es exactamente lo que un doble
 * puede probar y una rodaja no. Lo que el doble no puede es ejecutar el
 * {@code INSERT … ON DUPLICATE KEY UPDATE}: quien lo ejecuta contra MySQL real
 * es {@code QuoteNumberPortIT}, en este mismo paquete.
 *
 * <p>
 * <b>Cuidado con esa pareja</b> (#425). Para JaCoCo los dos tests son
 * indistinguibles: los dos marcan el SQL nativo como cubierto, pero solo uno lo
 * ha ejecutado. Si alguien borra {@code QuoteNumberPortIT} la cobertura no se
 * mueve ni un punto y el SQL vuelve a no ejecutarlo nadie, en silencio.
 * {@code ADAPTADOR_JPA_CON_RODAJA} no protege este caso: la regla solo alcanza
 * a los {@code Jpa<Algo>Repository}, y esto es un {@code …Port}.
 *
 * <p>
 * <b>Lo que no demuestra ninguno de los dos, y conviene no confundirlo: la
 * ausencia de carrera.</b> Eso exige dos transacciones concurrentes de verdad y
 * hoy no lo hace ningun test del repositorio (#61). Antes esta nota decia que
 * era trabajo de {@code QuotePersistenceIT}, y no lo es: alli no hay
 * concurrencia. Dejarlo escrito asi hacia creer que la garantia estaba cubierta
 * en otro sitio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaQuoteNumberPort: la reserva del consecutivo")
class JpaQuoteNumberPortTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query upsert;
    @Mock
    private Query lectura;

    private JpaQuoteNumberPort port;

    @BeforeEach
    void crearPuerto() {
        port = new JpaQuoteNumberPort(entityManager);
    }

    /**
     * Encadena los dos mocks de Query en el orden en que el puerto los pide.
     *
     * @param numeroReservado
     *            lo que devuelve la lectura, que NO es {@code next_value} sino
     *            {@code next_value - 1}: la resta la hace el SQL, no Java. Que este
     *            matiz se me escapara al escribir el doble y lo cazara el test es
     *            justamente para lo que sirve.
     */
    private void conSecuencia(Object numeroReservado) {
        when(entityManager.createNativeQuery(anyString())).thenReturn(upsert, lectura);
        when(upsert.setParameter(eq("prefix"), anyString())).thenReturn(upsert);
        when(lectura.setParameter(eq("prefix"), anyString())).thenReturn(lectura);
        when(lectura.setMaxResults(1)).thenReturn(lectura);
        when(lectura.getResultList()).thenReturn(List.of(numeroReservado));
    }

    private String sqlDeLaReserva() {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager, org.mockito.Mockito.atLeastOnce()).createNativeQuery(sql.capture());
        return sql.getAllValues().getFirst();
    }

    @Nested
    @DisplayName("El ano sin fila: el caso que fallaba")
    class AnoSinFila {

        @Test
        @DisplayName("el primer numero del ano no lanza y es el 1")
        void el_primer_numero_del_ano_es_el_uno() {
            // La rama INSERT deja next_value = 2 y el SELECT devuelve next_value - 1.
            conSecuencia(1L);

            String numero = port.next(2027);

            assertThat(numero).isEqualTo("COT-2027-00001");
        }

        @Test
        @DisplayName("crea la fila del ano en la MISMA sentencia que la incrementa")
        void crea_e_incrementa_en_una_sola_sentencia() {
            conSecuencia(2L);

            port.next(2027);

            assertThat(sqlDeLaReserva()).contains("INSERT INTO billing_document_sequences")
                    .contains("ON DUPLICATE KEY UPDATE next_value = next_value + 1");
        }

        @Test
        @DisplayName("ninguna rama lee un maximo: no hay MAX ni ORDER BY en la reserva")
        void ninguna_rama_lee_un_maximo() {
            conSecuencia(2L);

            port.next(2027);

            assertThat(sqlDeLaReserva()).doesNotContainIgnoringCase("MAX(")
                    .doesNotContainIgnoringCase("ORDER BY");
        }

        @Test
        @DisplayName("reserva ANTES de leer: la lectura solo la ve quien ya tiene el candado")
        void reserva_antes_de_leer() {
            conSecuencia(2L);

            port.next(2027);

            InOrder orden = inOrder(upsert, lectura);
            orden.verify(upsert).executeUpdate();
            orden.verify(lectura).getResultList();
        }
    }

    @Nested
    @DisplayName("El ano ya empezado")
    class AnoYaEmpezado {

        @Test
        @DisplayName("continua la serie por donde iba, rellenando a cinco digitos")
        void continua_la_serie() {
            // La rama UPDATE deja next_value = 185 y el SELECT devuelve 184.
            conSecuencia(184L);

            assertThat(port.next(2026)).isEqualTo("COT-2026-00184");
        }

        @Test
        @DisplayName("la serie de cada ano es independiente: el prefijo lleva el ano dentro")
        void cada_ano_tiene_su_serie() {
            conSecuencia(2L);

            port.next(2031);

            ArgumentCaptor<String> prefijo = ArgumentCaptor.forClass(String.class);
            verify(upsert).setParameter(eq("prefix"), prefijo.capture());
            assertThat(prefijo.getValue()).isEqualTo("COT-2031").hasSizeLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("el ultimo numero de cinco cifras se pinta entero")
        void el_ultimo_de_cinco_cifras_se_pinta_entero() {
            conSecuencia(99999L);

            assertThat(port.next(2026)).isEqualTo("COT-2026-99999");
        }

        @Test
        @DisplayName("pasado el rango de cinco digitos ensancha, no trunca: truncar colisionaria")
        void pasado_el_rango_ensancha_en_vez_de_truncar() {
            conSecuencia(100000L);

            // Si el formato truncara, la cotizacion 100.000 se llamaria igual que la
            // 00000 y chocaria contra uq_quotes_number. Ensanchar cabe de sobra en el
            // VARCHAR(30) de quote_number.
            assertThat(port.next(2026)).isEqualTo("COT-2026-100000").hasSizeLessThan(30);
        }
    }

    @Nested
    @DisplayName("Corrupcion")
    class Corrupcion {

        @Test
        @DisplayName("si el contador desaparece tras reservarlo falla en vez de reiniciar la serie")
        void si_el_contador_desaparece_falla() {
            when(entityManager.createNativeQuery(anyString())).thenReturn(upsert, lectura);
            when(upsert.setParameter(eq("prefix"), anyString())).thenReturn(upsert);
            when(lectura.setParameter(eq("prefix"), anyString())).thenReturn(lectura);
            when(lectura.setMaxResults(1)).thenReturn(lectura);
            when(lectura.getResultList()).thenReturn(List.of());

            assertThatThrownBy(() -> port.next(2026)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Quote number sequence vanished");
        }
    }
}
