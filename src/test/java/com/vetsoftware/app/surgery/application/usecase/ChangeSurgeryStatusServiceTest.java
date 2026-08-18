package com.vetsoftware.app.surgery.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import com.vetsoftware.app.surgery.domain.SurgeryStatus;
import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeSurgeryStatusService")
class ChangeSurgeryStatusServiceTest {

    @Mock
    private SurgeryRepository repository;

    @InjectMocks
    private ChangeSurgeryStatusService service;

    @Captor
    private ArgumentCaptor<Surgery> surgeryCaptor;

    @Nested
    @DisplayName("con companyId en el comando")
    class ConCompanyId {

        @ParameterizedTest
        @EnumSource(SurgeryStatus.class)
        @DisplayName("busca por id y empresa, y guarda con el nuevo estado sin importar el actual")
        void busca_por_id_y_empresa_y_guarda_con_el_nuevo_estado(SurgeryStatus nuevoEstado) {
            when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            SurgeryDto dto = service
                    .execute(SurgeryMother.comandoCambiarEstado(nuevoEstado.name().toLowerCase()));

            verify(repository).save(surgeryCaptor.capture());
            assertThat(surgeryCaptor.getValue().getStatus()).isEqualTo(nuevoEstado);
            assertThat(dto.status()).isEqualTo(nuevoEstado.name());
        }

        @Test
        @DisplayName("cirugia inexistente en la empresa lanza SurgeryNotFoundException y no guarda")
        void cirugia_inexistente_en_la_empresa() {
            when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID,
                    SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(SurgeryMother.comandoCambiarEstado("COMPLETADO")))
                    .isInstanceOf(SurgeryNotFoundException.class)
                    .hasMessageContaining("Surgery not found: " + SurgeryMother.SURGERY_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un estado que no existe en el catalogo lanza IllegalArgumentException")
        void un_estado_que_no_existe_en_el_catalogo() {
            when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));

            assertThatThrownBy(
                    () -> service.execute(SurgeryMother.comandoCambiarEstado("EN_CURSO")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No enum constant");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("con companyId nulo en el comando")
    class SinCompanyId {

        @Test
        @DisplayName("busca por id a secas, sin acotar por empresa")
        void busca_por_id_a_secas_sin_acotar_por_empresa() {
            when(repository.findById(SurgeryMother.SURGERY_ID))
                    .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(SurgeryMother.comandoCambiarEstadoSinCompanyId("COMPLETADO"));

            verify(repository).findById(SurgeryMother.SURGERY_ID);
            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }
}
