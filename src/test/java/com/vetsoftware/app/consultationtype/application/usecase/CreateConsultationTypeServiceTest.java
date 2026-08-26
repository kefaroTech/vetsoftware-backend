package com.vetsoftware.app.consultationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultationtype.application.command.CreateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNameAlreadyExistsException;
import com.vetsoftware.app.consultationtype.testsupport.ConsultationTypeMother;
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
 * El alta de un tipo de consulta dejo de ser un {@code save} pelado (#559):
 * ahora mira primero si el nombre esta ocupado y tiene tres desenlaces
 * distintos segun lo que encuentre. Los tres se prueban aqui, y lo que se
 * afirma en cada uno no es solo el resultado sino <b>que no se escribio de
 * mas</b>: el camino que lanza no debe guardar, y el que reactiva no debe
 * insertar una segunda fila.
 *
 * <p>
 * {@code consultation_types} es catalogo global de plataforma —el caso de uso
 * esta cerrado a {@code hasRole('SYSTEM')} y no hay {@code company_id}—, asi
 * que la guarda de nombre no tiene ambito: el ambito es la tabla entera. Por
 * eso no hay ningun caso de tenancy en esta clase, a diferencia de sus gemelas
 * de catalogo por empresa.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateConsultationTypeService — alta de un tipo de consulta")
class CreateConsultationTypeServiceTest {

    private static final String DESCRIPCION_NUEVA = "Descripcion reescrita en el alta";

    @Mock
    private ConsultationTypeRepository repository;

    @InjectMocks
    private CreateConsultationTypeService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("con el nombre libre crea el tipo con los datos del comando y devuelve su dto")
        void crea_el_tipo_con_los_datos_del_comando() {
            when(repository.findByNameIncludingDisabled("Vacunacion")).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(ConsultationTypeMother.consultaGeneral());

            ConsultationTypeDto dto = service.execute(
                    new CreateConsultationTypeCommand("Vacunacion", "Aplicacion de vacunas"));

            assertThat(dto.id()).isEqualTo(ConsultationTypeMother.ID);
            ArgumentCaptor<ConsultationType> captor = ArgumentCaptor
                    .forClass(ConsultationType.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Vacunacion");
            assertThat(captor.getValue().getDescription()).isEqualTo("Aplicacion de vacunas");
            assertThat(captor.getValue().getId()).isNull();
            assertThat(captor.getValue().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("con el nombre libre inserta y no toca la rama de reactivacion")
        void con_el_nombre_libre_no_reactiva_nada() {
            when(repository.findByNameIncludingDisabled("Vacunacion")).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(ConsultationTypeMother.consultaGeneral());

            service.execute(
                    new CreateConsultationTypeCommand("Vacunacion", "Aplicacion de vacunas"));

            verify(repository, never()).reactivateWithDetails(anyLong(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Reactivacion de una fila dada de baja")
    class Reactivacion {

        /**
         * El caso que justifica la rama entera (#432). La baja es logica: la fila
         * desaparece del listado —{@code @SQLRestriction("enabled = true")}— pero sigue
         * en la tabla. Antes de #559 el alta con ese nombre chocaba contra algo que el
         * administrador no ve; ahora resucita la fila y le aplica los datos del
         * comando.
         */
        @Test
        @DisplayName("con el nombre ocupado por una fila deshabilitada la reactiva con los datos nuevos")
        void reactiva_la_fila_deshabilitada_con_los_datos_nuevos() {
            when(repository.findByNameIncludingDisabled(ConsultationTypeMother.NOMBRE))
                    .thenReturn(Optional.of(ConsultationTypeMother.deshabilitada()));
            when(repository.reactivateWithDetails(ConsultationTypeMother.ID,
                    ConsultationTypeMother.NOMBRE, DESCRIPCION_NUEVA)).thenReturn(1);

            ConsultationTypeDto dto = service
                    .execute(ConsultationTypeMother.comandoCrearCon(DESCRIPCION_NUEVA));

            assertThat(dto.id()).isEqualTo(ConsultationTypeMother.ID);
            assertThat(dto.name()).isEqualTo(ConsultationTypeMother.NOMBRE);
            assertThat(dto.description()).isEqualTo(DESCRIPCION_NUEVA);
            assertThat(dto.enabled()).isTrue();
            verify(repository).reactivateWithDetails(ConsultationTypeMother.ID,
                    ConsultationTypeMother.NOMBRE, DESCRIPCION_NUEVA);
        }

        /**
         * La mitad que importa: reactivar es un {@code UPDATE} sobre la fila que ya
         * estaba. Si ademas se llamara a {@code save} habria dos filas compitiendo por
         * el mismo nombre, y la segunda chocaria contra el indice unico.
         */
        @Test
        @DisplayName("al reactivar NO inserta una segunda fila")
        void al_reactivar_no_inserta_una_segunda_fila() {
            when(repository.findByNameIncludingDisabled(ConsultationTypeMother.NOMBRE))
                    .thenReturn(Optional.of(ConsultationTypeMother.deshabilitada()));
            when(repository.reactivateWithDetails(ConsultationTypeMother.ID,
                    ConsultationTypeMother.NOMBRE, DESCRIPCION_NUEVA)).thenReturn(1);

            service.execute(ConsultationTypeMother.comandoCrearCon(DESCRIPCION_NUEVA));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("reactiva la fila por su propio id, no por el del comando")
        void reactiva_la_fila_por_su_propio_id() {
            when(repository.findByNameIncludingDisabled(ConsultationTypeMother.NOMBRE))
                    .thenReturn(Optional.of(ConsultationTypeMother.deshabilitada(77L)));
            when(repository.reactivateWithDetails(77L, ConsultationTypeMother.NOMBRE,
                    DESCRIPCION_NUEVA)).thenReturn(1);

            ConsultationTypeDto dto = service
                    .execute(ConsultationTypeMother.comandoCrearCon(DESCRIPCION_NUEVA));

            assertThat(dto.id()).isEqualTo(77L);
            verify(repository).reactivateWithDetails(77L, ConsultationTypeMother.NOMBRE,
                    DESCRIPCION_NUEVA);
        }

        /**
         * La rama que el issue de {@code laboratorytesttype} destapó. El UPDATE nativo
         * devuelve las filas alcanzadas; si son <b>cero</b>, la fila que leimos en ESTA
         * misma transaccion ha desaparecido o cambiado de dueño entre medias. Devolver
         * el DTO igualmente afirmaria un {@code enabled = true} que no esta en la base
         * —el fallo silencioso que la baja logica hace dificil de ver: el alta responde
         * 201 con un recurso que no existe—.
         *
         * <p>
         * Sale como {@code ObjectOptimisticLockingFailureException} —409
         * {@code CONCURRENT_MODIFICATION}— porque para el front la accion es la misma
         * que ante el candado optimista: recargar y reintentar.
         */
        @Test
        @DisplayName("si el UPDATE no alcanza ninguna fila lanza y no devuelve un dto mentiroso")
        void si_el_update_no_alcanza_ninguna_fila_lanza() {
            when(repository.findByNameIncludingDisabled(ConsultationTypeMother.NOMBRE))
                    .thenReturn(Optional.of(ConsultationTypeMother.deshabilitada()));
            when(repository.reactivateWithDetails(ConsultationTypeMother.ID,
                    ConsultationTypeMother.NOMBRE, DESCRIPCION_NUEVA)).thenReturn(0);
            CreateConsultationTypeCommand command = ConsultationTypeMother
                    .comandoCrearCon(DESCRIPCION_NUEVA);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("ConsultationType");

            // Y no cae al INSERT: reintentar por su cuenta duplicaria la fila.
            verify(repository, never()).save(any());
        }

        /**
         * El orden es la prueba: {@code existing.update(...)} corre ANTES del
         * {@code UPDATE} nativo. Si se invirtieran, un alta incoherente habria
         * resucitado la fila y despues abortado, dejando visible un tipo que nadie
         * pidio revivir y con la descripcion vieja.
         */
        @Test
        @DisplayName("una descripcion invalida aborta la reactivacion antes de tocar la base")
        void una_descripcion_invalida_aborta_antes_de_tocar_la_base() {
            when(repository.findByNameIncludingDisabled(ConsultationTypeMother.NOMBRE))
                    .thenReturn(Optional.of(ConsultationTypeMother.deshabilitada()));
            CreateConsultationTypeCommand command = ConsultationTypeMother.comandoCrearCon("");

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");

            verify(repository, never()).reactivateWithDetails(anyLong(), anyString(), anyString());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una descripcion de mas de 500 caracteres tampoco llega a reactivar")
        void una_descripcion_demasiado_larga_tampoco_reactiva() {
            when(repository.findByNameIncludingDisabled(ConsultationTypeMother.NOMBRE))
                    .thenReturn(Optional.of(ConsultationTypeMother.deshabilitada()));
            CreateConsultationTypeCommand command = ConsultationTypeMother
                    .comandoCrearCon("x".repeat(501));

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("500 chars or less");

            verify(repository, never()).reactivateWithDetails(anyLong(), anyString(), anyString());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un nombre ya usado por un tipo ACTIVO lanza y no guarda")
        void un_nombre_ya_usado_por_un_tipo_activo_lanza_y_no_guarda() {
            when(repository.findByNameIncludingDisabled(ConsultationTypeMother.NOMBRE))
                    .thenReturn(Optional.of(ConsultationTypeMother.consultaGeneral()));
            CreateConsultationTypeCommand command = ConsultationTypeMother.comandoCrear();

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(ConsultationTypeNameAlreadyExistsException.class)
                    .hasMessageContaining(ConsultationTypeMother.NOMBRE)
                    .hasMessageContaining("Ya existe un tipo de consulta activo");

            verify(repository, never()).save(any());
            verify(repository, never()).reactivateWithDetails(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("un nombre invalido lanza y no guarda")
        void nombre_invalido_lanza_y_no_guarda() {
            when(repository.findByNameIncludingDisabled("")).thenReturn(Optional.empty());
            CreateConsultationTypeCommand command = new CreateConsultationTypeCommand("",
                    "Aplicacion de vacunas");

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una descripcion invalida lanza y no guarda")
        void descripcion_invalida_lanza_y_no_guarda() {
            when(repository.findByNameIncludingDisabled("Vacunacion")).thenReturn(Optional.empty());
            CreateConsultationTypeCommand command = new CreateConsultationTypeCommand("Vacunacion",
                    "");

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");

            verify(repository, never()).save(any());
        }
    }
}
