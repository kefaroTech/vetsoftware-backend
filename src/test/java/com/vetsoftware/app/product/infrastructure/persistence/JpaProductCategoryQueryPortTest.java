package com.vetsoftware.app.product.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaEntity;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaProductCategoryQueryPort (product)")
class JpaProductCategoryQueryPortTest {

    private static final Long CATEGORY_ID = 3L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private ProductCategoryJpaRepository productCategoryJpaRepository;

    @InjectMocks
    private JpaProductCategoryQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la categoria encontrada en la empresa a su ProductCategoryRef")
        void mapea_la_categoria_encontrada() {
            ProductCategoryJpaEntity entidad = mock(ProductCategoryJpaEntity.class);
            when(entidad.getId()).thenReturn(CATEGORY_ID);
            when(entidad.getName()).thenReturn("Alimentos");
            when(productCategoryJpaRepository.findByIdAndCompany_Id(CATEGORY_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            Optional<ProductCategoryRef> ref = port.findById(CATEGORY_ID, COMPANY_ID);

            assertThat(ref).contains(new ProductCategoryRef(CATEGORY_ID, "Alimentos"));
        }

        @Test
        @DisplayName("devuelve vacio si la categoria no existe en esa empresa")
        void devuelve_vacio_si_no_existe_en_la_empresa() {
            when(productCategoryJpaRepository.findByIdAndCompany_Id(CATEGORY_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(CATEGORY_ID, COMPANY_ID)).isEmpty();
        }
    }
}
