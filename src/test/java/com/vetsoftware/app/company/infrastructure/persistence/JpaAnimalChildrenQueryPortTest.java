package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalChildrenQueryPort (company) — adaptador sobre AnimalJpaRepository")
class JpaAnimalChildrenQueryPortTest {

    @Mock
    private AnimalJpaRepository animalJpaRepository;

    private JpaAnimalChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaAnimalChildrenQueryPort(animalJpaRepository);
    }

    @Nested
    @DisplayName("existencia de animales activos")
    class ExistenciaDeAnimalesActivos {

        @Test
        @DisplayName("delega en el repositorio de animales por el id de la empresa")
        void delega_en_el_repositorio_de_animales() {
            when(animalJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID)).thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin animales devuelve falso")
        void una_empresa_sin_animales_devuelve_falso() {
            when(animalJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
