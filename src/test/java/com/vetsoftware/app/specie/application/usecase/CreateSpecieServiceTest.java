package com.vetsoftware.app.specie.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.Specie;
import com.vetsoftware.app.specie.testsupport.SpecieMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSpecieService")
class CreateSpecieServiceTest {

    @Mock
    private SpecieRepository repository;
    @InjectMocks
    private CreateSpecieService service;

    @Test
    @DisplayName("crea la especie a partir del nombre del comando y la persiste habilitada")
    void crea_la_especie_a_partir_del_nombre_del_comando() {
        when(repository.save(any())).thenAnswer(inv -> {
            Specie specie = inv.getArgument(0);
            return new Specie(SpecieMother.SPECIE_ID, specie.getName(), specie.getCreatedDate(),
                    specie.getVersion(), specie.isEnabled());
        });

        SpecieDto dto = service.execute(SpecieMother.comandoCrear());

        ArgumentCaptor<Specie> guardada = ArgumentCaptor.forClass(Specie.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getName()).isEqualTo("Perro");
        assertThat(guardada.getValue().isEnabled()).isTrue();
        assertThat(guardada.getValue().getId()).isNull();
        assertThat(dto.id()).isEqualTo(SpecieMother.SPECIE_ID);
        assertThat(dto.name()).isEqualTo("Perro");
    }
}
