package com.vetsoftware.app.surgerytype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgerytype.application.command.CreateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNameAlreadyExistsException;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * El alta ya no es un {@code save} a ciegas: antes de insertar mira quien ocupa
 * el nombre DENTRO DE SU AMBITO —la empresa del command, o el catalogo de
 * plataforma cuando el {@code companyId} es nulo— y decide entre tres
 * desenlaces (#559).
 *
 * <p>
 * Lo que estos casos vigilan y no se ve leyendo el servicio:
 *
 * <ul>
 * <li>que el ocupante ACTIVO aborte el alta <b>sin llamar a {@code save}</b>:
 * si llegara a la base, el 409 lo daria la constraint en ingles y sin errorCode
 * de negocio, que es exactamente el defecto que se arreglo;</li>
 * <li>que el ocupante DESHABILITADO se reactive con los datos NUEVOS —la baja
 * logica libera el nombre, asi que insertar otra fila o fallar serian los dos
 * incorrectos—;</li>
 * <li>que el {@code update} del dominio corra ANTES del UPDATE nativo: si se
 * invirtiera el orden, un alta incoherente dejaria una fila resucitada y rota
 * antes de que el XOR la rechazara.</li>
 * </ul>
 */
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

    /** Nadie ocupa el nombre en ese ambito: el alta cae en la rama del insert. */
    private void nombreLibreEn(String nombre, Long companyId) {
        when(repository.findByNameAndCompanyIdIncludingDisabled(nombre, companyId))
                .thenReturn(Optional.empty());
    }

    private void nombreOcupadoPor(String nombre, Long companyId, SurgeryType ocupante) {
        when(repository.findByNameAndCompanyIdIncludingDisabled(nombre, companyId))
                .thenReturn(Optional.of(ocupante));
    }

    private void laEmpresaExiste() {
        when(companyQueryPort.findById(SurgeryTypeMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryTypeMother.EMPRESA));
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("con el nombre libre crea un tipo propio con la empresa resuelta por el puerto")
        void con_el_nombre_libre_crea_un_tipo_propio() {
            laEmpresaExiste();
            nombreLibreEn("Castracion", SurgeryTypeMother.COMPANY_ID);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SurgeryTypeDto dto = service.execute(SurgeryTypeMother.comandoCrearPropio());

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCompany()).isEqualTo(SurgeryTypeMother.EMPRESA);
            assertThat(captor.getValue().getId()).isNull();
            assertThat(dto.name()).isEqualTo("Castracion");
            assertThat(dto.general()).isFalse();
            verify(repository, never()).reactivateWithDetails(any(), any(), any(), any());
        }

        @Test
        @DisplayName("el alta global busca al ocupante con companyId nulo y no consulta el puerto de empresa")
        void el_alta_global_busca_en_el_ambito_de_plataforma() {
            // Las dos mitades del arreglo de #565 en el service: sin empresa que
            // resolver, y la guarda de nombre acotada al catalogo de plataforma. Si la
            // busqueda se hiciera con una empresa, el nombre global quedaria sin guarda y
            // el choque volveria a detectarlo solo la constraint.
            nombreLibreEn("Cirugia general", null);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SurgeryTypeDto dto = service.execute(SurgeryTypeMother.comandoCrearGeneral());

            verifyNoInteractions(companyQueryPort);
            verify(repository).findByNameAndCompanyIdIncludingDisabled("Cirugia general", null);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCompany()).isNull();
            assertThat(dto.general()).isTrue();
        }
    }

    @Nested
    @DisplayName("Reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("el nombre ocupado por una fila dada de baja la reactiva con los datos nuevos")
        void el_nombre_ocupado_por_una_fila_de_baja_se_reactiva() {
            laEmpresaExiste();
            nombreOcupadoPor("Castracion", SurgeryTypeMother.COMPANY_ID,
                    SurgeryTypeMother.deshabilitado());
            when(repository.reactivateWithDetails(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID, "Castracion", "Descripcion nueva")).thenReturn(1);

            SurgeryTypeDto dto = service.execute(new CreateSurgeryTypeCommand("Castracion",
                    "Descripcion nueva", SurgeryTypeMother.COMPANY_ID, false));

            // La fila vuelve con lo que la usuaria acaba de escribir, no con lo que
            // tenia el dia que se dio de baja.
            verify(repository).reactivateWithDetails(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID, "Castracion", "Descripcion nueva");
            // Un save aqui insertaria una SEGUNDA fila con el mismo nombre y la
            // constraint lo rechazaria: la reactivacion es un UPDATE, no un alta.
            verify(repository, never()).save(any());
            assertThat(dto.id()).isEqualTo(SurgeryTypeMother.SURGERY_TYPE_ID);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.description()).isEqualTo("Descripcion nueva");
        }

        @Test
        @DisplayName("la reactivacion global va al catalogo de plataforma, con companyId nulo")
        void la_reactivacion_global_va_al_catalogo_de_plataforma() {
            nombreOcupadoPor("Cirugia general", null, SurgeryTypeMother.generalDeshabilitado());
            when(repository.reactivateWithDetails(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID, null,
                    "Cirugia general", "Descripcion nueva")).thenReturn(1);

            SurgeryTypeDto dto = service.execute(new CreateSurgeryTypeCommand("Cirugia general",
                    "Descripcion nueva", null, true));

            verifyNoInteractions(companyQueryPort);
            // El companyId nulo llega hasta el UPDATE: es lo que hace que el statement
            // acote por «no tiene empresa» y no alcance la fila privada de un tenant.
            verify(repository).reactivateWithDetails(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID,
                    null, "Cirugia general", "Descripcion nueva");
            verify(repository, never()).save(any());
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.general()).isTrue();
            assertThat(dto.company()).isNull();
            assertThat(dto.description()).isEqualTo("Descripcion nueva");
        }

        @Test
        @DisplayName("si el UPDATE no alcanza ninguna fila lanza conflicto en vez de mentir")
        void si_el_update_no_alcanza_ninguna_fila_lanza_conflicto() {
            // La fila estaba ahi cuando la leimos: cero filas significa que otra
            // operacion la borro o le cambio el dueno entre medias. Devolver el DTO
            // igualmente afirmaria un enabled = true que la base no tiene, y con baja
            // logica ese embuste no se ve hasta que alguien busca el tipo y no esta.
            laEmpresaExiste();
            nombreOcupadoPor("Castracion", SurgeryTypeMother.COMPANY_ID,
                    SurgeryTypeMother.deshabilitado());
            when(repository.reactivateWithDetails(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID, "Castracion", "Descripcion nueva")).thenReturn(0);

            assertThatThrownBy(() -> service.execute(new CreateSurgeryTypeCommand("Castracion",
                    "Descripcion nueva", SurgeryTypeMother.COMPANY_ID, false)))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

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

        @Test
        @DisplayName("el nombre ocupado por una fila ACTIVA lanza y no llega a guardar")
        void el_nombre_ocupado_por_una_fila_activa_lanza_y_no_guarda() {
            laEmpresaExiste();
            nombreOcupadoPor("Castracion", SurgeryTypeMother.COMPANY_ID,
                    SurgeryTypeMother.propioDeEmpresa());

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.comandoCrearPropio()))
                    .isInstanceOf(SurgeryTypeNameAlreadyExistsException.class)
                    .hasMessageContaining("Castracion");

            verify(repository, never()).save(any());
            verify(repository, never()).reactivateWithDetails(any(), any(), any(), any());
        }

        @Test
        @DisplayName("una fila de baja incoherente con el command aborta ANTES de tocar la base")
        void una_fila_de_baja_incoherente_aborta_antes_de_tocar_la_base() {
            // El command trae empresa Y general = true: el XOR del dominio lo rechaza.
            // Como el update del dominio corre antes del UPDATE nativo, la fila sigue
            // dada de baja; si el orden se invirtiera quedaria resucitada y rota.
            laEmpresaExiste();
            nombreOcupadoPor("Castracion", SurgeryTypeMother.COMPANY_ID,
                    SurgeryTypeMother.deshabilitado());

            assertThatThrownBy(() -> service.execute(new CreateSurgeryTypeCommand("Castracion",
                    "Cirugia de esterilizacion", SurgeryTypeMother.COMPANY_ID, true)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("general type cannot have company");

            verify(repository, never()).reactivateWithDetails(any(), any(), any(), any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un tipo general con compania resuelta lanza y no guarda")
        void un_tipo_general_con_compania_resuelta_lanza_y_no_guarda() {
            laEmpresaExiste();
            nombreLibreEn("Cirugia general", SurgeryTypeMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(new CreateSurgeryTypeCommand("Cirugia general",
                    "Procedimiento estandar", SurgeryTypeMother.COMPANY_ID, true)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("general type cannot have company");

            verify(repository, never()).save(any());
        }
    }
}
