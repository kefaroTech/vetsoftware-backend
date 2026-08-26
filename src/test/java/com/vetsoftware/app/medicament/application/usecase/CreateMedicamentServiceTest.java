package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.command.CreateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentNameAlreadyExistsException;
import com.vetsoftware.app.medicament.testsupport.MedicamentMother;
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
 * El alta de un medicamento tiene tres desenlaces y no dos: inserta, reactiva o
 * choca. La rama de en medio es la que documento #432 —el nombre lo ocupaba una
 * fila PAUSADA, invisible en el catalogo activo por el
 * {@code @SQLRestriction("enabled = true")}, y el sistema hablaba de un
 * conflicto con algo que la clinica no podia ver— y lo que estos casos fijan es
 * que reactivar NO pasa por {@code save}: va por el UPDATE nativo, el unico que
 * alcanza una fila que el filtro oculta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateMedicamentService")
class CreateMedicamentServiceTest {

    private static final CompanyRef COMPANY = MedicamentMother.CLINICA;
    private static final Long COMPANY_ID = MedicamentMother.COMPANY_ID;

    @Mock
    private MedicamentRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateMedicamentService service;

    @Nested
    @DisplayName("Creacion — el nombre esta libre en el ambito")
    class Creacion {

        @Test
        @DisplayName("resuelve la empresa y persiste el medicamento propio")
        void resuelve_la_empresa_y_persiste() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(COMPANY));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Suero", COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MedicamentDto dto = service.execute(
                    new CreateMedicamentCommand("Suero", "Formula propia", COMPANY_ID, false));

            assertThat(dto.name()).isEqualTo("Suero");
            assertThat(dto.company().id()).isEqualTo(COMPANY_ID);
            ArgumentCaptor<Medicament> guardado = ArgumentCaptor.forClass(Medicament.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompany()).isEqualTo(COMPANY);
            assertThat(guardado.getValue().getId()).isNull();
            verify(repository, never()).reactivateWithDetails(any(), any(), any(), any());
        }

        /**
         * El {@code companyId} nulo del command es el que viaja a la consulta de
         * unicidad: es lo que hace que el alta global mire el vademecum de plataforma y
         * no el catalogo de una empresa. Con STRICT_STUBS la firma del stub es la
         * asercion — llamarla con otro ambito rompe el test.
         */
        @Test
        @DisplayName("el alta global no consulta la empresa y busca el nombre en el ambito nulo")
        void alta_global_no_consulta_la_empresa() {
            when(repository.findByNameAndCompanyIdIncludingDisabled("Amoxicilina", null))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MedicamentDto dto = service
                    .execute(new CreateMedicamentCommand("Amoxicilina", null, null, true));

            assertThat(dto.company()).isNull();
            assertThat(dto.general()).isTrue();
            verify(companyQueryPort, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Reactivacion — el nombre lo ocupa una fila pausada")
    class Reactivacion {

        @Test
        @DisplayName("reactiva la fila pausada de la empresa con la descripcion NUEVA, sin insertar otra")
        void reactiva_la_fila_pausada_de_la_empresa() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(COMPANY));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Suero", COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentMother.pausadoDeEmpresa()));
            when(repository.reactivateWithDetails(MedicamentMother.MEDICAMENT_ID, COMPANY_ID,
                    "Suero", "Formula revisada")).thenReturn(1);

            MedicamentDto dto = service.execute(
                    new CreateMedicamentCommand("Suero", "Formula revisada", COMPANY_ID, false));

            verify(repository).reactivateWithDetails(MedicamentMother.MEDICAMENT_ID, COMPANY_ID,
                    "Suero", "Formula revisada");
            verify(repository, never()).save(any());
            assertThat(dto.id()).isEqualTo(MedicamentMother.MEDICAMENT_ID);
            assertThat(dto.enabled()).isTrue();
            // Vuelve con lo que la usuaria acaba de escribir, no con lo que tenia el dia
            // que se pauso.
            assertThat(dto.description()).isEqualTo("Formula revisada");
            assertThat(dto.company().id()).isEqualTo(COMPANY_ID);
        }

        /**
         * El ambito global viaja como {@code null} hasta el UPDATE nativo, que es lo
         * que lo traduce a {@code company_id IS NULL}. Un {@code = NULL} no casa nunca
         * en SQL, asi que si este argumento se perdiera el vademecum de plataforma se
         * quedaria sin reactivacion en silencio.
         */
        @Test
        @DisplayName("reactiva la fila pausada del vademecum de plataforma con ambito nulo")
        void reactiva_la_fila_pausada_global() {
            when(repository.findByNameAndCompanyIdIncludingDisabled("Amoxicilina", null))
                    .thenReturn(Optional.of(MedicamentMother.pausadoGeneral()));
            when(repository.reactivateWithDetails(MedicamentMother.MEDICAMENT_ID, null,
                    "Amoxicilina", "Antibiotico revisado")).thenReturn(1);

            MedicamentDto dto = service.execute(
                    new CreateMedicamentCommand("Amoxicilina", "Antibiotico revisado", null, true));

            verify(repository).reactivateWithDetails(MedicamentMother.MEDICAMENT_ID, null,
                    "Amoxicilina", "Antibiotico revisado");
            verify(repository, never()).save(any());
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.company()).isNull();
            assertThat(dto.description()).isEqualTo("Antibiotico revisado");
        }

        /**
         * La fila estaba ahi cuando se leyo, en ESTA misma transaccion: que el UPDATE
         * no alcance ninguna significa que otra operacion la borro o le cambio el dueno
         * entre medias. Devolver el DTO igualmente afirmaria un {@code enabled = true}
         * que no esta en la base, y con baja logica ese fallo silencioso es
         * dificilisimo de ver: el catalogo sigue sin el medicamento y el formulario
         * dice que se creo.
         */
        @Test
        @DisplayName("si el UPDATE no alcanza ninguna fila, falla como conflicto y no devuelve DTO")
        void update_sin_filas_afectadas_falla_como_conflicto() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(COMPANY));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Suero", COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentMother.pausadoDeEmpresa()));
            when(repository.reactivateWithDetails(MedicamentMother.MEDICAMENT_ID, COMPANY_ID,
                    "Suero", "Formula revisada")).thenReturn(0);

            assertThatThrownBy(() -> service.execute(
                    new CreateMedicamentCommand("Suero", "Formula revisada", COMPANY_ID, false)))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining(String.valueOf(MedicamentMother.MEDICAMENT_ID));

            verify(repository, never()).save(any());
        }

        /**
         * El {@code update} del dominio va ANTES del UPDATE nativo a proposito: si el
         * alta es incoherente con el XOR general/empresa, aborta sin haber resucitado
         * nada. Una fila pausada que se resucitara y luego fallara la validacion
         * quedaria visible en el catalogo con los datos viejos.
         */
        @Test
        @DisplayName("un alta incoherente aborta sin resucitar la fila pausada")
        void alta_incoherente_no_resucita_la_fila() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(COMPANY));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Suero", COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentMother.pausadoDeEmpresa()));

            assertThatThrownBy(() -> service.execute(
                    new CreateMedicamentCommand("Suero", "Formula revisada", COMPANY_ID, true)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("general medicament cannot have company");

            verify(repository, never()).reactivateWithDetails(any(), any(), any(), any());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones — el nombre lo ocupa una fila activa")
    class Validaciones {

        @Test
        @DisplayName("choca con el nombre ocupado por una fila activa de la empresa y no escribe nada")
        void choca_con_el_nombre_ocupado_por_una_fila_activa() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(COMPANY));
            when(repository.findByNameAndCompanyIdIncludingDisabled("Suero", COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentMother.activoDeEmpresa()));

            assertThatThrownBy(() -> service.execute(
                    new CreateMedicamentCommand("Suero", "Otra formula", COMPANY_ID, false)))
                    .isInstanceOf(MedicamentNameAlreadyExistsException.class)
                    .hasMessageContaining("Suero");

            verify(repository, never()).save(any());
            verify(repository, never()).reactivateWithDetails(any(), any(), any(), any());
        }

        @Test
        @DisplayName("choca tambien en el vademecum de plataforma")
        void choca_en_el_vademecum_de_plataforma() {
            when(repository.findByNameAndCompanyIdIncludingDisabled("Amoxicilina", null))
                    .thenReturn(Optional.of(MedicamentMother.activoGeneral()));

            assertThatThrownBy(() -> service
                    .execute(new CreateMedicamentCommand("Amoxicilina", "Otro", null, true)))
                    .isInstanceOf(MedicamentNameAlreadyExistsException.class)
                    .hasMessageContaining("Amoxicilina");

            verify(repository, never()).save(any());
            verify(repository, never()).reactivateWithDetails(any(), any(), any(), any());
        }

        @Test
        @DisplayName("no toca el repositorio si la empresa no existe")
        void no_guarda_si_la_empresa_no_existe() {
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new CreateMedicamentCommand("Suero", null, COMPANY_ID, false)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + COMPANY_ID);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("companyId nulo con general=false viola la invariante del dominio")
        void companyid_nulo_con_general_false_viola_la_invariante() {
            when(repository.findByNameAndCompanyIdIncludingDisabled("Suero", null))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new CreateMedicamentCommand("Suero", null, null, false)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-general medicament requires company");

            verify(repository, never()).save(any());
        }
    }
}
