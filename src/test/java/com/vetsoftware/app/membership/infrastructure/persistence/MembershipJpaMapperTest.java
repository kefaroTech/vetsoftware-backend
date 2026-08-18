package com.vetsoftware.app.membership.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import com.vetsoftware.app.membership.testsupport.MembershipMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 */
@DisplayName("MembershipJpaMapper")
class MembershipJpaMapperTest {

    private final MembershipJpaMapper mapper = new MembershipJpaMapper();

    private MembershipJpaEntity entidadCompleta() {
        MembershipJpaEntity entity = new MembershipJpaEntity();
        entity.setId(MembershipMother.MEMBERSHIP_ID);
        entity.setName("Plan Oro");
        entity.setStatus(MembershipStatus.ACTIVE.name());
        entity.setMandatory(Boolean.FALSE);
        entity.setCreatedDate(MembershipMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            Membership membership = MembershipMother.activa();

            MembershipJpaEntity entity = mapper.toJpa(membership);

            assertThat(entity.getId()).isEqualTo(MembershipMother.MEMBERSHIP_ID);
            assertThat(entity.getName()).isEqualTo("Plan Oro");
            assertThat(entity.getStatus()).isEqualTo("ACTIVE");
            assertThat(entity.getMandatory()).isFalse();
            assertThat(entity.getCreatedDate()).isEqualTo(MembershipMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("el estado se serializa con el nombre del enum")
        void el_estado_se_serializa_con_el_nombre_del_enum() {
            MembershipJpaEntity entity = mapper
                    .toJpa(MembershipMother.conEstado(MembershipStatus.DEPRECATED));

            assertThat(entity.getStatus()).isEqualTo("DEPRECATED");
        }
    }

    @Nested
    @DisplayName("toDomain — entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("reconstruye el agregado con el estado deserializado")
        void reconstruye_el_agregado_con_el_estado_deserializado() {
            Membership membership = mapper.toDomain(entidadCompleta());

            assertThat(membership.getId()).isEqualTo(MembershipMother.MEMBERSHIP_ID);
            assertThat(membership.getName()).isEqualTo("Plan Oro");
            assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
            assertThat(membership.isMandatory()).isFalse();
            assertThat(membership.getCreatedDate()).isEqualTo(MembershipMother.CREADO);
            assertThat(membership.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un mandatory nulo en la columna se lee como false, no NPE")
        void un_mandatory_nulo_en_la_columna_se_lee_como_false() {
            MembershipJpaEntity entity = entidadCompleta();
            entity.setMandatory(null);

            Membership membership = mapper.toDomain(entity);

            assertThat(membership.isMandatory()).isFalse();
        }

        @Test
        @DisplayName("la ida y vuelta dominio → entidad → dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            Membership original = MembershipMother.obligatoria();

            MembershipJpaEntity entity = mapper.toJpa(original);
            Membership vuelta = mapper.toDomain(entity);

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getName()).isEqualTo(original.getName());
            assertThat(vuelta.getStatus()).isEqualTo(original.getStatus());
            assertThat(vuelta.isMandatory()).isEqualTo(original.isMandatory());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }
    }
}
