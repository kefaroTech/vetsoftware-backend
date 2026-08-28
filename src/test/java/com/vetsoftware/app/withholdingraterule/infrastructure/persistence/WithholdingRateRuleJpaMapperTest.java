package com.vetsoftware.app.withholdingraterule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import com.vetsoftware.app.withholdingraterule.testsupport.WithholdingRateRuleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WithholdingRateRuleJpaMapper — dominio y fila, en los dos sentidos")
class WithholdingRateRuleJpaMapperTest {

    private final WithholdingRateRuleJpaMapper mapper = new WithholdingRateRuleJpaMapper();

    @Nested
    @DisplayName("Hacia la fila")
    class HaciaLaFila {

        @Test
        @DisplayName("copia los trece campos y no cruza las dos bases minimas")
        void copia_los_trece_campos_y_no_cruza_las_bases() {
            WithholdingRateRuleJpaEntity fila = mapper.toJpa(WithholdingRateRuleMother.ica());

            assertThat(fila.getId()).isEqualTo(8302L);
            assertThat(fila.getWithholdingType()).isEqualTo(WithholdingType.ICA);
            assertThat(fila.getServiceNature()).isEqualTo(ServiceNature.CONSULTING);
            assertThat(fila.getMunicipalityCode()).isEqualTo("11001");
            assertThat(fila.getRatePercent()).isEqualByComparingTo("0.69");
            assertThat(fila.getMinimumBaseAmount()).isEqualByComparingTo("213010.00");
            assertThat(fila.getMinimumBaseUvt()).isEqualByComparingTo("4.00");
            assertThat(fila.getLegalReference()).isEqualTo("Acuerdo 65 de 2002");
            assertThat(fila.getValidFrom()).isEqualTo(WithholdingRateRuleMother.DESDE);
            assertThat(fila.getValidTo()).isNull();
            assertThat(fila.getCreatedDate()).isEqualTo(WithholdingRateRuleMother.CREADA_EL);
            assertThat(fila.isEnabled()).isTrue();
            assertThat(fila.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("copia la version, sin la cual el cierre seria un insert y no una edicion")
        void copia_la_version() {
            // Con la version en nulo sobre una entidad que ya tiene id, Hibernate
            // la tomaria por transitoria y el merge escribiria una fila nueva: dos
            // vigencias para el mismo supuesto, que es justo lo que las columnas
            // generadas del changeset 317 existen para impedir.
            WithholdingRateRuleJpaEntity fila = mapper.toJpa(WithholdingRateRuleMother.cerrada());

            assertThat(fila.getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("una tarifa recien creada llega sin id y sin version")
        void una_tarifa_recien_creada_llega_sin_id_ni_version() {
            WithholdingRateRuleJpaEntity fila = mapper.toJpa(WithholdingRateRuleMother.nueva());

            assertThat(fila.getId()).isNull();
            assertThat(fila.getVersion()).isNull();
        }

        @Test
        @DisplayName("una retencion nacional deja el municipio en nulo, no en el centinela")
        void una_nacional_deja_el_municipio_en_nulo() {
            // El centinela es cosa de la columna generada municipality_key, que no
            // esta mapeada. Escribirlo en municipality_code romperia la clave
            // foranea contra cities.dane_code.
            WithholdingRateRuleJpaEntity fila = mapper.toJpa(WithholdingRateRuleMother.nacional());

            assertThat(fila.getMunicipalityCode()).isNull();
        }
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("el viaje completo no pierde ni un decimal de la tarifa")
        void el_viaje_completo_no_pierde_ni_un_decimal() {
            WithholdingRateRule original = WithholdingRateRuleMother.ica();

            WithholdingRateRule vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta.getRatePercent()).isEqualTo(original.getRatePercent());
            assertThat(vuelta.getRatePercent().scale()).isEqualTo(6);
        }

        @Test
        @DisplayName("una regla cerrada vuelve cerrada y con su fecha de fin")
        void una_regla_cerrada_vuelve_cerrada() {
            WithholdingRateRule vuelta = mapper
                    .toDomain(mapper.toJpa(WithholdingRateRuleMother.cerrada()));

            assertThat(vuelta.isOpen()).isFalse();
            assertThat(vuelta.getValidTo()).isEqualTo(WithholdingRateRuleMother.HASTA);
            assertThat(vuelta.getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("una regla nacional vuelve sin municipio y con su clave de centinela")
        void una_regla_nacional_vuelve_sin_municipio() {
            WithholdingRateRule vuelta = mapper
                    .toDomain(mapper.toJpa(WithholdingRateRuleMother.nacional()));

            assertThat(vuelta.getMunicipalityCode()).isNull();
            assertThat(vuelta.municipalityKey()).isEqualTo("-");
        }
    }
}
