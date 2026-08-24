package com.vetsoftware.app.pricelist.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListPriceListsService")
class ListPriceListsServiceTest {

    @Mock
    private PriceListRepository repository;

    @InjectMocks
    private ListPriceListsService service;

    @Test
    @DisplayName("mapea el contenido y conserva los metadatos que vienen de la consulta")
    void conserva_los_metadatos_de_la_consulta() {
        PageResult<PriceList> pagina = new PageResult<>(List.of(PriceListMother.borrador()), 2, 20,
                57L, 3);
        when(repository.findAll(2, 20)).thenReturn(pagina);

        PageResult<PriceListDto> resultado = service.listAll(2, 20);

        assertThat(resultado.content()).singleElement().extracting(PriceListDto::code)
                .isEqualTo("LISTA-2026-01");
        assertThat(resultado.page()).isEqualTo(2);
        assertThat(resultado.pageSize()).isEqualTo(20);
        assertThat(resultado.totalElements()).isEqualTo(57L);
        assertThat(resultado.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("una página vacía no pierde el total")
    void pagina_vacia_conserva_el_total() {
        when(repository.findAll(9, 20)).thenReturn(new PageResult<>(List.of(), 9, 20, 57L, 3));

        PageResult<PriceListDto> resultado = service.listAll(9, 20);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isEqualTo(57L);
    }
}
