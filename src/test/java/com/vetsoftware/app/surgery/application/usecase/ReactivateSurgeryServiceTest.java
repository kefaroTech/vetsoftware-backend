package com.vetsoftware.app.surgery.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code ReactivateSurgeryUseCase.execute(Long id, Long companyId)} recibe la
 * empresa y el UPDATE nativo de {@code SurgeryJpaRepository.reactivate} la
 * lleva al {@code WHERE}: aqui no hay lectura previa que valide la propiedad,
 * asi que ese filtro es la unica barrera.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateSurgeryService")
class ReactivateSurgeryServiceTest {

    private static final Long EMPRESA = SurgeryMother.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SurgeryMother.OTRA_CLINICA.id();

    @Mock
    private SurgeryRepository repository;

    @InjectMocks
    private ReactivateSurgeryService service;

    @Test
    @DisplayName("reactiva y devuelve la cirugia ya habilitada")
    void reactiva_y_devuelve_la_cirugia_ya_habilitada() {
        when(repository.reactivate(SurgeryMother.SURGERY_ID, EMPRESA)).thenReturn(1);
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, EMPRESA))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));

        SurgeryDto dto = service.execute(SurgeryMother.SURGERY_ID, EMPRESA);

        assertThat(dto.id()).isEqualTo(SurgeryMother.SURGERY_ID);
        assertThat(dto.enabled()).isTrue();
        verify(repository).reactivate(SurgeryMother.SURGERY_ID, EMPRESA);
    }

    @Test
    @DisplayName("cero filas afectadas es no-encontrado y evita la lectura posterior")
    void cero_filas_afectadas_es_no_encontrado() {
        when(repository.reactivate(SurgeryMother.SURGERY_ID, EMPRESA)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(SurgeryMother.SURGERY_ID, EMPRESA))
                .isInstanceOf(SurgeryNotFoundException.class)
                .hasMessageContaining("Surgery not found: " + SurgeryMother.SURGERY_ID);

        verify(repository, never()).findByIdAndCompanyId(any(), any());
    }

    @Test
    @DisplayName("si la cirugia desaparece entre el UPDATE y el SELECT, falla como no-encontrada")
    void si_la_cirugia_desaparece_entre_el_update_y_el_select() {
        when(repository.reactivate(SurgeryMother.SURGERY_ID, EMPRESA)).thenReturn(1);
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, EMPRESA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SurgeryMother.SURGERY_ID, EMPRESA))
                .isInstanceOf(SurgeryNotFoundException.class)
                .hasMessageContaining("Surgery not found: " + SurgeryMother.SURGERY_ID);
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("una cirugia de otra empresa no se reactiva y no se relee")
        void cirugia_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(SurgeryMother.SURGERY_ID, OTRA_EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(SurgeryMother.SURGERY_ID, OTRA_EMPRESA))
                    .isInstanceOf(SurgeryNotFoundException.class)
                    .hasMessageContaining("Surgery not found: " + SurgeryMother.SURGERY_ID);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(repository, never()).findById(any());
        }
    }
}
