package com.vetsoftware.app.electronicdocument.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.dto.PageResult;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Listado paginado de documentos de la empresa, con filtros OPCIONALES por
 * sede, tipo y estado DIAN. El servicio no reinventa la paginacion: proyecta el
 * contenido y traslada los metadatos tal cual, que es lo que el front usa para
 * pintar el paginador.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListElectronicDocumentsService — listado paginado")
class ListElectronicDocumentsServiceTest {

    @Mock
    private ElectronicDocumentRepository repository;
    @InjectMocks
    private ListElectronicDocumentsService service;

    @Test
    @DisplayName("proyecta cada documento a DTO conservando los metadatos de la pagina")
    void proyecta_cada_documento_conservando_los_metadatos() {
        List<ElectronicDocument> documentos = List.of(ElectronicDocumentMother.facturaValidada(55L),
                ElectronicDocumentMother.facturaValidada(56L));
        when(repository.findAllByCompanyId(9L, null, null, null, 1, 20))
                .thenReturn(new PageResult<>(documentos, 1, 20, 41L, 3));

        PageResult<ElectronicDocumentDto> pagina = service.listByCompany(9L, null, null, null, 1,
                20);

        assertThat(pagina.content()).extracting(ElectronicDocumentDto::id).containsExactly(55L,
                56L);
        assertThat(pagina.page()).isEqualTo(1);
        assertThat(pagina.pageSize()).isEqualTo(20);
        assertThat(pagina.totalElements()).isEqualTo(41L);
        assertThat(pagina.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("traslada el filtro de sede al repositorio sin reinterpretarlo")
    void traslada_el_filtro_de_sede_al_repositorio() {
        when(repository.findAllByCompanyId(9L, 7L, null, null, 0, 20))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        service.listByCompany(9L, 7L, null, null, 0, 20);

        verify(repository).findAllByCompanyId(9L, 7L, null, null, 0, 20);
    }

    /**
     * BE-06: los dos selectores de la pantalla viajan al servidor. Si el servicio
     * se los comiera, el listado paginado devolveria documentos de otro tipo o
     * estado y la pantalla, ya sin filtro de cliente, los pintaria.
     */
    @Test
    @DisplayName("traslada tipo de documento y estado DIAN al repositorio")
    void traslada_tipo_y_estado_al_repositorio() {
        when(repository.findAllByCompanyId(9L, null, ElectronicDocumentType.FE_VENTA,
                DianStatus.VALIDADO, 0, 20)).thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        service.listByCompany(9L, null, ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO, 0,
                20);

        verify(repository).findAllByCompanyId(9L, null, ElectronicDocumentType.FE_VENTA,
                DianStatus.VALIDADO, 0, 20);
    }

    @Test
    @DisplayName("una empresa sin documentos devuelve una pagina vacia, no null")
    void una_empresa_sin_documentos_devuelve_pagina_vacia() {
        when(repository.findAllByCompanyId(9L, null, null, null, 0, 20))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        assertThat(service.listByCompany(9L, null, null, null, 0, 20).content()).isEmpty();
    }

    @Test
    @DisplayName("el listado no escribe nada en el repositorio")
    void el_listado_no_escribe_nada() {
        when(repository.findAllByCompanyId(9L, null, null, null, 0, 20))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        service.listByCompany(9L, null, null, null, 0, 20);

        verify(repository).findAllByCompanyId(9L, null, null, null, 0, 20);
        verifyNoMoreInteractions(repository);
    }
}
