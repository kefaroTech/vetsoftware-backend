package com.vetsoftware.app.spa.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.spa.domain.AnimalRef;
import com.vetsoftware.app.spa.domain.CompanyRef;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaStatus;
import com.vetsoftware.app.spa.domain.SpaTypeRef;
import com.vetsoftware.app.spa.testsupport.SpaMother;
import com.vetsoftware.app.spatype.infrastructure.persistence.SpaTypeJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpaJpaMapper — ida y vuelta dominio <-> entidad")
class SpaJpaMapperTest {

    @Mock
    private SpaTypeJpaEntity spaTypeEntity;
    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    private final SpaJpaMapper mapper = new SpaJpaMapper();

    @BeforeEach
    void stubsComunes() {
        // toJpa() no lee estos getters (solo asigna la referencia); toDomain() si.
        // lenient() evita el fallo de STRICT_STUBS en el test que solo ejercita toJpa.
        lenient().when(spaTypeEntity.getId()).thenReturn(20L);
        lenient().when(spaTypeEntity.getName()).thenReturn("Baño básico");
        lenient().when(animalEntity.getId()).thenReturn(1L);
        lenient().when(animalEntity.getName()).thenReturn("Firulais");
        lenient().when(animalEntity.getCode()).thenReturn("A-001");
        lenient().when(companyEntity.getId()).thenReturn(10L);
        lenient().when(companyEntity.getName()).thenReturn("Veterinaria de prueba");
        lenient().when(companyEntity.getIdentifier()).thenReturn("900123456");
    }

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo del dominio a la entidad")
        void copia_cada_campo_a_la_entidad() {
            Spa spa = SpaMother.spaValido();

            SpaJpaEntity entity = mapper.toJpa(spa, spaTypeEntity, animalEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(spa.getId());
            assertThat(entity.getDate()).isEqualTo(spa.getDate());
            assertThat(entity.getSpaType()).isSameAs(spaTypeEntity);
            assertThat(entity.getReason()).isEqualTo(spa.getReason());
            assertThat(entity.getDetails()).isEqualTo(spa.getDetails());
            assertThat(entity.getObservations()).isEqualTo(spa.getObservations());
            assertThat(entity.getStatus()).isEqualTo("AGENDADA");
            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getCreatedDate()).isEqualTo(spa.getCreatedDate());
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un spa completado copia el status correspondiente")
        void un_spa_completado_copia_el_status_correspondiente() {
            Spa spa = SpaMother.spaValido();
            spa.changeStatus(SpaStatus.COMPLETADO);

            SpaJpaEntity entity = mapper.toJpa(spa, spaTypeEntity, animalEntity, companyEntity);

            assertThat(entity.getStatus()).isEqualTo("COMPLETADO");
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("el overload de un solo argumento reconstruye los refs desde el ManyToOne")
        void reconstruye_los_refs_desde_el_many_to_one() {
            SpaJpaEntity entity = mapper.toJpa(SpaMother.spaValido(), spaTypeEntity, animalEntity,
                    companyEntity);

            Spa dominio = mapper.toDomain(entity);

            assertThat(dominio.getSpaType()).isEqualTo(new SpaTypeRef(20L, "Baño básico"));
            assertThat(dominio.getAnimal()).isEqualTo(new AnimalRef(1L, "Firulais", "A-001"));
            assertThat(dominio.getCompany())
                    .isEqualTo(new CompanyRef(10L, "Veterinaria de prueba", "900123456"));
            assertThat(dominio.getStatus()).isEqualTo(SpaStatus.AGENDADA);
        }

        @Test
        @DisplayName("el overload con refs precargados no toca los proxies del ManyToOne")
        void con_refs_precargados_no_toca_los_proxies() {
            SpaJpaEntity entity = mapper.toJpa(SpaMother.spaValido(), spaTypeEntity, animalEntity,
                    companyEntity);

            Spa dominio = mapper.toDomain(entity, SpaMother.BANO_BASICO, SpaMother.FIRULAIS,
                    SpaMother.CLINICA);

            assertThat(dominio.getSpaType()).isEqualTo(SpaMother.BANO_BASICO);
            assertThat(dominio.getAnimal()).isEqualTo(SpaMother.FIRULAIS);
            assertThat(dominio.getCompany()).isEqualTo(SpaMother.CLINICA);
            // Si el mapper leyera entity.getSpaType()/getAnimal()/getCompany() en vez de
            // reusar el ref precargado, dispararia el proxy LAZY de Hibernate en
            // produccion. Los stubs del @BeforeEach quedan sin usar aqui a proposito:
            // por eso son lenient().
            verifyNoMoreInteractions(spaTypeEntity, animalEntity, companyEntity);
        }
    }
}
