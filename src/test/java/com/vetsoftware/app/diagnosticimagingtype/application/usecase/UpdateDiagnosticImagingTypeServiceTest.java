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
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNameAlreadyExistsException;
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
            // La guarda de unicidad se consulta en el ambito de la EMPRESA del command
            // y excluyendo la propia fila: renombrarse a si misma no es un choque.
            when(repository.existsActiveByNameAndCompanyIdExcludingId(
                    "Ecografia abdominal (actualizada)", DiagnosticImagingTypeMother.COMPANY_ID,
                    DiagnosticImagingTypeMother.TYPE_ID)).thenReturn(false);
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
            // La guarda de unicidad se consulta en el ambito de la EMPRESA del command
            // y excluyendo la propia fila: renombrarse a si misma no es un choque.
            when(repository.existsActiveByNameAndCompanyIdExcludingId(
                    "Ecografia abdominal (actualizada)", DiagnosticImagingTypeMother.COMPANY_ID,
                    DiagnosticImagingTypeMother.TYPE_ID)).thenReturn(false);
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
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("renombrar a un nombre ya usado por otra fila ACTIVA de la empresa no guarda nada")
        void renombrar_a_un_nombre_ya_usado_en_la_empresa_no_guarda_nada() {
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.propiaDeEmpresa()));
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.EMPRESA));
            when(repository.existsActiveByNameAndCompanyIdExcludingId(
                    "Ecografia abdominal (actualizada)", DiagnosticImagingTypeMother.COMPANY_ID,
                    DiagnosticImagingTypeMother.TYPE_ID)).thenReturn(true);

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingTypeMother.comandoActualizar()))
                    .isInstanceOf(DiagnosticImagingTypeNameAlreadyExistsException.class)
                    .hasMessageContaining("Ecografia abdominal (actualizada)");

            verify(repository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("el camino SYSTEM comprueba el choque en el catalogo de plataforma, con companyId nulo")
        void el_camino_system_comprueba_el_choque_en_el_catalogo_de_plataforma() {
            // Con STRICT_STUBS, preguntar la guarda con una empresa en vez de con null
            // levanta PotentialStubbingProblem: el stub ES la asercion del ambito.
            when(repository.findById(DiagnosticImagingTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.general()));
            when(repository.existsActiveByNameAndCompanyIdExcludingId("Radiografia", null,
                    DiagnosticImagingTypeMother.TYPE_ID)).thenReturn(true);

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingTypeMother.comandoActualizarGeneral()))
                    .isInstanceOf(DiagnosticImagingTypeNameAlreadyExistsException.class)
                    .hasMessageContaining("Radiografia");

            verifyNoInteractions(companyQueryPort);
            verify(repository, org.mockito.Mockito.never()).save(any());
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
            when(repository.findById(DiagnosticImagingTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.propiaDeEmpresa()));

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingTypeMother.comandoActualizarGeneral()))
                    .isInstanceOf(
                            com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException.class)
                    .hasMessageContaining("DiagnosticImagingType not found: "
                            + DiagnosticImagingTypeMother.TYPE_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, org.mockito.Mockito.never()).save(any());
        }
    }
}
