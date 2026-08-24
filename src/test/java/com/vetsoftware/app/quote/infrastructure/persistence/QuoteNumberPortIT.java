package com.vetsoftware.app.quote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Rodaja del consecutivo {@code COT-AAAA-NNNNN} contra MySQL real.
 *
 * <p>
 * <b>Por qué hace falta además del test unitario.</b>
 * {@code JpaQuoteNumberPortTest} prueba la misma clase con un doble del
 * {@code EntityManager}: verifica <em>qué SQL se emite</em> —que no hay
 * {@code MAX}, que la reserva va antes de la lectura— y eso es exactamente lo
 * que un doble puede probar. Lo que no puede es ejecutar
 * {@code INSERT … ON DUPLICATE KEY UPDATE}: que las dos ramas existan, que la
 * de inserción deje {@code next_value = 2} y la de incremento sume uno, y que
 * el {@code next_value - 1} de vuelta sea el número reservado. Eso solo lo dice
 * el motor.
 *
 * <p>
 * <b>El caso que hundió a la versión anterior</b> es el primero de aquí: el
 * primer número del año, cuando <em>no hay fila que bloquear</em>. La
 * implementación vieja leía el último número con {@code SELECT … FOR UPDATE} y
 * le sumaba uno; el 1 de enero esa fila no existe, el bloqueo no bloquea nada y
 * dos comerciales cotizando a la vez se llevan el mismo {@code COT-2027-00001}.
 *
 * <p>
 * <b>Se construye a mano</b> y no como bean: solo necesita un
 * {@code EntityManager}, y añadirlo al {@code @Import} costaría un arranque de
 * contexto entero.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaQuoteNumberPort — la reserva del consecutivo contra MySQL real")
class QuoteNumberPortIT extends AbstractDataJpaTest {

    /** Años sin fila en el contador: nadie ha cotizado todavía en ellos. */
    private static final int ANO_VIRGEN = 2099;
    private static final int OTRO_ANO_VIRGEN = 2098;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaQuoteNumberPort port;

    @BeforeEach
    void construirElPuerto() {
        port = new JpaQuoteNumberPort(entityManager);
    }

    /** El {@code next_value} que guarda la tabla para ese prefijo, o vacío. */
    private List<?> contador(String prefijo) {
        return entityManager
                .createNativeQuery(
                        "SELECT next_value FROM billing_document_sequences WHERE prefix = :p")
                .setParameter("p", prefijo).getResultList();
    }

    private long valorDelContador(String prefijo) {
        return ((Number) contador(prefijo).getFirst()).longValue();
    }

    @Nested
    @DisplayName("El año sin fila: el caso que fallaba")
    class AnoSinFila {

        @Test
        @DisplayName("el primer número del año es el 1 y no necesita que la fila exista antes")
        void el_primer_numero_del_ano_es_el_uno() {
            assertThat(contador("COT-" + ANO_VIRGEN)).isEmpty();

            assertThat(port.next(ANO_VIRGEN)).isEqualTo("COT-2099-00001");
        }

        @Test
        @DisplayName("la rama de inserción deja el contador en 2: el 1 ya está entregado")
        void la_rama_de_insercion_deja_el_contador_en_dos() {
            port.next(ANO_VIRGEN);

            assertThat(valorDelContador("COT-" + ANO_VIRGEN)).isEqualTo(2L);
        }

        @Test
        @DisplayName("la segunda petición del mismo año entra por la otra rama y da el 2")
        void la_segunda_peticion_entra_por_la_otra_rama() {
            // Las dos ramas del ON DUPLICATE KEY UPDATE, una detras de otra: la primera
            // inserta, la segunda incrementa. Ninguna lee un maximo.
            assertThat(port.next(ANO_VIRGEN)).isEqualTo("COT-2099-00001");
            assertThat(port.next(ANO_VIRGEN)).isEqualTo("COT-2099-00002");
            assertThat(valorDelContador("COT-" + ANO_VIRGEN)).isEqualTo(3L);
        }

        @Test
        @DisplayName("cada año arranca su propia serie desde 1, no continúa la del anterior")
        void cada_ano_arranca_su_propia_serie() {
            port.next(ANO_VIRGEN);
            port.next(ANO_VIRGEN);

            // El prefijo lleva el ano dentro, asi que son dos filas distintas y dos
            // candados distintos. Si la serie fuera global, cotizar en enero heredaria el
            // numero de diciembre y el consecutivo dejaria de ser anual.
            assertThat(port.next(OTRO_ANO_VIRGEN)).isEqualTo("COT-2098-00001");
            assertThat(valorDelContador("COT-" + ANO_VIRGEN)).isEqualTo(3L);
            assertThat(valorDelContador("COT-" + OTRO_ANO_VIRGEN)).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("El año ya empezado")
    class AnoYaEmpezado {

        @Test
        @DisplayName("continúa la serie por donde iba, rellenando a cinco dígitos")
        void continua_la_serie_rellenando_a_cinco_digitos() {
            entityManager.createNativeQuery("""
                    INSERT INTO billing_document_sequences (prefix, next_value, created_date)
                    VALUES (:p, 184, '2026-01-01 00:00:00')
                    ON DUPLICATE KEY UPDATE next_value = next_value
                    """).setParameter("p", "COT-" + ANO_VIRGEN).executeUpdate();

            assertThat(port.next(ANO_VIRGEN)).isEqualTo("COT-2099-00184");
            assertThat(valorDelContador("COT-" + ANO_VIRGEN)).isEqualTo(185L);
        }

        @Test
        @DisplayName("pasado el rango de cinco dígitos ensancha en vez de truncar")
        void pasado_el_rango_ensancha_en_vez_de_truncar() {
            // Truncar a cinco digitos colisionaria con un numero ya entregado, y el
            // numero de una cotizacion se cita en soporte y en cobranza.
            entityManager.createNativeQuery("""
                    INSERT INTO billing_document_sequences (prefix, next_value, created_date)
                    VALUES (:p, 100000, '2026-01-01 00:00:00')
                    ON DUPLICATE KEY UPDATE next_value = next_value
                    """).setParameter("p", "COT-" + ANO_VIRGEN).executeUpdate();

            assertThat(port.next(ANO_VIRGEN)).isEqualTo("COT-2099-100000");
        }

        @Test
        @DisplayName("el prefijo se guarda entero: ni truncado ni rellenado por la columna")
        void el_prefijo_se_guarda_entero() {
            // prefix es VARCHAR(10) y 'COT-' mas el ano son 8. Si la serie se alargara,
            // MySQL truncaria o fallaria segun el sql_mode, y dos anos distintos podrian
            // acabar compartiendo fila y por tanto consecutivo. Se lee de vuelta el
            // prefijo real en vez de darlo por bueno.
            port.next(ANO_VIRGEN);

            Object guardado = entityManager
                    .createNativeQuery(
                            "SELECT prefix FROM billing_document_sequences WHERE next_value = 2")
                    .setMaxResults(1).getSingleResult();

            assertThat(String.valueOf(guardado)).isEqualTo("COT-2099");
        }
    }
}
