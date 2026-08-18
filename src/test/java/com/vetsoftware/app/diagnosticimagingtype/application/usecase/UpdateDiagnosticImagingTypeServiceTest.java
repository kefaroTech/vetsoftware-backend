package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import com.vetsoftware.app.diagnosticimagingtype.testsupport.DiagnosticImagingTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateDiagnosticImagingTypeService")
class UpdateDiagnosticImagingTypeServiceTest {

    @Mock
    private DiagnosticImagingTypeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateDiagnosticImagingTypeService service;

    @Captor
    private ArgumentCaptor<DiagnosticImagingType> captor;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("actualiza el tipo existente con la empresa resuelta y lo persiste")
        void actualiza_el_tipo_existente_y_lo_persiste() {
            DiagnosticImagingType existente = DiagnosticImagingTypeMother.propiaDeEmpresa();
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.EMPRESA));
            when(repository.save(any())).thenReturn(existente);

            service.execute(DiagnosticImagingTypeMother.comandoActualizar());

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Ecografia abdominal (actualizada)");
        }

        @Test
        @DisplayName("devuelve el DTO del tipo actualizado")
        void devuelve_el_dto_del_tipo_actualizado() {
            DiagnosticImagingType existente = DiagnosticImagingTypeMother.propiaDeEmpresa();
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.EMPRESA));
            when(repository.save(any())).thenReturn(existente);

            DiagnosticImagingTypeDto dto = service
                    .execute(DiagnosticImagingTypeMother.comandoActualizar());

            assertThat(dto.id()).isEqualTo(DiagnosticImagingTypeMother.TYPE_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("tipo inexistente: no consulta la empresa ni persiste")
        void tipo_inexistente_no_consulta_ni_persiste() {
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingTypeMother.comandoActualizar()))
                    .isInstanceOf(
                            com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException.class)
                    .hasMessageContaining("DiagnosticImagingType not found: "
                            + DiagnosticImagingTypeMother.TYPE_ID);

            verifyNoInteractions(companyQueryPort);
        }

        @Test
        @DisplayName("empresa inexistente: no persiste")
        void empresa_inexistente_no_persiste() {
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.propiaDeEmpresa()));
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingTypeMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + DiagnosticImagingTypeMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("el tipo de OTRA empresa es 404 y no se apropia")
        void tipo_de_otra_empresa_es_not_found_y_no_escribe() {
            // El caso que @authz.isMyCompany NO cubre: el atacante declara SU empresa
            // (el gate pasa) y apunta al id ajeno. Sin fila que cargar no hay update que
            // le reescriba el company_id.
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingTypeMother.comandoActualizar()))
                    .isInstanceOf(
                            com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException.class);

            verifyNoInteractions(companyQueryPort);
            verify(repository, org.mockito.Mockito.never()).save(any());
            verify(repository, org.mockito.Mockito.never())
                    .findById(org.mockito.ArgumentMatchers.any());
        }
    }
}
