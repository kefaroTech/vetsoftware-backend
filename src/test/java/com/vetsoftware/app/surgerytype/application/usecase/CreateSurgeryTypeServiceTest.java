package com.vetsoftware.app.surgerytype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import com.vetsoftware.app.surgerytype.testsupport.SurgeryTypeMother;
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
@DisplayName("CreateSurgeryTypeService")
class CreateSurgeryTypeServiceTest {

    @Mock
    private SurgeryTypeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateSurgeryTypeService service;

    @Captor
    private ArgumentCaptor<SurgeryType> captor;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("crea un tipo propio con la empresa resuelta por el puerto")
        void crea_un_tipo_propio_con_la_empresa_resuelta() {
            when(companyQueryPort.findById(SurgeryTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryTypeMother.EMPRESA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SurgeryTypeDto dto = service.execute(SurgeryTypeMother.comandoCrearPropio());

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCompany()).isEqualTo(SurgeryTypeMother.EMPRESA);
            assertThat(dto.name()).isEqualTo("Castracion");
            assertThat(dto.general()).isFalse();
        }

        @Test
        @DisplayName("crea un tipo general sin consultar el puerto de empresa")
        void crea_un_tipo_general_sin_consultar_el_puerto() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SurgeryTypeDto dto = service.execute(SurgeryTypeMother.comandoCrearGeneral());

            verifyNoInteractions(companyQueryPort);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCompany()).isNull();
            assertThat(dto.general()).isTrue();
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no crea el tipo si la empresa no existe")
        void no_crea_el_tipo_si_la_empresa_no_existe() {
            when(companyQueryPort.findById(SurgeryTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.comandoCrearPropio()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + SurgeryTypeMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }
}
