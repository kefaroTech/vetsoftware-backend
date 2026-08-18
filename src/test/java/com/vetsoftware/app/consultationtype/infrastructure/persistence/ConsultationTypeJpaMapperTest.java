package com.vetsoftware.app.consultationtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import com.vetsoftware.app.consultationtype.testsupport.ConsultationTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 */
@DisplayName("ConsultationTypeJpaMapper")
class ConsultationTypeJpaMapperTest {

    private final ConsultationTypeJpaMapper mapper = new ConsultationTypeJpaMapper();

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo en su columna")
        void copia_cada_campo_en_su_columna() {
            ConsultationType tipo = ConsultationTypeMother.consultaGeneral();

            ConsultationTypeJpaEntity entity = mapper.toJpa(tipo);

            assertThat(entity.getId()).isEqualTo(ConsultationTypeMother.ID);
            assertThat(entity.getName()).isEqualTo(ConsultationTypeMother.NOMBRE);
            assertThat(entity.getDescription()).isEqualTo(ConsultationTypeMother.DESCRIPCION);
            assertThat(entity.getCreatedDate()).isEqualTo(ConsultationTypeMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("conserva el estado deshabilitado")
        void conserva_el_estado_deshabilitado() {
            ConsultationTypeJpaEntity entity = mapper.toJpa(ConsultationTypeMother.deshabilitada());

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain — entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("reconstruye el agregado con cada campo")
        void reconstruye_el_agregado_con_cada_campo() {
            ConsultationTypeJpaEntity entity = new ConsultationTypeJpaEntity();
            entity.setId(ConsultationTypeMother.ID);
            entity.setName(ConsultationTypeMother.NOMBRE);
            entity.setDescription(ConsultationTypeMother.DESCRIPCION);
            entity.setCreatedDate(ConsultationTypeMother.CREADO);
            entity.setEnabled(true);

            ConsultationType tipo = mapper.toDomain(entity);

            assertThat(tipo.getId()).isEqualTo(ConsultationTypeMother.ID);
            assertThat(tipo.getName()).isEqualTo(ConsultationTypeMother.NOMBRE);
            assertThat(tipo.getDescription()).isEqualTo(ConsultationTypeMother.DESCRIPCION);
            assertThat(tipo.getCreatedDate()).isEqualTo(ConsultationTypeMother.CREADO);
            assertThat(tipo.isEnabled()).isTrue();
        }
    }

    @Test
    @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
    void la_ida_y_vuelta_no_pierde_nada() {
        ConsultationType original = ConsultationTypeMother.consultaGeneral();

        ConsultationTypeJpaEntity entity = mapper.toJpa(original);
        ConsultationType vuelta = mapper.toDomain(entity);

        assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
    }
}
