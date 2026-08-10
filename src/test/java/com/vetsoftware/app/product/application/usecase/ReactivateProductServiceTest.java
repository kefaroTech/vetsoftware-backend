package com.vetsoftware.app.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import com.vetsoftware.app.product.testsupport.ProductMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateProductService")
class ReactivateProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ReactivateProductService service;

    @Test
    @DisplayName("reactiva y devuelve el producto ya releido")
    void reactiva_y_devuelve_el_producto() {
        when(repository.reactivate(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID))
                .thenReturn(1);
        when(repository.findByIdAndCompanyId(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.gravado()));

        ProductDto dto = service.execute(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(ProductMother.PRODUCT_ID);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("cero filas afectadas: el producto no era de la empresa o no existe")
    void cero_filas_afectadas() {
        when(repository.reactivate(ProductMother.PRODUCT_ID, 999L)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(ProductMother.PRODUCT_ID, 999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found: " + ProductMother.PRODUCT_ID);

        // Si el UPDATE no toco nada, no tiene sentido releer: se corta ahi.
        verify(repository, never()).findByIdAndCompanyId(any(), any());
    }

    @Test
    @DisplayName("si la relectura vuelve vacia tras reactivar, tambien es 404")
    void relectura_vacia_tras_reactivar() {
        when(repository.reactivate(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID))
                .thenReturn(1);
        when(repository.findByIdAndCompanyId(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.execute(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
