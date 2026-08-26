package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.command.UpdateGlobalMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentNameAlreadyExistsException;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
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

/**
 * Edicion en el vademecum de PLATAFORMA.
 *
 * <p>
 * El {@code filter(Medicament::isGeneral)} de este servicio no es defensa en
 * profundidad: es LA barrera. El id lo escribe el cliente en la URL y este
 * puerto no recibe empresa, asi que sin el filtro un PUT de plataforma con el
 * id de una fila PRIVADA la cargaria y el {@code update} posterior le pondria
 * {@code company = null} y {@code general = true} — el medicamento de una
 * clinica pasando en silencio al catalogo global, visible para todos los
 * tenants. Ese es el caso de {@code Tenancy}, y es el que da valor a esta
 * clase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateGlobalMedicamentService")
class UpdateGlobalMedicamentServiceTest {

    private static final Long ID = MedicamentMother.MEDICAMENT_ID;

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private UpdateGlobalMedicamentService service;

    @Nested
    @DisplayName("Edicion")
    class Edicion {

        @Test
        @DisplayName("cambia nombre y descripcion reafirmando empresa nula y general=true")
        void cambia_nombre_y_descripcion_reafirmando_el_ambito_global() {
            when(repository.findById(ID)).thenReturn(Optional.of(MedicamentMother.activoGeneral()));
            when(repository.existsActiveByNameAndCompanyIdExcludingId("Amoxicilina trihidrato",
                    null, ID)).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MedicamentDto dto = service.execute(
                    new UpdateGlobalMedicamentCommand(ID, "Amoxicilina trihidrato", "Revisado"));

            ArgumentCaptor<Medicament> guardado = ArgumentCaptor.forClass(Medicament.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompany()).isNull();
            assertThat(guardado.getValue().isGeneral()).isTrue();
            assertThat(guardado.getValue().getName()).isEqualTo("Amoxicilina trihidrato");
            assertThat(guardado.getValue().getDescription()).isEqualTo("Revisado");
            assertThat(dto.company()).isNull();
            assertThat(dto.general()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validaciones — no debe escribir")
    class Validaciones {

        @Test
        @DisplayName("lanza MedicamentNotFoundException si el medicamento no existe")
        void lanza_not_found_si_no_existe() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new UpdateGlobalMedicamentCommand(ID, "Lo que sea", "x")))
                    .isInstanceOf(MedicamentNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).save(any());
            verify(repository, never()).existsActiveByNameAndCompanyIdExcludingId(any(), any(),
                    any());
        }

        /**
         * La unicidad se comprueba contra el vademecum de PLATAFORMA —ambito nulo—, que
         * es donde la fila vive y donde va a seguir viviendo. El stub exacto con
         * {@code null} mas STRICT_STUBS es lo que lo afirma: con cualquier otro ambito
         * el stub quedaria sin usar y Mockito rompe el test.
         */
        @Test
        @DisplayName("un nombre ya ocupado en el vademecum global choca y no muta la fila cargada")
        void nombre_ocupado_en_el_vademecum_global_choca() {
            Medicament existente = MedicamentMother.activoGeneral();
            when(repository.findById(ID)).thenReturn(Optional.of(existente));
            when(repository.existsActiveByNameAndCompanyIdExcludingId("Ampicilina", null, ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service
                    .execute(new UpdateGlobalMedicamentCommand(ID, "Ampicilina", "Revisado")))
                    .isInstanceOf(MedicamentNameAlreadyExistsException.class)
                    .hasMessageContaining("Ampicilina");

            verify(repository, never()).save(any());
            // La guarda va ANTES del update del dominio: el caso de uso corre
            // @Transactional y una entidad gestionada mutada se volcaria en el flush
            // aunque nadie llamara a save.
            assertThat(existente.getName()).isEqualTo("Amoxicilina");
            assertThat(existente.getDescription()).isEqualTo("Antibiotico");
        }
    }

    @Nested
    @DisplayName("Tenancy — la consola de plataforma no toca el vademecum de una clinica")
    class Tenancy {

        /**
         * El caso de #590 en su version mas fea para este servicio: sin el filtro, el
         * medicamento privado de una clinica se convertiria en global y todos los
         * tenants pasarian a verlo. Un 404 y no un 403: no se revela de quien es la
         * fila.
         */
        @Test
        @DisplayName("el medicamento PRIVADO de una clinica no se carga, no se edita y no se hace global")
        void el_medicamento_privado_de_una_clinica_no_se_toca() {
            Medicament deEmpresa = MedicamentMother.activoDeEmpresa();
            when(repository.findById(ID)).thenReturn(Optional.of(deEmpresa));

            assertThatThrownBy(() -> service
                    .execute(new UpdateGlobalMedicamentCommand(ID, "Robado", "Robado")))
                    .isInstanceOf(MedicamentNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).save(any());
            verify(repository, never()).existsActiveByNameAndCompanyIdExcludingId(any(), any(),
                    any());
            // Ni siquiera en memoria: la fila conserva su empresa y su general=false.
            assertThat(deEmpresa.getCompany()).isEqualTo(MedicamentMother.CLINICA);
            assertThat(deEmpresa.isGeneral()).isFalse();
            assertThat(deEmpresa.getName()).isEqualTo("Suero");
        }
    }
}
