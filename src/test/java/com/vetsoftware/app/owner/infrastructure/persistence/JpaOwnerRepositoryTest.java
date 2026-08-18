package com.vetsoftware.app.owner.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.owner.domain.Owner;
import com.vetsoftware.app.owner.testsupport.OwnerMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Adaptador de persistencia probado con dobles sobre el puente de Spring Data:
 * no toca base de datos, pero verifica el cableado exacto entre
 * {@code OwnerJpaRepository}, {@code OwnerJpaMapper} y las referencias
 * cross-feature ({@code CityJpaRepository}, {@code CompanyJpaRepository}). El
 * comportamiento que solo decide el motor (soft delete, {@code @EntityGraph},
 * unicidad) lo cubre {@link OwnerPersistenceIT} contra MySQL real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaOwnerRepository")
class JpaOwnerRepositoryTest {

    @Mock
    private OwnerJpaRepository jpaRepository;
    @Mock
    private CityJpaRepository cityJpaRepository;
    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CityJpaEntity cityEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    private final OwnerJpaMapper mapper = new OwnerJpaMapper();

    private JpaOwnerRepository repository;

    private void construirRepository() {
        repository = new JpaOwnerRepository(jpaRepository, mapper, cityJpaRepository,
                companyJpaRepository);
    }

    /**
     * Entidad cruda, sin estimular los getters de las asociaciones: sirve para los
     * caminos que no relacionan la entidad de vuelta a dominio via
     * {@code toDomain(entity)} de un solo argumento (save, delete).
     */
    private OwnerJpaEntity entidadCruda(Long id) {
        return mapper.toJpa(OwnerMother.personaNatural(id), cityEntity, companyEntity);
    }

    /**
     * Estimula los getters de city/company: solo hace falta en los caminos de
     * lectura que pasan por {@code toDomain(entity)} de un solo argumento.
     */
    private void estimularAsociaciones() {
        when(cityEntity.getId()).thenReturn(OwnerMother.CITY_ID);
        when(cityEntity.getName()).thenReturn(OwnerMother.BOGOTA.name());
        when(companyEntity.getId()).thenReturn(OwnerMother.COMPANY_ID);
        when(companyEntity.getName()).thenReturn(OwnerMother.CLINICA.name());
        when(companyEntity.getIdentifier()).thenReturn(OwnerMother.CLINICA.identifier());
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("resuelve city y company por referencia y guarda la entidad mapeada")
        void resuelve_referencias_y_guarda_la_entidad_mapeada() {
            construirRepository();
            when(cityJpaRepository.getReferenceById(OwnerMother.CITY_ID)).thenReturn(cityEntity);
            when(companyJpaRepository.getReferenceById(OwnerMother.COMPANY_ID))
                    .thenReturn(companyEntity);
            OwnerJpaEntity guardada = entidadCruda(OwnerMother.OWNER_ID);
            when(jpaRepository.save(any())).thenReturn(guardada);

            Owner resultado = repository.save(OwnerMother.personaNatural(null));

            ArgumentCaptor<OwnerJpaEntity> capturado = ArgumentCaptor
                    .forClass(OwnerJpaEntity.class);
            verify(jpaRepository).save(capturado.capture());
            assertThat(capturado.getValue().getCity()).isSameAs(cityEntity);
            assertThat(capturado.getValue().getCompany()).isSameAs(companyEntity);
            assertThat(resultado.getId()).isEqualTo(OwnerMother.OWNER_ID);
            // El resultado reusa el CityRef/CompanyRef del owner original, no relee las
            // asociaciones JPA: evita la query de hidratacion tras getReferenceById.
            assertThat(resultado.getCity()).isEqualTo(OwnerMother.BOGOTA);
            assertThat(resultado.getCompany()).isEqualTo(OwnerMother.CLINICA);
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("owner encontrado se traduce a dominio")
        void owner_encontrado_se_traduce_a_dominio() {
            construirRepository();
            estimularAsociaciones();
            when(jpaRepository.findByIdAndCompanyId(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                    .thenReturn(Optional.of(entidadCruda(OwnerMother.OWNER_ID)));

            Optional<Owner> encontrado = repository.findByIdAndCompanyId(OwnerMother.OWNER_ID,
                    OwnerMother.COMPANY_ID);

            assertThat(encontrado).isPresent();
            assertThat(encontrado.orElseThrow().getId()).isEqualTo(OwnerMother.OWNER_ID);
        }

        @Test
        @DisplayName("owner inexistente o de otra empresa devuelve Optional vacio")
        void owner_inexistente_devuelve_optional_vacio() {
            construirRepository();
            when(jpaRepository.findByIdAndCompanyId(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(
                    repository.findByIdAndCompanyId(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("listados paginados")
    class ListadosPaginados {

        @Test
        @DisplayName("findAllByCompanyId traduce la pagina de Spring Data a PageResult")
        void find_all_by_company_id_traduce_la_pagina() {
            construirRepository();
            estimularAsociaciones();
            Page<OwnerJpaEntity> pagina = new PageImpl<>(
                    java.util.List.of(entidadCruda(OwnerMother.OWNER_ID)), PageRequest.of(0, 20),
                    1);
            when(jpaRepository.findAllByCompanyId(
                    org.mockito.ArgumentMatchers.eq(OwnerMother.COMPANY_ID), any()))
                    .thenReturn(pagina);

            PageResult<Owner> resultado = repository.findAllByCompanyId(OwnerMother.COMPANY_ID, 0,
                    20);

            assertThat(resultado.content()).extracting(Owner::getId)
                    .containsExactly(OwnerMother.OWNER_ID);
            assertThat(resultado.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("searchByCompanyAndTerm traduce la pagina de Spring Data a PageResult")
        void search_by_company_and_term_traduce_la_pagina() {
            construirRepository();
            estimularAsociaciones();
            Page<OwnerJpaEntity> pagina = new PageImpl<>(
                    java.util.List.of(entidadCruda(OwnerMother.OWNER_ID)), PageRequest.of(0, 20),
                    1);
            when(jpaRepository.searchByCompanyAndTerm(
                    org.mockito.ArgumentMatchers.eq(OwnerMother.COMPANY_ID),
                    org.mockito.ArgumentMatchers.eq("ana"), any())).thenReturn(pagina);

            PageResult<Owner> resultado = repository.searchByCompanyAndTerm(OwnerMother.COMPANY_ID,
                    "ana", 0, 20);

            assertThat(resultado.content()).extracting(Owner::getId)
                    .containsExactly(OwnerMother.OWNER_ID);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("owner encontrado en la empresa se borra")
        void owner_encontrado_se_borra() {
            construirRepository();
            OwnerJpaEntity entidad = entidadCruda(OwnerMother.OWNER_ID);
            when(jpaRepository.findByIdAndCompanyId(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            repository.delete(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID);

            verify(jpaRepository).delete(entidad);
        }

        @Test
        @DisplayName("owner inexistente en la empresa no dispara ningun delete")
        void owner_inexistente_no_dispara_delete() {
            construirRepository();
            when(jpaRepository.findByIdAndCompanyId(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            repository.delete(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID);

            verify(jpaRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("reactivate")
    class Reactivate {

        @Test
        @DisplayName("delega en el UPDATE nativo y devuelve las filas afectadas")
        void delega_en_el_update_nativo() {
            construirRepository();
            when(jpaRepository.reactivate(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                    .thenReturn(1);

            int filas = repository.reactivate(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID);

            assertThat(filas).isEqualTo(1);
        }
    }
}
