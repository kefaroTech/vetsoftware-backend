package com.vetsoftware.app.productcategory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productcategory.testsupport.ProductCategoryMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaProductChildrenQueryPort — adaptador sobre ProductJpaRepository")
class JpaProductChildrenQueryPortTest {

    @Mock
    private ProductJpaRepository productJpaRepository;

    private JpaProductChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaProductChildrenQueryPort(productJpaRepository);
    }

    @Nested
    @DisplayName("existencia de hijos activos")
    class ExistenciaDeHijosActivos {

        @Test
        @DisplayName("delega en el repositorio de productos por el id de la categoria")
        void delega_en_el_repositorio_de_productos() {
            when(productJpaRepository.existsByProductCategory_Id(ProductCategoryMother.CATEGORY_ID))
                    .thenReturn(true);

            boolean resultado = port
                    .existsActiveByProductCategoryId(ProductCategoryMother.CATEGORY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una categoria sin productos devuelve falso")
        void una_categoria_sin_productos_devuelve_falso() {
            when(productJpaRepository.existsByProductCategory_Id(ProductCategoryMother.CATEGORY_ID))
                    .thenReturn(false);

            boolean resultado = port
                    .existsActiveByProductCategoryId(ProductCategoryMother.CATEGORY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
