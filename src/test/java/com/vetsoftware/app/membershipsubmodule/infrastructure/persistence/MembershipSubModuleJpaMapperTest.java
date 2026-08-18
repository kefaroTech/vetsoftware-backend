package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.membershipsubmodule.testsupport.MembershipSubModuleMother;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 *
 * <p>
 * {@code MembershipJpaEntity} y {@code SubModuleJpaEntity} se mockean porque
 * sus constructores sin argumentos son {@code protected} y no son instanciables
 * desde este paquete. No tienen logica: son portadores de datos, y mockearlos
 * no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipSubModuleJpaMapper")
class MembershipSubModuleJpaMapperTest {

    private final MembershipSubModuleJpaMapper mapper = new MembershipSubModuleJpaMapper();

    @Mock
    private MembershipJpaEntity membershipEntity;
    @Mock
    private SubModuleJpaEntity subModuleEntity;

    private MembershipSubModuleJpaEntity entidadCompleta() {
        MembershipSubModuleJpaEntity entity = new MembershipSubModuleJpaEntity();
        entity.setId(MembershipSubModuleMother.RELATION_ID);
        entity.setCreatedDate(MembershipSubModuleMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            MembershipSubModule relacion = MembershipSubModuleMother.activa();

            MembershipSubModuleJpaEntity entity = mapper.toJpa(relacion, membershipEntity,
                    subModuleEntity);

            assertThat(entity.getId()).isEqualTo(MembershipSubModuleMother.RELATION_ID);
            assertThat(entity.getCreatedDate()).isEqualTo(MembershipSubModuleMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha membership y subModule en su slot")
        void engancha_membership_y_sub_module_en_su_slot() {
            MembershipSubModuleJpaEntity entity = mapper.toJpa(MembershipSubModuleMother.activa(),
                    membershipEntity, subModuleEntity);

            assertThat(entity.getMembership()).isSameAs(membershipEntity);
            assertThat(entity.getSubModule()).isSameAs(subModuleEntity);
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            // Este overload existe para no inicializar el proxy de getReferenceById: si
            // leyera entity.getMembership()/getSubModule(), Hibernate lanzaria un SELECT
            // extra por save.
            MembershipSubModule relacion = mapper.toDomain(entidadCompleta(),
                    MembershipSubModuleMother.PLAN_PREMIUM, MembershipSubModuleMother.FACTURACION);

            assertThat(relacion.getId()).isEqualTo(MembershipSubModuleMother.RELATION_ID);
            assertThat(relacion.getMembership()).isEqualTo(MembershipSubModuleMother.PLAN_PREMIUM);
            assertThat(relacion.getSubModule()).isEqualTo(MembershipSubModuleMother.FACTURACION);
            assertThat(relacion.getCreatedDate()).isEqualTo(MembershipSubModuleMother.CREADO);
            assertThat(relacion.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            MembershipSubModule original = MembershipSubModuleMother.activa();

            MembershipSubModuleJpaEntity entity = mapper.toJpa(original, membershipEntity,
                    subModuleEntity);
            MembershipSubModule vuelta = mapper.toDomain(entity, original.getMembership(),
                    original.getSubModule());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye los companion VO desde las asociaciones hidratadas")
        void construye_los_companion_vo_desde_las_asociaciones() {
            when(membershipEntity.getId()).thenReturn(MembershipSubModuleMother.MEMBERSHIP_ID);
            when(membershipEntity.getName())
                    .thenReturn(MembershipSubModuleMother.PLAN_PREMIUM.name());
            when(subModuleEntity.getId()).thenReturn(MembershipSubModuleMother.SUB_MODULE_ID);
            when(subModuleEntity.getName())
                    .thenReturn(MembershipSubModuleMother.FACTURACION.name());
            when(subModuleEntity.getCode())
                    .thenReturn(MembershipSubModuleMother.FACTURACION.code());
            MembershipSubModuleJpaEntity entity = entidadCompleta();
            entity.setMembership(membershipEntity);
            entity.setSubModule(subModuleEntity);

            MembershipSubModule relacion = mapper.toDomain(entity);

            assertThat(relacion.getMembership()).isEqualTo(MembershipSubModuleMother.PLAN_PREMIUM);
            assertThat(relacion.getSubModule()).isEqualTo(MembershipSubModuleMother.FACTURACION);
        }
    }
}
