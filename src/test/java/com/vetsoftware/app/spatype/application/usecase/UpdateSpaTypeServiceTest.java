package com.vetsoftware.app.spatype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.spatype.application.command.UpdateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import com.vetsoftware.app.spatype.domain.SpaType;
import com.vetsoftware.app.spatype.domain.SpaTypeNameAlreadyExistsException;
import com.vetsoftware.app.spatype.domain.SpaTypeNotFoundException;
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

/**
 * La edicion gano una guarda de nombre en #559. Lo delicado de esa guarda no es
 * que detecte el choque, es que <b>excluya la propia fila</b>: sin el
 * {@code ExcludingId} nadie podria guardar una ficha sin cambiarle el nombre
 * —corregir solo la descripcion daria 409 contra si misma—, que es la regresion
 * mas facil de introducir aqui y la que fija
 * {@link Actualizacion#conservar_el_mismo_nombre_no_es_conflicto()}.
 *
 * <p>
 * La guarda cuenta solo filas ACTIVAS, igual que el indice unico de la base:
 * una fila dada de baja con ese nombre no estorba.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateSpaTypeService — edicion de un tipo de spa")
class UpdateSpaTypeServiceTest {

    @Mock
    private SpaTypeRepository repository;

    @InjectMocks
    private UpdateSpaTypeService service;

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza nombre y descripcion sobre el tipo existente y lo persiste")
        void actualiza_nombre_y_descripcion_y_persiste() {
            when(repository.findById(SpaTypeMother.ID))
                    .thenReturn(Optional.of(SpaTypeMother.banoMedicado()));
            when(repository.existsActiveByNameExcludingId("Corte de pelo", SpaTypeMother.ID))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            SpaTypeDto dto = service.execute(SpaTypeMother.comandoActualizar());

            assertThat(dto.name()).isEqualTo("Corte de pelo");
            assertThat(dto.description()).isEqualTo("Corte y peinado");
            ArgumentCaptor<SpaType> captor = ArgumentCaptor.forClass(SpaType.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(SpaTypeMother.ID);
        }

        /**
         * La guarda excluye la propia fila por id. Si no lo hiciera, guardar la ficha
         * sin tocar el nombre —el caso mas comun de todos: corregir una descripcion—
         * responderia 409 contra si misma y el tipo quedaria inmutable para siempre.
         */
        @Test
        @DisplayName("conservar el mismo nombre que ya tenia la fila no es conflicto")
        void conservar_el_mismo_nombre_no_es_conflicto() {
            when(repository.findById(SpaTypeMother.ID))
                    .thenReturn(Optional.of(SpaTypeMother.banoMedicado()));
            when(repository.existsActiveByNameExcludingId(SpaTypeMother.NOMBRE, SpaTypeMother.ID))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            SpaTypeDto dto = service.execute(new UpdateSpaTypeCommand(SpaTypeMother.ID,
                    SpaTypeMother.NOMBRE, "Solo cambia la descripcion"));

            assertThat(dto.name()).isEqualTo(SpaTypeMother.NOMBRE);
            assertThat(dto.description()).isEqualTo("Solo cambia la descripcion");
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("un nombre ya usado por OTRO tipo activo lanza y no guarda")
        void un_nombre_ya_usado_por_otro_tipo_activo_lanza_y_no_guarda() {
            when(repository.findById(SpaTypeMother.ID))
                    .thenReturn(Optional.of(SpaTypeMother.banoMedicado()));
            when(repository.existsActiveByNameExcludingId("Bano ocupado", SpaTypeMother.ID))
                    .thenReturn(true);
            UpdateSpaTypeCommand command = new UpdateSpaTypeCommand(SpaTypeMother.ID,
                    "Bano ocupado", "Descripcion valida");

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(SpaTypeNameAlreadyExistsException.class)
                    .hasMessageContaining("Bano ocupado")
                    .hasMessageContaining("Ya existe un tipo de spa activo");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un id inexistente lanza y no guarda ni consulta el nombre")
        void un_id_inexistente_lanza_y_no_guarda() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new UpdateSpaTypeCommand(99L, "Nombre", "Descripcion valida")))
                    .isInstanceOf(SpaTypeNotFoundException.class)
                    .hasMessageContaining("SpaType not found: 99");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un nombre invalido lanza y no guarda")
        void un_nombre_invalido_lanza_y_no_guarda() {
            when(repository.findById(SpaTypeMother.ID))
                    .thenReturn(Optional.of(SpaTypeMother.banoMedicado()));
            when(repository.existsActiveByNameExcludingId("", SpaTypeMother.ID)).thenReturn(false);

            assertThatThrownBy(() -> service
                    .execute(new UpdateSpaTypeCommand(SpaTypeMother.ID, "", "Descripcion valida")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
        }
    }
}
