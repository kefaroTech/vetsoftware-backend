package com.vetsoftware.app.branch.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.testsupport.BranchMother;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el único punto que conoce dominio y entidad JPA a la vez.
 *
 * <p>
 * {@code CityJpaEntity} y {@code CompanyJpaEntity} se mockean porque su
 * constructor sin argumentos es {@code protected} y no son instanciables desde
 * este paquete; no tienen lógica propia, son portadores de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BranchJpaMapper")
class BranchJpaMapperTest {

    private final BranchJpaMapper mapper = new BranchJpaMapper();

    @Mock
    private CityJpaEntity cityEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar y engancha ciudad y empresa")
        void copia_cada_campo_y_engancha_las_asociaciones() {
            Branch branch = BranchMother.sedeActiva();

            BranchJpaEntity entity = mapper.toJpa(branch, cityEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(BranchMother.BRANCH_ID);
            assertThat(entity.getName()).isEqualTo(branch.getName());
            assertThat(entity.getCode()).isEqualTo(branch.getCode());
            assertThat(entity.getAddress()).isEqualTo(branch.getAddress());
            assertThat(entity.getPhone()).isEqualTo(branch.getPhone());
            assertThat(entity.getCity()).isSameAs(cityEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getCreatedDate()).isEqualTo(branch.getCreatedDate());
            assertThat(entity.isActive()).isTrue();
        }

        @Test
        @DisplayName("una sucursal sin dirección ni teléfono deja esas columnas en null")
        void permite_direccion_y_telefono_nulos() {
            BranchJpaEntity entity = mapper.toJpa(BranchMother.sinDireccionNiTelefono(), cityEntity,
                    companyEntity);

            assertThat(entity.getAddress()).isNull();
            assertThat(entity.getPhone()).isNull();
        }

        @Test
        @DisplayName("una sucursal inactiva se persiste con active en false")
        void una_sucursal_inactiva_se_persiste_con_active_false() {
            BranchJpaEntity entity = mapper.toJpa(BranchMother.sedeInactiva(), cityEntity,
                    companyEntity);

            assertThat(entity.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            BranchJpaEntity entity = mapper.toJpa(BranchMother.sedeActiva(), cityEntity,
                    companyEntity);

            Branch branch = mapper.toDomain(entity, BranchMother.BOGOTA, BranchMother.CLINICA);

            assertThat(branch.getId()).isEqualTo(BranchMother.BRANCH_ID);
            assertThat(branch.getName()).isEqualTo("Sede Norte");
            assertThat(branch.getCode()).isEqualTo("NORTE");
            assertThat(branch.getCity()).isEqualTo(BranchMother.BOGOTA);
            assertThat(branch.getCompany()).isEqualTo(BranchMother.CLINICA);
            assertThat(branch.isActive()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio → entidad → dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            Branch original = BranchMother.sedeActiva();

            BranchJpaEntity entity = mapper.toJpa(original, cityEntity, companyEntity);
            Branch vuelta = mapper.toDomain(entity, original.getCity(), original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociación")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(cityEntity.getId()).thenReturn(BranchMother.BOGOTA.id());
            when(cityEntity.getName()).thenReturn(BranchMother.BOGOTA.name());
            when(companyEntity.getId()).thenReturn(BranchMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(BranchMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(BranchMother.CLINICA.identifier());

            BranchJpaEntity entity = mapper.toJpa(BranchMother.sedeActiva(), cityEntity,
                    companyEntity);

            Branch branch = mapper.toDomain(entity);

            assertThat(branch.getCity()).isEqualTo(BranchMother.BOGOTA);
            assertThat(branch.getCompany()).isEqualTo(BranchMother.CLINICA);
        }
    }
}
