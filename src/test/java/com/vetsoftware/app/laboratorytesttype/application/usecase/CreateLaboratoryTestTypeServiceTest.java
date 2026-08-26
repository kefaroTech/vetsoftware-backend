package com.vetsoftware.app.laboratorytesttype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNameAlreadyExistsException;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * El alta ya no es un {@code save} pelado: comprueba que el nombre este libre
 * DENTRO DE SU AMBITO —la empresa para un tipo propio, el catalogo de
 * plataforma para uno global— y reactiva la fila dada de baja que lo ocupaba en
 * vez de insertar otra (#559).
 *
 * <p>
 * Las tres ramas del flujo se prueban por separado porque tienen efectos
 * distintos: insertar, reactivar sin insertar, y no escribir nada.
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
@DisplayName("CreateLaboratoryTestTypeService")
class CreateLaboratoryTestTypeServiceTest {

    @Mock
    private LaboratoryTestTypeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateLaboratoryTestTypeService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("un tipo propio de empresa con el nombre libre resuelve la company por el puerto y la persiste")
        void un_tipo_propio_con_nombre_libre_se_persiste() {
            when(companyQueryPort.findById(LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.CLINICA));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Hemograma",
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LaboratoryTestTypeDto dto = service
                    .execute(LaboratoryTestTypeMother.comandoCrearPropio());

            ArgumentCaptor<LaboratoryTestType> guardado = ArgumentCaptor
                    .forClass(LaboratoryTestType.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompany())
                    .isEqualTo(LaboratoryTestTypeMother.CLINICA);
            assertThat(guardado.getValue().isGeneral()).isFalse();
            assertThat(guardado.getValue().getId()).isNull();
            assertThat(dto.company().id()).isEqualTo(LaboratoryTestTypeMother.COMPANY_ID);
            verify(repository, never()).reactivateWithDetails(any(), any(), anyString(),
                    anyString());
        }

        @Test
        @DisplayName("un tipo general busca el ocupante en el catalogo de plataforma y no consulta el puerto de empresas")
        void un_tipo_general_busca_en_el_ambito_global() {
            // companyId nulo en el stub: es el ambito de plataforma. Con STRICT_STUBS,
            // buscar el ocupante en una empresa haria fallar este caso.
            when(repository.findByNameAndCompanyIdIncludingDisabled("Perfil renal", null))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LaboratoryTestTypeDto dto = service
                    .execute(LaboratoryTestTypeMother.comandoCrearGeneral());

            ArgumentCaptor<LaboratoryTestType> guardado = ArgumentCaptor
                    .forClass(LaboratoryTestType.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompany()).isNull();
            assertThat(guardado.getValue().isGeneral()).isTrue();
            assertThat(dto.company()).isNull();
            assertThat(dto.general()).isTrue();
            verifyNoInteractions(companyQueryPort);
        }
    }

    @Nested
    @DisplayName("Reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("el nombre ocupado por una fila deshabilitada la reactiva con los detalles nuevos, sin insertar otra")
        void el_nombre_ocupado_por_una_fila_deshabilitada_la_reactiva() {
            when(companyQueryPort.findById(LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.CLINICA));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Hemograma",
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(
                            Optional.of(LaboratoryTestTypeMother.propioDeEmpresaDeshabilitado()));
            when(repository.reactivateWithDetails(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID, "Hemograma",
                    LaboratoryTestTypeMother.DESCRIPCION_NUEVA)).thenReturn(1);

            LaboratoryTestTypeDto dto = service
                    .execute(LaboratoryTestTypeMother.comandoCrearPropioConDescripcionNueva());

            verify(repository).reactivateWithDetails(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID, "Hemograma",
                    LaboratoryTestTypeMother.DESCRIPCION_NUEVA);
            // Insertar seria un nombre duplicado contra el indice unico de la base: la
            // baja logica libera el nombre, no borra la fila.
            verify(repository, never()).save(any());
            assertThat(dto.id()).isEqualTo(LaboratoryTestTypeMother.TYPE_ID);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.description()).isEqualTo(LaboratoryTestTypeMother.DESCRIPCION_NUEVA);
        }

        @Test
        @DisplayName("la fila global deshabilitada se reactiva en el ambito de plataforma, con companyId nulo")
        void la_fila_global_deshabilitada_se_reactiva_en_el_ambito_de_plataforma() {
            when(repository.findByNameAndCompanyIdIncludingDisabled("Perfil renal", null))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.generalDeshabilitado()));
            when(repository.reactivateWithDetails(LaboratoryTestTypeMother.TYPE_ID, null,
                    "Perfil renal", LaboratoryTestTypeMother.DESCRIPCION_GENERAL_NUEVA))
                    .thenReturn(1);

            LaboratoryTestTypeDto dto = service
                    .execute(LaboratoryTestTypeMother.comandoCrearGeneralConDescripcionNueva());

            // El companyId nulo es lo que hace que el UPDATE nativo lleve
            // "company_id IS NULL" y no pueda alcanzar la fila privada de un tenant.
            verify(repository).reactivateWithDetails(LaboratoryTestTypeMother.TYPE_ID, null,
                    "Perfil renal", LaboratoryTestTypeMother.DESCRIPCION_GENERAL_NUEVA);
            verify(repository, never()).save(any());
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.company()).isNull();
            assertThat(dto.description())
                    .isEqualTo(LaboratoryTestTypeMother.DESCRIPCION_GENERAL_NUEVA);
            verifyNoInteractions(companyQueryPort);
        }

        @Test
        @DisplayName("si el UPDATE de reactivacion no alcanza ninguna fila, falla como conflicto y no devuelve DTO")
        void si_la_reactivacion_no_alcanza_ninguna_fila_falla_como_conflicto() {
            // La fila estaba ahi cuando la leimos, en la MISMA transaccion: cero filas
            // afectadas significa que otra operacion la borro o le cambio el dueno entre
            // medias. Devolver el DTO afirmaria un enabled = true que no esta en la base,
            // y con baja logica ese fallo silencioso es dificilisimo de ver despues.
            when(companyQueryPort.findById(LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.CLINICA));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Hemograma",
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(
                            Optional.of(LaboratoryTestTypeMother.propioDeEmpresaDeshabilitado()));
            when(repository.reactivateWithDetails(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID, "Hemograma",
                    LaboratoryTestTypeMother.DESCRIPCION_NUEVA)).thenReturn(0);

            assertThatThrownBy(() -> service
                    .execute(LaboratoryTestTypeMother.comandoCrearPropioConDescripcionNueva()))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("LaboratoryTestType");

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
            when(companyQueryPort.findById(LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.CLINICA));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Hemograma",
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.propioDeEmpresa()));

            assertThatThrownBy(() -> service.execute(LaboratoryTestTypeMother.comandoCrearPropio()))
                    .isInstanceOf(LaboratoryTestTypeNameAlreadyExistsException.class)
                    .hasMessageContaining("Hemograma");

            verify(repository, never()).save(any());
            verify(repository, never()).reactivateWithDetails(any(), any(), anyString(),
                    anyString());
        }

        @Test
        @DisplayName("un alta incoherente con la fila deshabilitada aborta antes de resucitarla")
        void un_alta_incoherente_aborta_antes_de_resucitar_la_fila() {
            // El XOR del dominio lo valida el update() ANTES del UPDATE nativo: si el
            // orden se invirtiera, la fila quedaria reactivada y con datos invalidos.
            when(companyQueryPort.findById(LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.CLINICA));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Hemograma",
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(
                            Optional.of(LaboratoryTestTypeMother.propioDeEmpresaDeshabilitado()));

            assertThatThrownBy(
                    () -> service.execute(LaboratoryTestTypeMother.comandoCrearIncoherente()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("general type cannot have company");

            verify(repository, never()).reactivateWithDetails(any(), any(), anyString(),
                    anyString());
            verify(repository, never()).save(any());
        }

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
