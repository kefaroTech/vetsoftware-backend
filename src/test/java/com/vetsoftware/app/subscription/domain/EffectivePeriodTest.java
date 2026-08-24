package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La definicion de «vigente» esta escrita en un solo sitio, y este es el test
 * que la fija. Si alguien la reinterpreta, aqui se entera.
 */
@DisplayName("EffectivePeriod - la definicion de vigente")
class EffectivePeriodTest {

    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JUNIO_30 = LocalDate.of(2026, 6, 30);
    private static final LocalDate MAYO_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate DICIEMBRE_31 = LocalDate.of(2026, 12, 31);

    @Nested
    @DisplayName("Vigencia")
    class Vigencia {

        @Test
        @DisplayName("vigente NO es simplemente no tener fecha de fin")
        void vigenteNoEsNoTenerFechaDeFin() {
            EffectivePeriod futura = EffectivePeriod.openFrom(LocalDate.of(2026, 9, 1));

            // Sin fecha de fin, pero todavia no ha empezado: NO esta vigente hoy.
            assertThat(futura.isOpen()).isTrue();
            assertThat(futura.isCurrentOn(ENERO_1)).isFalse();
        }

        @Test
        @DisplayName("ya empezo y todavia no ha terminado")
        void yaEmpezoYNoHaTerminado() {
            EffectivePeriod primerSemestre = new EffectivePeriod(ENERO_1, JUNIO_30);

            assertThat(primerSemestre.isCurrentOn(LocalDate.of(2026, 3, 15))).isTrue();
        }

        @Test
        @DisplayName("el dia de inicio esta cubierto: el criterio es effective_from <= dia")
        void elDiaDeInicioEstaCubierto() {
            assertThat(new EffectivePeriod(ENERO_1, JUNIO_30).isCurrentOn(ENERO_1)).isTrue();
        }

        @Test
        @DisplayName("el dia de fin NO esta cubierto: el criterio es effective_to > dia")
        void elDiaDeFinNoEstaCubierto() {
            // Semiabierto. Es lo que hace que la linea que cierra el 30 y la que abre el
            // 30 no se pisen ni dejen hueco: exactamente un dia, exactamente una linea.
            assertThat(new EffectivePeriod(ENERO_1, JUNIO_30).isCurrentOn(JUNIO_30)).isFalse();
        }

        @Test
        @DisplayName("una linea abierta sigue vigente indefinidamente")
        void lineaAbiertaSigueVigente() {
            EffectivePeriod abierta = EffectivePeriod.openFrom(ENERO_1);

            assertThat(abierta.isCurrentOn(LocalDate.of(2099, 1, 1))).isTrue();
            assertThat(abierta.endExclusive()).isEqualTo(EffectivePeriod.OPEN_ENDED);
        }

        @Test
        @DisplayName("antes de empezar no esta vigente")
        void antesDeEmpezarNoEstaVigente() {
            assertThat(new EffectivePeriod(MAYO_1, DICIEMBRE_31).isCurrentOn(ENERO_1)).isFalse();
        }
    }

    @Nested
    @DisplayName("Solape")
    class Solape {

        @Test
        @DisplayName("dos tramos con fechas de fin futuras que se pisan SI se detectan")
        void dosTramosConFinFuturoQueSePisan() {
            // Este es el caso que el indice unico sobre current_item_marker NO puede
            // impedir: las dos lineas tienen effective_to, las dos dan marcador nulo y
            // MySQL las acepta. En mayo y junio el modulo se factura dos veces.
            EffectivePeriod a = new EffectivePeriod(ENERO_1, JUNIO_30);
            EffectivePeriod b = new EffectivePeriod(MAYO_1, DICIEMBRE_31);

            assertThat(a.overlaps(b)).isTrue();
            assertThat(b.overlaps(a)).isTrue();
        }

        @Test
        @DisplayName("tramos consecutivos no se pisan: el fin de uno es el inicio del otro")
        void tramosConsecutivosNoSePisan() {
            EffectivePeriod primero = new EffectivePeriod(ENERO_1, JUNIO_30);
            EffectivePeriod segundo = EffectivePeriod.openFrom(JUNIO_30);

            assertThat(primero.overlaps(segundo)).isFalse();
            assertThat(segundo.overlaps(primero)).isFalse();
        }

        @Test
        @DisplayName("dos lineas abiertas del mismo articulo siempre se pisan")
        void dosLineasAbiertasSePisan() {
            assertThat(EffectivePeriod.openFrom(ENERO_1)
                    .overlaps(EffectivePeriod.openFrom(DICIEMBRE_31))).isTrue();
        }

        @Test
        @DisplayName("un tramo contenido dentro de otro se pisa")
        void tramoContenido() {
            EffectivePeriod grande = new EffectivePeriod(ENERO_1, DICIEMBRE_31);
            EffectivePeriod pequeno = new EffectivePeriod(MAYO_1, JUNIO_30);

            assertThat(grande.overlaps(pequeno)).isTrue();
        }

        @Test
        @DisplayName("tramos disjuntos no se pisan")
        void tramosDisjuntos() {
            EffectivePeriod primero = new EffectivePeriod(ENERO_1, LocalDate.of(2026, 3, 1));
            EffectivePeriod segundo = new EffectivePeriod(MAYO_1, DICIEMBRE_31);

            assertThat(primero.overlaps(segundo)).isFalse();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin fecha de inicio no hay vigencia posible")
        void sinFechaDeInicio() {
            assertThatThrownBy(() -> new EffectivePeriod(null, JUNIO_30))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("effectiveFrom");
        }

        @Test
        @DisplayName("la fecha de fin no puede ser anterior a la de inicio")
        void finAnteriorAInicio() {
            assertThatThrownBy(() -> new EffectivePeriod(JUNIO_30, ENERO_1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("effectiveTo");
        }

        @Test
        @DisplayName("endingOn conserva el inicio y escribe el fin")
        void endingOnConservaElInicio() {
            EffectivePeriod cerrada = EffectivePeriod.openFrom(ENERO_1).endingOn(JUNIO_30);

            assertThat(cerrada.from()).isEqualTo(ENERO_1);
            assertThat(cerrada.to()).isEqualTo(JUNIO_30);
            assertThat(cerrada.isOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("Contrato de los argumentos")
    class ContratoDeLosArgumentos {

        @Test
        @DisplayName("preguntar por un dia nulo falla en vez de responder que no")
        void diaNulo() {
            // Responder false seria mucho peor que fallar: una linea vigente pasaria
            // por no vigente y el recalculo de permisos le cerraria el modulo a un
            // cliente que lo tiene pagado, sin que nada quedara registrado.
            assertThatThrownBy(() -> EffectivePeriod.openFrom(ENERO_1).isCurrentOn(null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("day");
        }

        @Test
        @DisplayName("comparar contra un tramo nulo falla en vez de responder que no se pisan")
        void tramoNulo() {
            // Y aqui responder false dejaria pasar el alta que duplica la facturacion.
            assertThatThrownBy(() -> EffectivePeriod.openFrom(ENERO_1).overlaps(null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("other");
        }
    }
}
