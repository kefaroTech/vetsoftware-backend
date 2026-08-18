package com.vetsoftware.app.laboratorytest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
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

/**
 * {@code ReactivateLaboratoryTestUseCase.execute(Long id, Long companyId)}
 * recibe la empresa y su {@code @PreAuthorize} la valida con
 * {@code @authz.isMyCompany(#companyId)}. La empresa viaja hasta el UPDATE
 * porque aqui no hay lectura previa que valide la propiedad: el servicio decide
 * si la fila existe mirando las filas afectadas, asi que el SQL acotado es LA
 * barrera.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateLaboratoryTestService")
class ReactivateLaboratoryTestServiceTest {

    private static final Long ID = LaboratoryTestMother.ID;
    private static final Long EMPRESA = LaboratoryTestMother.CLINICA.id();
    private static final Long OTRA_EMPRESA = 99L;

    @Mock
    private LaboratoryTestRepository repository;

    @InjectMocks
    private ReactivateLaboratoryTestService service;

    @Nested
    @DisplayName("Reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva la muestra y devuelve el dto releido")
        void reactiva_la_muestra_y_devuelve_el_dto() {
            when(repository.reactivate(ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(LaboratoryTestMother.deshabilitada()));

            var dto = service.execute(ID, EMPRESA);

            assertThat(dto.id()).isEqualTo(ID);
            verify(repository).reactivate(ID, EMPRESA);
        }

        @Test
        @DisplayName("la relectura tambien va acotada por empresa")
        void la_relectura_tambien_va_acotada_por_empresa() {
            when(repository.reactivate(ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(LaboratoryTestMother.deshabilitada()));

            service.execute(ID, EMPRESA);

            verify(repository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Rechazos")
    class Rechazos {

        @Test
        @DisplayName("una muestra inexistente no consulta la relectura")
        void una_muestra_inexistente_no_consulta_la_relectura() {
            when(repository.reactivate(ID, EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                    .isInstanceOf(LaboratoryTestNotFoundException.class)
                    .hasMessageContaining("LaboratoryTest not found: 42");

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }

        @Test
        @DisplayName("una reactivacion que afecta filas pero luego no encuentra la muestra tambien falla")
        void reactivar_sin_encontrar_luego_la_muestra_tambien_falla() {
            when(repository.reactivate(ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                    .isInstanceOf(LaboratoryTestNotFoundException.class)
                    .hasMessageContaining("LaboratoryTest not found: 42");
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("una muestra de otra empresa no se reactiva y no se relee")
        void muestra_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(ID, OTRA_EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ID, OTRA_EMPRESA))
                    .isInstanceOf(LaboratoryTestNotFoundException.class)
                    .hasMessageContaining("LaboratoryTest not found: 42");

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(repository, never()).findById(any());
        }
    }
}
