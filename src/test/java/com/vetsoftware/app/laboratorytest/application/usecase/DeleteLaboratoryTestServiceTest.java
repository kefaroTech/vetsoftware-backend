package com.vetsoftware.app.laboratorytest.application.usecase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteLaboratoryTestService")
class DeleteLaboratoryTestServiceTest {

    private static final Long ID = LaboratoryTestMother.ID;
    private static final Long EMPRESA = LaboratoryTestMother.CLINICA.id();
    private static final Long OTRA_EMPRESA = 99L;

    @Mock
    private LaboratoryTestRepository repository;

    @InjectMocks
    private DeleteLaboratoryTestService service;

    @Test
    @DisplayName("borra la muestra existente de la empresa del actor")
    void borra_la_muestra_existente() {
        when(repository.findByIdAndCompanyId(ID, EMPRESA))
                .thenReturn(Optional.of(LaboratoryTestMother.pendienteDeToma()));

        service.execute(ID, EMPRESA);

        verify(repository).delete(ID);
    }

    @Test
    @DisplayName("sin companyId (actor global) busca por id global antes de borrar")
    void sin_company_id_busca_por_id_global() {
        when(repository.findById(ID))
                .thenReturn(Optional.of(LaboratoryTestMother.pendienteDeToma()));

        service.execute(ID, null);

        verify(repository).delete(ID);
    }

    @Test
    @DisplayName("una muestra inexistente no dispara ningun borrado")
    void una_muestra_inexistente_no_dispara_borrado() {
        when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                .isInstanceOf(LaboratoryTestNotFoundException.class)
                .hasMessageContaining("LaboratoryTest not found: 42");

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("borrar una muestra ya deshabilitada sigue siendo valido: el borrado es logico")
    void borrar_una_muestra_ya_deshabilitada_sigue_siendo_valido() {
        when(repository.findByIdAndCompanyId(ID, EMPRESA))
                .thenReturn(Optional.of(LaboratoryTestMother.deshabilitada()));

        assertThatCode(() -> service.execute(ID, EMPRESA)).doesNotThrowAnyException();

        verify(repository).delete(ID);
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("una muestra de otra empresa es un 404 y no se borra nada")
        void muestra_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(ID, OTRA_EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ID, OTRA_EMPRESA))
                    .isInstanceOf(LaboratoryTestNotFoundException.class)
                    .hasMessageContaining("LaboratoryTest not found: 42");

            verify(repository, never()).delete(any());
            verify(repository, never()).findById(any());
        }
    }
}
