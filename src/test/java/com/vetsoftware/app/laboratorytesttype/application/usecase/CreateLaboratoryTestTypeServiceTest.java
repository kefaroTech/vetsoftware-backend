package com.vetsoftware.app.laboratorytesttype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import com.vetsoftware.app.laboratorytesttype.testsupport.LaboratoryTestTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateLaboratoryTestTypeService")
class CreateLaboratoryTestTypeServiceTest {

    @Mock
    private LaboratoryTestTypeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateLaboratoryTestTypeService service;

    @Nested
    @DisplayName("creacion permitida")
    class CreacionPermitida {

        @Test
        @DisplayName("un tipo propio de empresa resuelve la company por el puerto y la persiste")
        void un_tipo_propio_resuelve_la_company_por_el_puerto() {
            when(companyQueryPort.findById(LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LaboratoryTestTypeDto dto = service
                    .execute(LaboratoryTestTypeMother.comandoCrearPropio());

            ArgumentCaptor<LaboratoryTestType> guardado = ArgumentCaptor
                    .forClass(LaboratoryTestType.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompany())
                    .isEqualTo(LaboratoryTestTypeMother.CLINICA);
            assertThat(guardado.getValue().isGeneral()).isFalse();
            assertThat(dto.company().id()).isEqualTo(LaboratoryTestTypeMother.COMPANY_ID);
        }

        @Test
        @DisplayName("un tipo general no consulta el puerto de empresas")
        void un_tipo_general_no_consulta_el_puerto_de_empresas() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(LaboratoryTestTypeMother.comandoCrearGeneral());

            ArgumentCaptor<LaboratoryTestType> guardado = ArgumentCaptor
                    .forClass(LaboratoryTestType.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompany()).isNull();
            assertThat(guardado.getValue().isGeneral()).isTrue();
            verifyNoInteractions(companyQueryPort);
        }
    }

    @Nested
    @DisplayName("empresa inexistente")
    class EmpresaInexistente {

        @Test
        @DisplayName("no persiste nada si la empresa del command no existe")
        void no_persiste_nada_si_la_empresa_no_existe() {
            when(companyQueryPort.findById(LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(LaboratoryTestTypeMother.comandoCrearPropio()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + LaboratoryTestTypeMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }
}
