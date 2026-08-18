package com.vetsoftware.app.service.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.testsupport.ServiceMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchServicesService")
class SearchServicesServiceTest {

    @Mock
    private ServiceRepository repository;

    @InjectMocks
    private SearchServicesService service;

    @Test
    @DisplayName("mapea el contenido de la pagina sin alterar sus metadatos")
    void mapea_el_contenido_sin_alterar_los_metadatos() {
        SearchServicesCommand command = new SearchServicesCommand(ServiceMother.COMPANY_ID,
                "Consulta", null, null, 0, 20);
        PageResult<com.vetsoftware.app.service.domain.Service> pagina = PageResult
                .of(List.of(ServiceMother.consultaGeneral()), 0, 20, 1L);
        when(repository.search(command)).thenReturn(pagina);

        PageResult<ServiceDto> resultado = service.execute(command);

        assertThat(resultado.content()).extracting(ServiceDto::id)
                .containsExactly(ServiceMother.SERVICE_ID);
        assertThat(resultado.page()).isEqualTo(0);
        assertThat(resultado.pageSize()).isEqualTo(20);
        assertThat(resultado.totalElements()).isEqualTo(1L);
        assertThat(resultado.totalPages()).isEqualTo(1);
    }
}
