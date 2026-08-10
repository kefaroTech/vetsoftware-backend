package com.vetsoftware.app.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.testsupport.ProductMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchProductsService")
class SearchProductsServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private SearchProductsService service;

    @Test
    @DisplayName("proyecta el contenido de la pagina y conserva sus metadatos")
    void proyecta_y_conserva_metadatos() {
        SearchProductsCommand comando = ProductMother.comandoBuscar();
        PageResult<Product> pagina = new PageResult<>(
                List.of(ProductMother.gravado(1L), ProductMother.gravado(2L)), 0, 20, 2L, 1);
        when(repository.search(comando)).thenReturn(pagina);

        PageResult<ProductDto> resultado = service.execute(comando);

        assertThat(resultado.content()).extracting(ProductDto::id).containsExactly(1L, 2L);
        assertThat(resultado.page()).isZero();
        assertThat(resultado.pageSize()).isEqualTo(20);
        assertThat(resultado.totalElements()).isEqualTo(2L);
        assertThat(resultado.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("una busqueda sin coincidencias devuelve una pagina vacia")
    void busqueda_sin_coincidencias() {
        SearchProductsCommand comando = ProductMother.comandoBuscar();
        when(repository.search(comando)).thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        PageResult<ProductDto> resultado = service.execute(comando);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isZero();
    }

    @Test
    @DisplayName("pasa el comando al repositorio tal cual, incluido el companyId")
    void pasa_el_comando_tal_cual() {
        SearchProductsCommand comando = new SearchProductsCommand(ProductMother.COMPANY_ID, null,
                null, null, null, 3, 50);
        when(repository.search(comando))
                .thenReturn(new PageResult<>(List.of(ProductMother.gravado()), 3, 50, 151L, 4));

        PageResult<ProductDto> resultado = service.execute(comando);

        // El stub solo casa con ESTE comando: si el service reconstruyera uno
        // distinto (p. ej. perdiendo el companyId), Mockito no lo reconoceria.
        assertThat(resultado.page()).isEqualTo(3);
        assertThat(resultado.pageSize()).isEqualTo(50);
        assertThat(resultado.totalPages()).isEqualTo(4);
    }
}
