package com.vetsoftware.app.dunning.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.dunning.application.port.out.DunningEventRepository;
import com.vetsoftware.app.dunning.testsupport.DunningEventMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAllDunningEventsService - cobranza cross-tenant")
class ListAllDunningEventsServiceTest {

    @Mock
    private DunningEventRepository repository;
    @InjectMocks
    private ListAllDunningEventsService service;

    @Test
    @DisplayName("sin companyId usa el expediente global y conserva la pagina")
    void sin_company_id_lista_todos_los_tenants() {
        when(repository.findAll(1, 10))
                .thenReturn(PageResult.of(List.of(DunningEventMother.recordatorio()), 1, 10, 13));

        PageResult<DunningEventDto> result = service.listAll(null, 1, 10);

        assertThat(result.content()).singleElement().satisfies(event -> {
            assertThat(event.companyId()).isEqualTo(DunningEventMother.EMPRESA);
            assertThat(event.subscription().id()).isEqualTo(11L);
        });
        assertThat(result.totalElements()).isEqualTo(13);
        assertThat(result.totalPages()).isEqualTo(2);
        verify(repository).findAll(1, 10);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("con companyId delega en el expediente acotado")
    void con_company_id_filtra_el_tenant_solicitado() {
        when(repository.findAllByCompanyId(DunningEventMother.EMPRESA, 0, 20))
                .thenReturn(PageResult.empty(0, 20));

        PageResult<DunningEventDto> result = service.listAll(DunningEventMother.EMPRESA, 0, 20);

        assertThat(result.content()).isEmpty();
        verify(repository).findAllByCompanyId(DunningEventMother.EMPRESA, 0, 20);
        verifyNoMoreInteractions(repository);
    }
}
