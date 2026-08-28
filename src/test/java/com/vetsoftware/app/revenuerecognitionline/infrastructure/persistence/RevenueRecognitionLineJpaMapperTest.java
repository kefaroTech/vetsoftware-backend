package com.vetsoftware.app.revenuerecognitionline.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLine;
import com.vetsoftware.app.revenuerecognitionline.testsupport.RevenueRecognitionLineMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa.
 *
 * <p>
 * <strong>No hay ninguna columna generada que vigilar en este mapper</strong>:
 * a diferencia de {@code AccountingExportJpaEntity}, esta entidad no declara
 * {@code current_export_marker} ni ningun otro campo {@code GENERATED ALWAYS} —
 * se comprobo leyendo {@code RevenueRecognitionLineJpaEntity} entera, que solo
 * tiene las ocho columnas escalares que {@code toJpa}/{@code toDomain} ya
 * cubren abajo.
 */
@DisplayName("RevenueRecognitionLineJpaMapper")
class RevenueRecognitionLineJpaMapperTest {

    private final RevenueRecognitionLineJpaMapper mapper = new RevenueRecognitionLineJpaMapper();

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            RevenueRecognitionLine line = RevenueRecognitionLineMother.renglon();

            RevenueRecognitionLineJpaEntity entity = mapper.toJpa(line);

            assertThat(entity.getId()).isEqualTo(RevenueRecognitionLineMother.LINE_ID);
            assertThat(entity.getCompanyId()).isEqualTo(RevenueRecognitionLineMother.COMPANY_ID);
            assertThat(entity.getChargeId()).isEqualTo(RevenueRecognitionLineMother.CHARGE_ID);
            assertThat(entity.getPeriodKey()).isEqualTo(RevenueRecognitionLineMother.PERIOD_KEY);
            assertThat(entity.getPostingPeriod())
                    .isEqualTo(RevenueRecognitionLineMother.POSTING_PERIOD);
            assertThat(entity.getRecognizedAmount())
                    .isEqualByComparingTo(RevenueRecognitionLineMother.RECOGNIZED_AMOUNT);
            assertThat(entity.getMethod()).isEqualTo(RevenueRecognitionLineMother.METHOD);
            assertThat(entity.getCreatedDate()).isEqualTo(RevenueRecognitionLineMother.CREADO);
        }

        @Test
        @DisplayName("un renglon sin id, listo para insertar, mapea a una entidad con id nulo")
        void un_renglon_sin_id_mapea_a_entidad_con_id_nulo() {
            RevenueRecognitionLine nuevo = RevenueRecognitionLineMother.renglon(null);

            RevenueRecognitionLineJpaEntity entity = mapper.toJpa(nuevo);

            assertThat(entity.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain — entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("reconstruye el agregado completo desde la entidad")
        void reconstruye_el_agregado_completo_desde_la_entidad() {
            RevenueRecognitionLineJpaEntity entity = mapper
                    .toJpa(RevenueRecognitionLineMother.renglon());

            RevenueRecognitionLine reconstruido = mapper.toDomain(entity);

            assertThat(reconstruido.getId()).isEqualTo(RevenueRecognitionLineMother.LINE_ID);
            assertThat(reconstruido.getCompanyId())
                    .isEqualTo(RevenueRecognitionLineMother.COMPANY_ID);
            assertThat(reconstruido.getChargeId())
                    .isEqualTo(RevenueRecognitionLineMother.CHARGE_ID);
            assertThat(reconstruido.getPeriodKey())
                    .isEqualTo(RevenueRecognitionLineMother.PERIOD_KEY);
            assertThat(reconstruido.getPostingPeriod())
                    .isEqualTo(RevenueRecognitionLineMother.POSTING_PERIOD);
            assertThat(reconstruido.getRecognizedAmount())
                    .isEqualByComparingTo(RevenueRecognitionLineMother.RECOGNIZED_AMOUNT);
            assertThat(reconstruido.getMethod()).isEqualTo(RevenueRecognitionLineMother.METHOD);
            assertThat(reconstruido.getCreatedDate())
                    .isEqualTo(RevenueRecognitionLineMother.CREADO);
        }

        @Test
        @DisplayName("ida y vuelta conserva el signo del importe de una fila que compensa")
        void ida_y_vuelta_conserva_el_signo_del_importe_de_una_fila_que_compensa() {
            RevenueRecognitionLine compensacion = RevenueRecognitionLineMother.compensacion();

            RevenueRecognitionLine reconstruido = mapper.toDomain(mapper.toJpa(compensacion));

            assertThat(reconstruido.isOffset()).isTrue();
            assertThat(reconstruido.getRecognizedAmount())
                    .isEqualByComparingTo(compensacion.getRecognizedAmount());
        }
    }
}
