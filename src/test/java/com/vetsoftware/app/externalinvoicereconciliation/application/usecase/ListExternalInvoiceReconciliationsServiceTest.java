package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.testsupport.ExternalInvoiceReconciliationMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El ternario del filtro opcional: con {@code companyId} acota, sin el barre.
 *
 * <p>
 * <b>Lo que estos dos casos congelan es cual de las dos consultas se llama.</b>
 * Un {@code companyId} nulo que acabara llamando a {@code findAllByCompanyId}
 * devolveria siempre vacio, y la consola mostraria «no hay conciliaciones»
 * sobre una base llena — un fallo que no lanza, no registra nada y se lee como
 * un dato.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListExternalInvoiceReconciliationsService — barrido de la consola")
class ListExternalInvoiceReconciliationsServiceTest {

    @Mock
    private ExternalInvoiceReconciliationRepository repository;

    private ListExternalInvoiceReconciliationsService service;

    @BeforeEach
    void servicio() {
        service = new ListExternalInvoiceReconciliationsService(repository);
    }

    @Nested
    @DisplayName("Filtro opcional")
    class FiltroOpcional {

        @Test
        @DisplayName("sin companyId barre todas las empresas y no toca la consulta acotada")
        void sin_company_id_barre_todas_las_empresas() {
            when(repository.findAll(0, 20)).thenReturn(unaPagina());

            PageResult<ExternalInvoiceReconciliationDto> pagina = service.listAll(null, 0, 20);

            assertThat(pagina.content()).singleElement()
                    .satisfies(fila -> assertThat(fila.companyId()).isEqualTo(900L));
            assertThat(pagina.totalElements()).isEqualTo(1L);
            verify(repository).findAll(0, 20);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("con companyId acota y no toca el barrido completo")
        void con_company_id_acota() {
            when(repository.findAllByCompanyId(901L, 2, 5))
                    .thenReturn(PageResult.of(List.of(), 2, 5, 0L));

            PageResult<ExternalInvoiceReconciliationDto> pagina = service.listAll(901L, 2, 5);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.page()).isEqualTo(2);
            assertThat(pagina.pageSize()).isEqualTo(5);
            verify(repository).findAllByCompanyId(901L, 2, 5);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("los totales son los de la consulta, no los del contenido mapeado")
        void los_totales_son_los_de_la_consulta() {
            // Recalcular sobre el contenido ya paginado es como se acaba reportando
            // «1 de 1» en una bandeja de cuatrocientas.
            when(repository.findAll(0, 20)).thenReturn(PageResult.of(
                    List.of(ExternalInvoiceReconciliationMother.abiertaConId(41L)), 0, 20, 437L));

            PageResult<ExternalInvoiceReconciliationDto> pagina = service.listAll(null, 0, 20);

            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.totalElements()).isEqualTo(437L);
            assertThat(pagina.totalPages()).isEqualTo(22);
        }
    }

    private static PageResult<ExternalInvoiceReconciliation> unaPagina() {
        return PageResult.of(List.of(ExternalInvoiceReconciliationMother.abiertaConId(41L)), 0, 20,
                1L);
    }
}
