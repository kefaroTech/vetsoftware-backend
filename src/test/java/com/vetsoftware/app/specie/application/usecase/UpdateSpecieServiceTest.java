package com.vetsoftware.app.specie.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.specie.application.command.UpdateSpecieCommand;
import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.Specie;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import com.vetsoftware.app.specie.testsupport.SpecieMother;
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
 * {@code Specie} es un catalogo maestro global: ni el dominio, ni
 * {@link SpecieRepository}, ni este service reciben o filtran por
 * {@code companyId} — lo confirma el {@code @NoAuthorizationRequired} de
 * {@code ListSpeciesUseCase} ("no contiene datos de ninguna empresa"). No hay
 * por tanto un escenario de "entidad de otra empresa" que construir aqui: no
 * existe el campo contra el que aislar. La unica frontera de esta feature es el
 * rol SYSTEM en el {@code @PreAuthorize} del puerto de entrada, que no lo
 * ejercita esta capa (ver `SpecieControllerTest` y el informe final).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateSpecieService")
class UpdateSpecieServiceTest {

    @Mock
    private SpecieRepository repository;
    @InjectMocks
    private UpdateSpecieService service;

    @Nested
    @DisplayName("actualizacion permitida")
    class ActualizacionPermitida {

        @Test
        @DisplayName("actualiza el nombre de la especie encontrada y la persiste")
        void actualiza_el_nombre_y_persiste() {
            when(repository.findById(SpecieMother.SPECIE_ID))
                    .thenReturn(Optional.of(SpecieMother.perro()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SpecieDto dto = service.execute(SpecieMother.comandoActualizar());

            ArgumentCaptor<Specie> guardada = ArgumentCaptor.forClass(Specie.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getName()).isEqualTo("Gato");
            assertThat(guardada.getValue().getId()).isEqualTo(SpecieMother.SPECIE_ID);
            assertThat(dto.name()).isEqualTo("Gato");
        }
    }

    @Nested
    @DisplayName("especie inexistente")
    class EspecieInexistente {

        @Test
        @DisplayName("no toca el repositorio de escritura si la especie no existe")
        void no_escribe_si_la_especie_no_existe() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new UpdateSpecieCommand(999L, "Gato")))
                    .isInstanceOf(SpecieNotFoundException.class)
                    .hasMessageContaining("Specie not found: 999");

            verify(repository, never()).save(any());
        }
    }
}
