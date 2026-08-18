package com.vetsoftware.app.servicecategory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.servicecategory.testsupport.ServiceCategoryMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaServiceChildrenQueryPort — adaptador sobre ServiceJpaRepository")
class JpaServiceChildrenQueryPortTest {

    @Mock
    private ServiceJpaRepository serviceJpaRepository;

    private JpaServiceChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaServiceChildrenQueryPort(serviceJpaRepository);
    }

    @Nested
    @DisplayName("existencia de hijos activos")
    class ExistenciaDeHijosActivos {

        @Test
        @DisplayName("delega en el repositorio de servicios por el id de la categoria")
        void delega_en_el_repositorio_de_servicios() {
            when(serviceJpaRepository.existsByServiceCategory_Id(ServiceCategoryMother.CATEGORY_ID))
                    .thenReturn(true);

            boolean resultado = port
                    .existsActiveByServiceCategoryId(ServiceCategoryMother.CATEGORY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una categoria sin servicios devuelve falso")
        void una_categoria_sin_servicios_devuelve_falso() {
            when(serviceJpaRepository.existsByServiceCategory_Id(ServiceCategoryMother.CATEGORY_ID))
                    .thenReturn(false);

            boolean resultado = port
                    .existsActiveByServiceCategoryId(ServiceCategoryMother.CATEGORY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
