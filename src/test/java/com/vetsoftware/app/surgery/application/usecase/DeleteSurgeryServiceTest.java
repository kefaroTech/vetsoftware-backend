package com.vetsoftware.app.surgery.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
 * {@code DeleteSurgeryUseCase.execute(Long id, Long companyId)} acota el
 * borrado por empresa: la lectura previa es lo que convierte un id ajeno en un
 * 404.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSurgeryService")
class DeleteSurgeryServiceTest {

    private static final Long EMPRESA = SurgeryMother.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SurgeryMother.OTRA_CLINICA.id();

    @Mock
    private SurgeryRepository repository;

    @InjectMocks
    private DeleteSurgeryService service;

    @Test
    @DisplayName("borra la cirugia existente de la empresa del actor")
    void borra_la_cirugia_existente() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, EMPRESA))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));

        service.execute(SurgeryMother.SURGERY_ID, EMPRESA);

        verify(repository).delete(SurgeryMother.SURGERY_ID);
    }

    @Test
    @DisplayName("sin companyId (actor global) busca por id global antes de borrar")
    void sin_company_id_busca_por_id_global() {
        when(repository.findById(SurgeryMother.SURGERY_ID))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));

        service.execute(SurgeryMother.SURGERY_ID, null);

        verify(repository).delete(SurgeryMother.SURGERY_ID);
    }

    @Test
    @DisplayName("una cirugia inexistente lanza SurgeryNotFoundException y no borra nada")
    void una_cirugia_inexistente_lanza_not_found_y_no_borra_nada() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, EMPRESA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SurgeryMother.SURGERY_ID, EMPRESA))
                .isInstanceOf(SurgeryNotFoundException.class)
                .hasMessageContaining("Surgery not found: " + SurgeryMother.SURGERY_ID);

        verify(repository, never()).delete(any());
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("una cirugia de otra empresa es un 404 y no se borra nada")
        void cirugia_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryMother.SURGERY_ID, OTRA_EMPRESA))
                    .isInstanceOf(SurgeryNotFoundException.class)
                    .hasMessageContaining("Surgery not found: " + SurgeryMother.SURGERY_ID);

            verify(repository, never()).delete(any());
            verify(repository, never()).findById(any());
        }
    }
}
