package com.vetsoftware.app.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeleteProductService")
class DeleteProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private DeleteProductService service;

    @Test
    @DisplayName("borra el producto que pertenece a la empresa")
    void borra_el_producto_de_la_empresa() {
        when(repository.findByIdAndCompanyId(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.gravado()));

        service.execute(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID);

        verify(repository).delete(ProductMother.PRODUCT_ID);
    }

    @Test
    @DisplayName("producto de otra empresa: excepcion y CERO borrados")
    void producto_de_otra_empresa_no_se_borra() {
        when(repository.findByIdAndCompanyId(ProductMother.PRODUCT_ID, 999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(ProductMother.PRODUCT_ID, 999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found: " + ProductMother.PRODUCT_ID);

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("borra por id, no por la entidad leida")
    void borra_por_id() {
        when(repository.findByIdAndCompanyId(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.gravado()));

        service.execute(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID);

        // El soft-delete de Hibernate cuelga del id; pasar otro id borraria otra fila.
        verify(repository).delete(ProductMother.PRODUCT_ID);
        verify(repository, never()).save(any());
    }
}
