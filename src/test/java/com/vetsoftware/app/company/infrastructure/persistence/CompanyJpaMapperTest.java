package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.MembershipRef;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.company.testsupport.ReflectionEntities;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyJpaMapper — ida y vuelta dominio <-> entidad")
class CompanyJpaMapperTest {

    private final CompanyJpaMapper mapper = new CompanyJpaMapper();

    private static CityJpaEntity ciudad(Long id, String nombre)
            throws ReflectiveOperationException {
        CityJpaEntity entity = ReflectionEntities.newInstance(CityJpaEntity.class);
        entity.setId(id);
        entity.setName(nombre);
        return entity;
    }

    private static MembershipJpaEntity membresia(Long id, String nombre, String estado)
            throws ReflectiveOperationException {
        MembershipJpaEntity entity = ReflectionEntities.newInstance(MembershipJpaEntity.class);
        entity.setId(id);
        entity.setName(nombre);
        entity.setStatus(estado);
        return entity;
    }

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo del dominio y fija las entidades de ciudad y membresia dadas")
        void copia_cada_campo() throws ReflectiveOperationException {
            Company company = CompanyMother.clinicaNorte();
            CityJpaEntity city = ciudad(11L, "Bogota");
            MembershipJpaEntity membership = membresia(21L, "Premium", "ACTIVE");

            CompanyJpaEntity entity = mapper.toJpa(company, city, membership);

            assertThat(entity.getId()).isEqualTo(company.getId());
            assertThat(entity.getName()).isEqualTo("Clinica Norte");
            assertThat(entity.getIdentifier()).isEqualTo("NIT-900");
            assertThat(entity.getAddress()).isEqualTo("Calle 123 #45-67");
            assertThat(entity.getContactNumber()).isEqualTo("3001234567");
            assertThat(entity.getCity()).isSameAs(city);
            assertThat(entity.getMembership()).isSameAs(membership);
            assertThat(entity.getCreatedDate()).isEqualTo(CompanyMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("una empresa deshabilitada mapea el flag habilitado en falso")
        void empresa_deshabilitada_mapea_el_flag_en_falso() throws ReflectiveOperationException {
            Company company = CompanyMother.deshabilitada();

            CompanyJpaEntity entity = mapper.toJpa(company, ciudad(11L, "Bogota"),
                    membresia(21L, "Premium", "ACTIVE"));

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain(entity) — arma los companion VO desde las relaciones cargadas")
    class ToDomainDesdeLaEntidad {

        @Test
        @DisplayName("construye los companion VO de ciudad y membresia leyendo las relaciones de la entidad")
        void construye_los_companion_vo_desde_las_relaciones() throws ReflectiveOperationException {
            CompanyJpaEntity entity = new CompanyJpaEntity();
            entity.setId(9L);
            entity.setName("Clinica Norte");
            entity.setIdentifier("NIT-900");
            entity.setAddress("Calle 123 #45-67");
            entity.setContactNumber("3001234567");
            entity.setCity(ciudad(11L, "Bogota"));
            entity.setMembership(membresia(21L, "Premium", "ACTIVE"));
            entity.setCreatedDate(LocalDateTime.of(2026, 1, 15, 10, 30));
            entity.setEnabled(true);

            Company company = mapper.toDomain(entity);

            assertThat(company.getCity()).isEqualTo(new CityRef(11L, "Bogota"));
            assertThat(company.getMembership())
                    .isEqualTo(new MembershipRef(21L, "Premium", "ACTIVE"));
            assertThat(company.getName()).isEqualTo("Clinica Norte");
            assertThat(company.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("toDomain(entity, cityRef, membershipRef) — reusa los companion VO precargados")
    class ToDomainConRefsPrecargados {

        @Test
        @DisplayName("usa los refs recibidos sin tocar las relaciones de la entidad")
        void usa_los_refs_recibidos_sin_tocar_las_relaciones() {
            // La entidad NO trae ciudad ni membresia cargadas (como pasa justo despues de
            // un save con getReferenceById): si este overload leyera entity.getCity() o
            // entity.getMembership() en vez de los refs recibidos, esto lanzaria NPE en
            // vez de devolver el dominio con los refs que ya tenia en memoria.
            CompanyJpaEntity entity = new CompanyJpaEntity();
            entity.setId(9L);
            entity.setName("Clinica Norte");
            entity.setIdentifier("NIT-900");
            entity.setCreatedDate(LocalDateTime.of(2026, 1, 15, 10, 30));
            entity.setEnabled(true);
            CityRef cityRef = new CityRef(11L, "Bogota");
            MembershipRef membershipRef = new MembershipRef(21L, "Premium", "ACTIVE");

            Company company = mapper.toDomain(entity, cityRef, membershipRef);

            assertThat(company.getCity()).isEqualTo(cityRef);
            assertThat(company.getMembership()).isEqualTo(membershipRef);
            assertThat(company.getId()).isEqualTo(9L);
        }
    }
}
