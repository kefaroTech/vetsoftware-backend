package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.command.CreateGlobalMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
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
 * Alta en el vademecum de PLATAFORMA.
 *
 * <p>
 * Lo que hay que demostrar aqui es que el ambito NO es un dato de entrada: el
 * command solo transporta nombre y descripcion, y quien escribe
 * {@code company = null} / {@code general = true} es el caso de uso. El
 * {@code ArgumentCaptor} sobre {@code save} es la unica forma de afirmarlo — un
 * {@code verify(repository).save(any())} pasaria igual con la fila de un tenant
 * dentro.
 *
 * <p>
 * El ambito de la BUSQUEDA previa no se verifica con un {@code verify}: es una
 * consulta. Lo prueba el stub exacto {@code (nombre, null)} mas STRICT_STUBS —
 * si el servicio buscara con otro ambito, el stub quedaria sin usar y Mockito
 * romperia el test por {@code UnnecessaryStubbing}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateGlobalMedicamentService")
class CreateGlobalMedicamentServiceTest {

    private static final String NOMBRE = "Alfamicina";

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private CreateGlobalMedicamentService service;

    @Nested
    @DisplayName("Creacion — el ambito lo pone el servidor")
    class Creacion {

        @Test
        @DisplayName("guarda con empresa nula y general=true, que no vienen del command")
        void guarda_con_empresa_nula_y_general_true() {
            when(repository.findByNameAndCompanyIdIncludingDisabled(NOMBRE, null))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MedicamentDto dto = service
                    .execute(new CreateGlobalMedicamentCommand(NOMBRE, "Antibiotico"));

            ArgumentCaptor<Medicament> guardado = ArgumentCaptor.forClass(Medicament.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCompany()).isNull();
            assertThat(guardado.getValue().isGeneral()).isTrue();
            assertThat(guardado.getValue().getName()).isEqualTo(NOMBRE);
            assertThat(guardado.getValue().getDescription()).isEqualTo("Antibiotico");
            assertThat(dto.company()).isNull();
            assertThat(dto.general()).isTrue();
            assertThat(dto.enabled()).isTrue();
        }

        /**
         * Que la empresa 7 tenga su homonima privada no bloquea el alta global: el
         * indice unico es {@code (owner_scope, active_name)} y son claves distintas.
         * Aqui se ve como el finder acotado al vademecum de plataforma no la encuentra
         * y el alta sigue adelante.
         */
        @Test
        @DisplayName("no ve la homonima privada de una clinica: busca solo el vademecum de plataforma")
        void no_ve_la_homonima_privada_de_una_clinica() {
            when(repository.findByNameAndCompanyIdIncludingDisabled("Suero", null))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MedicamentDto dto = service
                    .execute(new CreateGlobalMedicamentCommand("Suero", "Version de plataforma"));

            assertThat(dto.general()).isTrue();
            assertThat(dto.company()).isNull();
        }
    }

    /**
     * El indice unico de la base cubre solo las filas ACTIVAS, asi que un global
     * pausado NO ocupa su nombre: la respuesta correcta al reencontrarlo es
     * reactivarlo con los datos que la usuaria acaba de escribir, no insertar otra
     * fila ni fallar con un 409 que habla de algo que nadie ve (#432, #559).
     */
    @Nested
    @DisplayName("Reactivacion — el nombre lo ocupaba una fila pausada")
    class Reactivacion {

        @Test
        @DisplayName("reactiva la fila pausada con la descripcion nueva y no inserta otra")
        void reactiva_la_fila_pausada_con_la_descripcion_nueva() {
            Medicament pausado = MedicamentMother.pausadoGeneral();
            when(repository.findByNameAndCompanyIdIncludingDisabled("Amoxicilina", null))
                    .thenReturn(Optional.of(pausado));
            when(repository.reactivateWithDetails(MedicamentMother.MEDICAMENT_ID, null,
                    "Amoxicilina", "Antibiotico revisado")).thenReturn(1);

            MedicamentDto dto = service.execute(
                    new CreateGlobalMedicamentCommand("Amoxicilina", "Antibiotico revisado"));

            assertThat(dto.id()).isEqualTo(MedicamentMother.MEDICAMENT_ID);
            assertThat(dto.description()).isEqualTo("Antibiotico revisado");
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.general()).isTrue();
            assertThat(dto.company()).isNull();
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("si la fila que ocupa el nombre esta ACTIVA choca y no escribe nada")
        void fila_activa_con_el_mismo_nombre_choca() {
            when(repository.findByNameAndCompanyIdIncludingDisabled("Amoxicilina", null))
                    .thenReturn(Optional.of(MedicamentMother.activoGeneral()));

            assertThatThrownBy(() -> service
                    .execute(new CreateGlobalMedicamentCommand("Amoxicilina", "Otra cosa")))
                    .isInstanceOf(MedicamentNameAlreadyExistsException.class)
                    .hasMessageContaining("Amoxicilina");

            verify(repository, never()).save(any());
            verify(repository, never()).reactivateWithDetails(any(), any(), any(), any());
        }

        /**
         * La fila estaba ahi cuando la leimos, en ESTA misma transaccion: cero filas
         * afectadas significa que otra operacion la borro entre medias. Devolver el DTO
         * igualmente afirmaria un {@code enabled = true} que no esta en la base.
         */
        @Test
        @DisplayName("si el UPDATE nativo no alcanza ninguna fila lanza el fallo de concurrencia")
        void update_nativo_sin_filas_es_un_fallo_de_concurrencia() {
            when(repository.findByNameAndCompanyIdIncludingDisabled("Amoxicilina", null))
                    .thenReturn(Optional.of(MedicamentMother.pausadoGeneral()));
            when(repository.reactivateWithDetails(MedicamentMother.MEDICAMENT_ID, null,
                    "Amoxicilina", "Revisado")).thenReturn(0);

            assertThatThrownBy(() -> service
                    .execute(new CreateGlobalMedicamentCommand("Amoxicilina", "Revisado")))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);

            verify(repository, never()).save(any());
        }

        /**
         * El {@code update} del dominio va ANTES del UPDATE nativo a proposito: valida
         * y aborta sin haber resucitado nada. Si el orden se invirtiera, un alta
         * invalida dejaria la fila viva en el catalogo global antes de reventar.
         */
        @Test
        @DisplayName("un alta invalida aborta en el dominio sin resucitar la fila")
        void alta_invalida_aborta_sin_resucitar_la_fila() {
            String nombreLargo = "x".repeat(201);
            when(repository.findByNameAndCompanyIdIncludingDisabled(nombreLargo, null))
                    .thenReturn(Optional.of(MedicamentMother.pausadoGeneral()));

            assertThatThrownBy(
                    () -> service.execute(new CreateGlobalMedicamentCommand(nombreLargo, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("200 chars or less");

            verify(repository, never()).reactivateWithDetails(any(), any(), any(), any());
            verify(repository, never()).save(any());
        }
    }
}
