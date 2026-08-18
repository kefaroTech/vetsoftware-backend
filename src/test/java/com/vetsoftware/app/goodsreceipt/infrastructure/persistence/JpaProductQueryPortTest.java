package com.vetsoftware.app.goodsreceipt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.goodsreceipt.domain.ProductRef;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaProductQueryPort (goodsreceipt)")
class JpaProductQueryPortTest {

    private static final Long PRODUCT_ID = 21L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private ProductJpaRepository productJpaRepository;

    @InjectMocks
    private JpaProductQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea el producto encontrado a su ProductRef")
        void mapea_el_producto_encontrado() {
            ProductJpaEntity entidad = mock(ProductJpaEntity.class);
            when(entidad.getId()).thenReturn(PRODUCT_ID);
            when(entidad.getName()).thenReturn("Vacuna triple");
            when(entidad.getCode()).thenReturn("P-021");
            when(productJpaRepository.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            Optional<ProductRef> ref = port.findById(PRODUCT_ID, COMPANY_ID);

            assertThat(ref).contains(new ProductRef(PRODUCT_ID, "Vacuna triple", "P-021"));
        }

        @Test
        @DisplayName("devuelve vacio si el producto no existe en la empresa")
        void devuelve_vacio_si_no_existe() {
            when(productJpaRepository.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(PRODUCT_ID, COMPANY_ID)).isEmpty();
        }
    }
}
