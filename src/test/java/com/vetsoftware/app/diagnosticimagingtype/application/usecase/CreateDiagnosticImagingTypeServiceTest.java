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
@DisplayName("CreateDiagnosticImagingTypeService")
class CreateDiagnosticImagingTypeServiceTest {

    @Mock
    private DiagnosticImagingTypeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateDiagnosticImagingTypeService service;

    @Captor
    private ArgumentCaptor<DiagnosticImagingType> captor;

    @Nested
    @DisplayName("tipo general (sin empresa)")
    class TipoGeneral {

        @Test
        @DisplayName("persiste el tipo sin consultar el puerto de empresa")
        void persiste_el_tipo_sin_consultar_el_puerto_de_empresa() {
            when(repository.save(any())).thenReturn(DiagnosticImagingTypeMother.general());

            service.execute(DiagnosticImagingTypeMother.comandoCrearGeneral());

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCompany()).isNull();
            assertThat(captor.getValue().isGeneral()).isTrue();
            verifyNoInteractions(companyQueryPort);
        }

        @Test
        @DisplayName("devuelve el DTO del tipo ya persistido")
        void devuelve_el_dto_del_tipo_persistido() {
            when(repository.save(any())).thenReturn(DiagnosticImagingTypeMother.general());

            DiagnosticImagingTypeDto dto = service
                    .execute(DiagnosticImagingTypeMother.comandoCrearGeneral());

            assertThat(dto.id()).isEqualTo(DiagnosticImagingTypeMother.TYPE_ID);
            assertThat(dto.general()).isTrue();
        }
    }

    @Nested
    @DisplayName("tipo propio de una empresa")
    class TipoDeEmpresa {

        @Test
        @DisplayName("resuelve la empresa con el puerto y la asocia al tipo guardado")
        void resuelve_la_empresa_y_la_asocia_al_tipo() {
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.EMPRESA));
            when(repository.save(any())).thenReturn(DiagnosticImagingTypeMother.propiaDeEmpresa());

            service.execute(DiagnosticImagingTypeMother.comandoCrearDeEmpresa());

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCompany())
                    .isEqualTo(DiagnosticImagingTypeMother.EMPRESA);
        }

        @Test
        @DisplayName("empresa inexistente: no persiste nada")
        void empresa_inexistente_no_persiste_nada() {
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingTypeMother.comandoCrearDeEmpresa()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + DiagnosticImagingTypeMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }
}
