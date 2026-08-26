package com.vetsoftware.app.consultationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultationtype.application.command.UpdateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNameAlreadyExistsException;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
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
@DisplayName("UpdateConsultationTypeService — edicion de un tipo de consulta")
class UpdateConsultationTypeServiceTest {

    @Mock
    private ConsultationTypeRepository repository;

    @InjectMocks
    private UpdateConsultationTypeService service;

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza nombre y descripcion sobre el tipo existente y lo persiste")
        void actualiza_nombre_y_descripcion_y_persiste() {
            when(repository.findById(ConsultationTypeMother.ID))
                    .thenReturn(Optional.of(ConsultationTypeMother.consultaGeneral()));
            when(repository.existsActiveByNameExcludingId("Nuevo nombre",
                    ConsultationTypeMother.ID)).thenReturn(false);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ConsultationTypeDto dto = service.execute(new UpdateConsultationTypeCommand(
                    ConsultationTypeMother.ID, "Nuevo nombre", "Nueva descripcion de la consulta"));

            assertThat(dto.name()).isEqualTo("Nuevo nombre");
            assertThat(dto.description()).isEqualTo("Nueva descripcion de la consulta");
            ArgumentCaptor<ConsultationType> captor = ArgumentCaptor
                    .forClass(ConsultationType.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(ConsultationTypeMother.ID);
        }

        /**
         * La guarda excluye la propia fila por id. Si no lo hiciera, guardar la ficha
         * sin tocar el nombre —el caso mas comun de todos: corregir una descripcion—
         * respondería 409 contra si misma y el tipo quedaria inmutable para siempre.
         */
        @Test
        @DisplayName("conservar el mismo nombre que ya tenia la fila no es conflicto")
        void conservar_el_mismo_nombre_no_es_conflicto() {
            when(repository.findById(ConsultationTypeMother.ID))
                    .thenReturn(Optional.of(ConsultationTypeMother.consultaGeneral()));
            when(repository.existsActiveByNameExcludingId(ConsultationTypeMother.NOMBRE,
                    ConsultationTypeMother.ID)).thenReturn(false);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ConsultationTypeDto dto = service
                    .execute(new UpdateConsultationTypeCommand(ConsultationTypeMother.ID,
                            ConsultationTypeMother.NOMBRE, "Solo cambia la descripcion"));

            assertThat(dto.name()).isEqualTo(ConsultationTypeMother.NOMBRE);
            assertThat(dto.description()).isEqualTo("Solo cambia la descripcion");
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("un nombre ya usado por OTRO tipo activo lanza y no guarda")
        void un_nombre_ya_usado_por_otro_tipo_activo_lanza_y_no_guarda() {
            when(repository.findById(ConsultationTypeMother.ID))
                    .thenReturn(Optional.of(ConsultationTypeMother.consultaGeneral()));
            when(repository.existsActiveByNameExcludingId("Consulta ocupada",
                    ConsultationTypeMother.ID)).thenReturn(true);
            UpdateConsultationTypeCommand command = new UpdateConsultationTypeCommand(
                    ConsultationTypeMother.ID, "Consulta ocupada", "Descripcion valida");

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(ConsultationTypeNameAlreadyExistsException.class)
                    .hasMessageContaining("Consulta ocupada")
                    .hasMessageContaining("Ya existe un tipo de consulta activo");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un id inexistente lanza y no guarda")
        void un_id_inexistente_lanza_y_no_guarda() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new UpdateConsultationTypeCommand(99L,
                    "Nombre", "Descripcion valida de la consulta")))
                    .isInstanceOf(ConsultationTypeNotFoundException.class)
                    .hasMessageContaining("ConsultationType not found: 99");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un nombre invalido lanza y no guarda")
        void un_nombre_invalido_lanza_y_no_guarda() {
            when(repository.findById(ConsultationTypeMother.ID))
                    .thenReturn(Optional.of(ConsultationTypeMother.consultaGeneral()));
            when(repository.existsActiveByNameExcludingId("", ConsultationTypeMother.ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(new UpdateConsultationTypeCommand(
                    ConsultationTypeMother.ID, "", "Descripcion valida de la consulta")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
        }
    }
}
