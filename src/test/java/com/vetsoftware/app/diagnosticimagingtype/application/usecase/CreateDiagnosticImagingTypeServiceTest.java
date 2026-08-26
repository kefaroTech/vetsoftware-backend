package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * El alta ya no es un {@code save} pelado: comprueba que el nombre este libre
 * DENTRO DE SU AMBITO —la empresa para un tipo propio, el catalogo de
 * plataforma para uno global— y reactiva la fila dada de baja que lo ocupaba en
 * vez de insertar otra (#559).
 *
 * <p>
 * <b>Sobre el ambito de la busqueda.</b> Los stubs de
 * {@code findByNameAndCompanyIdIncludingDisabled} nombran el {@code companyId}
 * exacto a proposito: con {@code STRICT_STUBS}, si el servicio la llamara con
 * otro ambito —la empresa donde toca el catalogo global, o al reves— Mockito
 * levanta {@code PotentialStubbingProblem} y el caso falla. El stub ES la
 * asercion del ambito.
 */
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
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("un tipo general con el nombre libre se persiste sin consultar el puerto de empresa")
        void un_tipo_general_con_nombre_libre_se_persiste() {
            // companyId nulo en el stub: es el ambito de plataforma. Con STRICT_STUBS,
            // buscar el ocupante en una empresa haria fallar este caso.
            when(repository.findByNameAndCompanyIdIncludingDisabled("Radiografia", null))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DiagnosticImagingTypeDto dto = service
                    .execute(DiagnosticImagingTypeMother.comandoCrearGeneral());

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCompany()).isNull();
            assertThat(captor.getValue().isGeneral()).isTrue();
            assertThat(captor.getValue().getId()).isNull();
            assertThat(dto.general()).isTrue();
            assertThat(dto.company()).isNull();
            verifyNoInteractions(companyQueryPort);
        }

        @Test
        @DisplayName("un tipo propio de empresa resuelve la empresa con el puerto y la asocia al tipo guardado")
        void un_tipo_propio_resuelve_la_empresa_y_la_asocia() {
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.EMPRESA));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Ecografia abdominal",
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DiagnosticImagingTypeDto dto = service
                    .execute(DiagnosticImagingTypeMother.comandoCrearDeEmpresa());

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCompany())
                    .isEqualTo(DiagnosticImagingTypeMother.EMPRESA);
            assertThat(captor.getValue().isGeneral()).isFalse();
            assertThat(dto.company().id()).isEqualTo(DiagnosticImagingTypeMother.COMPANY_ID);
            verify(repository, never()).reactivateWithDetails(any(), any(), anyString(),
                    anyString());
        }
    }

    @Nested
    @DisplayName("Reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("el nombre ocupado por una fila deshabilitada la reactiva con los detalles nuevos, sin insertar otra")
        void el_nombre_ocupado_por_una_fila_deshabilitada_la_reactiva() {
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.EMPRESA));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Tomografia",
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.deshabilitada()));
            when(repository.reactivateWithDetails(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID, "Tomografia",
                    DiagnosticImagingTypeMother.DESCRIPCION_NUEVA)).thenReturn(1);

            DiagnosticImagingTypeDto dto = service
                    .execute(DiagnosticImagingTypeMother.comandoCrearTomografia());

            verify(repository).reactivateWithDetails(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID, "Tomografia",
                    DiagnosticImagingTypeMother.DESCRIPCION_NUEVA);
            // Insertar seria un nombre duplicado contra el indice unico de la base: la
            // baja logica libera el nombre, no borra la fila.
            verify(repository, never()).save(any());
            assertThat(dto.id()).isEqualTo(DiagnosticImagingTypeMother.TYPE_ID);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.description()).isEqualTo(DiagnosticImagingTypeMother.DESCRIPCION_NUEVA);
        }

        @Test
        @DisplayName("la fila global deshabilitada se reactiva en el ambito de plataforma, con companyId nulo")
        void la_fila_global_deshabilitada_se_reactiva_en_el_ambito_de_plataforma() {
            when(repository.findByNameAndCompanyIdIncludingDisabled("Radiografia", null))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.generalDeshabilitada()));
            when(repository.reactivateWithDetails(DiagnosticImagingTypeMother.TYPE_ID, null,
                    "Radiografia", DiagnosticImagingTypeMother.DESCRIPCION_GENERAL_NUEVA))
                    .thenReturn(1);

            DiagnosticImagingTypeDto dto = service
                    .execute(DiagnosticImagingTypeMother.comandoCrearGeneralConDescripcionNueva());

            // El companyId nulo es lo que hace que el UPDATE nativo lleve
            // "company_id IS NULL" y no pueda alcanzar la fila privada de un tenant.
            verify(repository).reactivateWithDetails(DiagnosticImagingTypeMother.TYPE_ID, null,
                    "Radiografia", DiagnosticImagingTypeMother.DESCRIPCION_GENERAL_NUEVA);
            verify(repository, never()).save(any());
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.company()).isNull();
            assertThat(dto.description())
                    .isEqualTo(DiagnosticImagingTypeMother.DESCRIPCION_GENERAL_NUEVA);
            verifyNoInteractions(companyQueryPort);
        }

        @Test
        @DisplayName("si el UPDATE de reactivacion no alcanza ninguna fila, falla como conflicto y no devuelve DTO")
        void si_la_reactivacion_no_alcanza_ninguna_fila_falla_como_conflicto() {
            // La fila estaba ahi cuando la leimos, en la MISMA transaccion: cero filas
            // afectadas significa que otra operacion la borro o le cambio el dueno entre
            // medias. Devolver el DTO afirmaria un enabled = true que no esta en la base,
            // y con baja logica ese fallo silencioso es dificilisimo de ver despues.
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.EMPRESA));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Tomografia",
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.deshabilitada()));
            when(repository.reactivateWithDetails(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID, "Tomografia",
                    DiagnosticImagingTypeMother.DESCRIPCION_NUEVA)).thenReturn(0);

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingTypeMother.comandoCrearTomografia()))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("DiagnosticImagingType");

            // El handler lo mapea a 409 CONCURRENT_MODIFICATION: al cliente le sirve
            // "recarga y reintenta", que es la misma accion que ante el candado optimista.
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("el nombre ocupado por una fila ACTIVA es un conflicto y no escribe nada")
        void el_nombre_ocupado_por_una_fila_activa_es_conflicto() {
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.EMPRESA));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Ecografia abdominal",
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.propiaDeEmpresa()));

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingTypeMother.comandoCrearDeEmpresa()))
                    .isInstanceOf(DiagnosticImagingTypeNameAlreadyExistsException.class)
                    .hasMessageContaining("Ecografia abdominal");

            verify(repository, never()).save(any());
            verify(repository, never()).reactivateWithDetails(any(), any(), anyString(),
                    anyString());
        }

        @Test
        @DisplayName("un alta incoherente con la fila deshabilitada aborta antes de resucitarla")
        void un_alta_incoherente_aborta_antes_de_resucitar_la_fila() {
            // El XOR del dominio lo valida el update() ANTES del UPDATE nativo: si el
            // orden se invirtiera, la fila quedaria reactivada y con datos invalidos.
            when(companyQueryPort.findById(DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.EMPRESA));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Tomografia",
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.deshabilitada()));

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingTypeMother.comandoCrearIncoherente()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("general type cannot have company");

            verify(repository, never()).reactivateWithDetails(any(), any(), anyString(),
                    anyString());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no persiste nada si la empresa del command no existe")
        void no_persiste_nada_si_la_empresa_no_existe() {
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
