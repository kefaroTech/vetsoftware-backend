package com.vetsoftware.app.laboratorytesttype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytesttype.application.command.UpdateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNameAlreadyExistsException;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
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
@DisplayName("UpdateLaboratoryTestTypeService")
class UpdateLaboratoryTestTypeServiceTest {

    @Mock
    private LaboratoryTestTypeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateLaboratoryTestTypeService service;

    @Nested
    @DisplayName("actualizacion permitida")
    class ActualizacionPermitida {

        @Test
        @DisplayName("resuelve la empresa por el puerto y guarda el tipo actualizado")
        void resuelve_la_empresa_por_el_puerto_y_guarda() {
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.propioDeEmpresa()));
            when(companyQueryPort.findById(LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.CLINICA));
            // La guarda de unicidad se consulta en el ambito de la EMPRESA del command
            // y excluyendo la propia fila: renombrarse a si misma no es un choque.
            when(repository.existsActiveByNameAndCompanyIdExcludingId("Hemograma completo",
                    LaboratoryTestTypeMother.COMPANY_ID, LaboratoryTestTypeMother.TYPE_ID))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LaboratoryTestTypeDto dto = service
                    .execute(LaboratoryTestTypeMother.comandoActualizarPropio());

            ArgumentCaptor<LaboratoryTestType> guardado = ArgumentCaptor
                    .forClass(LaboratoryTestType.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getName()).isEqualTo("Hemograma completo");
            assertThat(guardado.getValue().getDescription())
                    .isEqualTo("Hemograma completo con formula");
            assertThat(dto.name()).isEqualTo("Hemograma completo");
        }

        @Test
        @DisplayName("un command sin companyId deja el tipo sin company y no consulta el puerto")
        void un_command_sin_company_id_deja_el_tipo_sin_company() {
            when(repository.findById(LaboratoryTestTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.general()));
            // El camino SYSTEM consulta la guarda con companyId NULO: el ambito es el
            // catalogo de plataforma. Con STRICT_STUBS, preguntarla con una empresa
            // levanta PotentialStubbingProblem y este caso falla.
            when(repository.existsActiveByNameAndCompanyIdExcludingId("Perfil renal", null,
                    LaboratoryTestTypeMother.TYPE_ID)).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateLaboratoryTestTypeCommand comando = new UpdateLaboratoryTestTypeCommand(
                    LaboratoryTestTypeMother.TYPE_ID, "Perfil renal", "Perfil renal ampliado", null,
                    true);

            service.execute(comando);

            ArgumentCaptor<LaboratoryTestType> guardado = ArgumentCaptor
                    .forClass(LaboratoryTestType.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompany()).isNull();
            verifyNoInteractions(companyQueryPort);
        }
    }

    @Nested
    @DisplayName("tipo inexistente")
    class TipoInexistente {

        @Test
        @DisplayName("no guarda nada ni consulta la empresa si el tipo no existe")
        void no_guarda_nada_si_el_tipo_no_existe() {
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(LaboratoryTestTypeMother.comandoActualizarPropio()))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class).hasMessageContaining(
                            "LaboratoryTestType not found: " + LaboratoryTestTypeMother.TYPE_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("empresa inexistente")
    class EmpresaInexistente {

        @Test
        @DisplayName("no guarda nada si la nueva empresa del command no existe")
        void no_guarda_nada_si_la_empresa_no_existe() {
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.propioDeEmpresa()));
            when(companyQueryPort.findById(LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(LaboratoryTestTypeMother.comandoActualizarPropio()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + LaboratoryTestTypeMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("renombrar a un nombre ya usado por otra fila ACTIVA de la empresa no guarda nada")
        void renombrar_a_un_nombre_ya_usado_en_la_empresa_no_guarda_nada() {
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.propioDeEmpresa()));
            when(companyQueryPort.findById(LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.CLINICA));
            when(repository.existsActiveByNameAndCompanyIdExcludingId("Hemograma completo",
                    LaboratoryTestTypeMother.COMPANY_ID, LaboratoryTestTypeMother.TYPE_ID))
                    .thenReturn(true);

            assertThatThrownBy(
                    () -> service.execute(LaboratoryTestTypeMother.comandoActualizarPropio()))
                    .isInstanceOf(LaboratoryTestTypeNameAlreadyExistsException.class)
                    .hasMessageContaining("Hemograma completo");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("el camino SYSTEM comprueba el choque en el catalogo de plataforma, con companyId nulo")
        void el_camino_system_comprueba_el_choque_en_el_catalogo_de_plataforma() {
            when(repository.findById(LaboratoryTestTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.general()));
            when(repository.existsActiveByNameAndCompanyIdExcludingId("Perfil renal", null,
                    LaboratoryTestTypeMother.TYPE_ID)).thenReturn(true);

            assertThatThrownBy(
                    () -> service.execute(LaboratoryTestTypeMother.comandoActualizarGeneral()))
                    .isInstanceOf(LaboratoryTestTypeNameAlreadyExistsException.class)
                    .hasMessageContaining("Perfil renal");

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
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
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(LaboratoryTestTypeMother.comandoActualizarPropio()))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
            verify(repository, never()).findById(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se apropia desde una empresa")
        void la_fila_general_no_se_apropia_desde_una_empresa() {
            // El finder de ESCRITURA excluye las generales: si se cargaran, el update les
            // pondria el company_id del llamador y dejarian de ser de todos.
            when(repository.findOwnedByIdAndCompanyId(71L, LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new UpdateLaboratoryTestTypeCommand(71L,
                    "Mio ahora", "Robado", LaboratoryTestTypeMother.COMPANY_ID, false)))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("el camino SYSTEM no alcanza una fila PRIVADA: es 404 y no la expropia al catalogo global")
        void el_camino_system_no_alcanza_una_fila_privada() {
            // Espejo del caso de arriba, y la via que #565 dejo viva: desde que el
            // controller pasa currentCompanyIdOrNull(), la rama companyId == null es
            // alcanzable de verdad por HTTP. Sin el .filter(isGeneral) un PUT de
            // plataforma con el id de una fila PRIVADA la cargaba, y el update posterior
            // le ponia company = null y general = true -la consola manda general: true
            // fijo-, asi que el tipo de una clinica pasaba EN SILENCIO al catalogo
            // global y quedaba visible para todos los tenants. 404 y no 403: no se
            // revela de quien es la fila.
            when(repository.findById(LaboratoryTestTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.propioDeEmpresa()));

            assertThatThrownBy(
                    () -> service.execute(LaboratoryTestTypeMother.comandoActualizarGeneral()))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class).hasMessageContaining(
                            "LaboratoryTestType not found: " + LaboratoryTestTypeMother.TYPE_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }
    }
}
