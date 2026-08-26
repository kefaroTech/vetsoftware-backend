package com.vetsoftware.app.spatype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.spatype.application.command.CreateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import com.vetsoftware.app.spatype.domain.SpaType;
import com.vetsoftware.app.spatype.domain.SpaTypeNameAlreadyExistsException;
import com.vetsoftware.app.spatype.testsupport.SpaTypeMother;
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
 * Primer test de caso de uso que ha tenido nunca la rodaja {@code spatype}: la
 * cubrian {@code SpaTypeTest}, {@code SpaTypeControllerTest} y
 * {@code SpaTypePersistenceIT}, y en medio —los dos servicios de escritura— no
 * habia nada. Justo donde #559 acaba de meter la guarda de nombre y la rama de
 * reactivacion.
 *
 * <p>
 * {@code spa_types} es catalogo global de plataforma —el caso de uso esta
 * cerrado a {@code hasRole('SYSTEM')} y no hay {@code company_id}—, asi que la
 * guarda de nombre no tiene ambito: el ambito es la tabla entera. Por eso no
 * hay ningun caso de tenancy en esta clase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSpaTypeService — alta de un tipo de spa")
class CreateSpaTypeServiceTest {

    private static final String DESCRIPCION_NUEVA = "Descripcion reescrita en el alta";

    @Mock
    private SpaTypeRepository repository;

    @InjectMocks
    private CreateSpaTypeService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("con el nombre libre crea el tipo con los datos del comando y devuelve su dto")
        void crea_el_tipo_con_los_datos_del_comando() {
            when(repository.findByNameIncludingDisabled("Corte de pelo"))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(SpaTypeMother.banoMedicado());

            SpaTypeDto dto = service
                    .execute(new CreateSpaTypeCommand("Corte de pelo", "Corte y peinado"));

            assertThat(dto.id()).isEqualTo(SpaTypeMother.ID);
            ArgumentCaptor<SpaType> captor = ArgumentCaptor.forClass(SpaType.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Corte de pelo");
            assertThat(captor.getValue().getDescription()).isEqualTo("Corte y peinado");
            assertThat(captor.getValue().getId()).isNull();
            assertThat(captor.getValue().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("con el nombre libre inserta y no toca la rama de reactivacion")
        void con_el_nombre_libre_no_reactiva_nada() {
            when(repository.findByNameIncludingDisabled("Corte de pelo"))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(SpaTypeMother.banoMedicado());

            service.execute(new CreateSpaTypeCommand("Corte de pelo", "Corte y peinado"));

            verify(repository, never()).reactivateWithDetails(anyLong(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Reactivacion de una fila dada de baja")
    class Reactivacion {

        /**
         * El caso que denunciaba #482 para esta tabla exactamente: «dar de baja un tipo
         * de spa quema su nombre para siempre». La baja es logica —la fila desaparece
         * del listado por el {@code @SQLRestriction} pero sigue en la tabla—, asi que
         * el alta con ese nombre chocaba contra algo que el administrador no ve. Ahora
         * resucita la fila y le aplica los datos del comando.
         */
        @Test
        @DisplayName("con el nombre ocupado por una fila deshabilitada la reactiva con los datos nuevos")
        void reactiva_la_fila_deshabilitada_con_los_datos_nuevos() {
            when(repository.findByNameIncludingDisabled(SpaTypeMother.NOMBRE))
                    .thenReturn(Optional.of(SpaTypeMother.deshabilitado()));
            when(repository.reactivateWithDetails(SpaTypeMother.ID, SpaTypeMother.NOMBRE,
                    DESCRIPCION_NUEVA)).thenReturn(1);

            SpaTypeDto dto = service.execute(SpaTypeMother.comandoCrearCon(DESCRIPCION_NUEVA));

            assertThat(dto.id()).isEqualTo(SpaTypeMother.ID);
            assertThat(dto.name()).isEqualTo(SpaTypeMother.NOMBRE);
            assertThat(dto.description()).isEqualTo(DESCRIPCION_NUEVA);
            assertThat(dto.enabled()).isTrue();
            verify(repository).reactivateWithDetails(SpaTypeMother.ID, SpaTypeMother.NOMBRE,
                    DESCRIPCION_NUEVA);
        }

        /**
         * La mitad que importa: reactivar es un {@code UPDATE} sobre la fila que ya
         * estaba. Si ademas se llamara a {@code save} habria dos filas compitiendo por
         * el mismo nombre.
         */
        @Test
        @DisplayName("al reactivar NO inserta una segunda fila")
        void al_reactivar_no_inserta_una_segunda_fila() {
            when(repository.findByNameIncludingDisabled(SpaTypeMother.NOMBRE))
                    .thenReturn(Optional.of(SpaTypeMother.deshabilitado()));
            when(repository.reactivateWithDetails(SpaTypeMother.ID, SpaTypeMother.NOMBRE,
                    DESCRIPCION_NUEVA)).thenReturn(1);

            service.execute(SpaTypeMother.comandoCrearCon(DESCRIPCION_NUEVA));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("reactiva la fila por su propio id, no por el del comando")
        void reactiva_la_fila_por_su_propio_id() {
            when(repository.findByNameIncludingDisabled(SpaTypeMother.NOMBRE))
                    .thenReturn(Optional.of(SpaTypeMother.deshabilitado(77L)));
            when(repository.reactivateWithDetails(77L, SpaTypeMother.NOMBRE, DESCRIPCION_NUEVA))
                    .thenReturn(1);

            SpaTypeDto dto = service.execute(SpaTypeMother.comandoCrearCon(DESCRIPCION_NUEVA));

            assertThat(dto.id()).isEqualTo(77L);
            verify(repository).reactivateWithDetails(77L, SpaTypeMother.NOMBRE, DESCRIPCION_NUEVA);
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
            when(repository.findByNameIncludingDisabled(SpaTypeMother.NOMBRE))
                    .thenReturn(Optional.of(SpaTypeMother.deshabilitado()));
            when(repository.reactivateWithDetails(SpaTypeMother.ID, SpaTypeMother.NOMBRE,
                    DESCRIPCION_NUEVA)).thenReturn(0);
            CreateSpaTypeCommand command = SpaTypeMother.comandoCrearCon(DESCRIPCION_NUEVA);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("SpaType");

            // Y no cae al INSERT: reintentar por su cuenta duplicaria la fila.
            verify(repository, never()).save(any());
        }

        /**
         * El orden es la prueba: {@code existing.update(...)} corre ANTES del
         * {@code UPDATE} nativo. Si se invirtieran, un alta incoherente habria
         * resucitado la fila y despues abortado, dejando visible un tipo que nadie
         * pidio revivir.
         *
         * <p>
         * En {@code SpaType} la descripcion es opcional —a diferencia de
         * {@code ConsultationType}—, asi que lo unico que la invalida es el tope de 500
         * caracteres. Ese es el invariante que se usa aqui para provocar el aborto.
         */
        @Test
        @DisplayName("una descripcion invalida aborta la reactivacion antes de tocar la base")
        void una_descripcion_invalida_aborta_antes_de_tocar_la_base() {
            when(repository.findByNameIncludingDisabled(SpaTypeMother.NOMBRE))
                    .thenReturn(Optional.of(SpaTypeMother.deshabilitado()));
            CreateSpaTypeCommand command = SpaTypeMother.comandoCrearCon("x".repeat(501));

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("500 chars or less");

            verify(repository, never()).reactivateWithDetails(anyLong(), anyString(), anyString());
            verify(repository, never()).save(any());
        }

        /**
         * Contracara del anterior: la descripcion nula SI es valida para el dominio,
         * asi que la reactivacion llega a la base con {@code null}. Quien lo para es la
         * columna {@code NOT NULL} —lo fija {@code SpaTypePersistenceIT}—, no este caso
         * de uso. Se afirma para que la divergencia entre dominio y esquema quede
         * escrita donde se toma la decision.
         */
        @Test
        @DisplayName("una descripcion nula pasa el dominio y llega a la base tal cual")
        void una_descripcion_nula_pasa_el_dominio() {
            when(repository.findByNameIncludingDisabled(SpaTypeMother.NOMBRE))
                    .thenReturn(Optional.of(SpaTypeMother.deshabilitado()));
            when(repository.reactivateWithDetails(SpaTypeMother.ID, SpaTypeMother.NOMBRE, null))
                    .thenReturn(1);

            SpaTypeDto dto = service.execute(SpaTypeMother.comandoCrearCon(null));

            assertThat(dto.description()).isNull();
            verify(repository).reactivateWithDetails(SpaTypeMother.ID, SpaTypeMother.NOMBRE, null);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un nombre ya usado por un tipo ACTIVO lanza y no guarda")
        void un_nombre_ya_usado_por_un_tipo_activo_lanza_y_no_guarda() {
            when(repository.findByNameIncludingDisabled(SpaTypeMother.NOMBRE))
                    .thenReturn(Optional.of(SpaTypeMother.banoMedicado()));
            CreateSpaTypeCommand command = SpaTypeMother.comandoCrear();

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(SpaTypeNameAlreadyExistsException.class)
                    .hasMessageContaining(SpaTypeMother.NOMBRE)
                    .hasMessageContaining("Ya existe un tipo de spa activo");

            verify(repository, never()).save(any());
            verify(repository, never()).reactivateWithDetails(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("un nombre invalido lanza y no guarda")
        void nombre_invalido_lanza_y_no_guarda() {
            when(repository.findByNameIncludingDisabled("")).thenReturn(Optional.empty());
            CreateSpaTypeCommand command = new CreateSpaTypeCommand("", "Corte y peinado");

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un nombre de mas de 100 caracteres lanza y no guarda")
        void nombre_demasiado_largo_lanza_y_no_guarda() {
            String demasiado = "x".repeat(101);
            when(repository.findByNameIncludingDisabled(demasiado)).thenReturn(Optional.empty());
            CreateSpaTypeCommand command = new CreateSpaTypeCommand(demasiado, "Corte y peinado");

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100 chars or less");

            verify(repository, never()).save(any());
        }
    }
}
