package com.vetsoftware.app.daycare.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.daycare.domain.AnimalRef;
import com.vetsoftware.app.daycare.domain.CompanyRef;
import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.daycare.domain.DayCareType;
import com.vetsoftware.app.daycare.testsupport.DayCareMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DayCareJpaMapper — ida y vuelta dominio <-> entidad")
class DayCareJpaMapperTest {

    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    private final DayCareJpaMapper mapper = new DayCareJpaMapper();

    @BeforeEach
    void stubsComunes() {
        // toJpa() no lee estos getters (solo asigna la referencia); toDomain() si.
        // lenient() evita el fallo de STRICT_STUBS en el test que solo ejercita toJpa.
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
            DayCare dayCare = DayCareMother.guarderiaValida();

            DayCareJpaEntity entity = mapper.toJpa(dayCare, animalEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(dayCare.getId());
            assertThat(entity.getDate()).isEqualTo(dayCare.getDate());
            assertThat(entity.getStartDate()).isEqualTo(dayCare.getStartDate());
            assertThat(entity.getEndDate()).isEqualTo(dayCare.getEndDate());
            assertThat(entity.getType()).isEqualTo(DayCareType.DAYCARE);
            assertThat(entity.getObjects()).isEqualTo(dayCare.getObjects());
            assertThat(entity.getObservations()).isEqualTo(dayCare.getObservations());
            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getCreatedDate()).isEqualTo(dayCare.getCreatedDate());
            assertThat(entity.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("el overload de un solo argumento reconstruye los refs desde el ManyToOne")
        void reconstruye_los_refs_desde_el_many_to_one() {
            DayCareJpaEntity entity = mapper.toJpa(DayCareMother.guarderiaValida(), animalEntity,
                    companyEntity);

            DayCare dominio = mapper.toDomain(entity);

            assertThat(dominio.getAnimal()).isEqualTo(new AnimalRef(1L, "Firulais", "A-001"));
            assertThat(dominio.getCompany())
                    .isEqualTo(new CompanyRef(10L, "Veterinaria de prueba", "900123456"));
        }

        @Test
        @DisplayName("el overload con refs precargados no toca los proxies del ManyToOne")
        void con_refs_precargados_no_toca_los_proxies() {
            DayCareJpaEntity entity = mapper.toJpa(DayCareMother.guarderiaValida(), animalEntity,
                    companyEntity);

            DayCare dominio = mapper.toDomain(entity, DayCareMother.FIRULAIS,
                    DayCareMother.CLINICA);

            assertThat(dominio.getAnimal()).isEqualTo(DayCareMother.FIRULAIS);
            assertThat(dominio.getCompany()).isEqualTo(DayCareMother.CLINICA);
            // Si el mapper leyera entity.getAnimal()/getCompany() en vez de reusar el ref
            // precargado, dispararia el proxy LAZY de Hibernate en produccion. Los
            // stubs de getId/getName/getIdentifier del @BeforeEach quedan sin usar aqui
            // a proposito: por eso son lenient().
            verifyNoMoreInteractions(animalEntity, companyEntity);
        }
    }
}
