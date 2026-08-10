package com.vetsoftware.app.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindProductService")
class FindProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private FindProductService service;

    @Test
    @DisplayName("devuelve el DTO del producto de la empresa")
    void devuelve_el_dto_del_producto() {
        when(repository.findByIdAndCompanyId(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.gravado()));

        ProductDto dto = service.findById(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(ProductMother.PRODUCT_ID);
        assertThat(dto.name()).isEqualTo("Concentrado adulto");
        assertThat(dto.company().id()).isEqualTo(ProductMother.COMPANY_ID);
    }

    @Test
    @DisplayName("un producto de otra empresa se ve como inexistente")
    void producto_de_otra_empresa_es_inexistente() {
        // La busqueda va siempre acotada por companyId: sin esa firma, el producto
        // de otro tenant seria legible desde aqui.
        when(repository.findByIdAndCompanyId(ProductMother.PRODUCT_ID, 999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ProductMother.PRODUCT_ID, 999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found: " + ProductMother.PRODUCT_ID);
    }

    @Test
    @DisplayName("proyecta tambien el producto sin impuesto ni proveedor")
    void proyecta_el_producto_sin_referencias_opcionales() {
        when(repository.findByIdAndCompanyId(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.excluido()));

        ProductDto dto = service.findById(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID);

        assertThat(dto.tax()).isNull();
        assertThat(dto.supplier()).isNull();
    }
}
