package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListMedicamentsService")
class ListMedicamentsServiceTest {

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private ListMedicamentsService service;

    @Test
    @DisplayName("mapea la pagina de dominio a una pagina de DTOs conservando los metadatos")
    void mapea_la_pagina_conservando_metadatos() {
        Medicament medicamento = Medicament.create("Amoxicilina", null, null, true);
        when(repository.findAll(null, 0, 20))
                .thenReturn(PageResult.of(List.of(medicamento), 0, 20, 1L));

        PageResult<MedicamentDto> pagina = service.listAll(null, 0, 20);

        assertThat(pagina.content()).extracting(MedicamentDto::name).containsExactly("Amoxicilina");
        assertThat(pagina.totalElements()).isEqualTo(1L);
    }

    /**
     * El caso de uso no interpreta el termino: no lo recorta, no lo normaliza y no
     * decide si esta vacio. Eso vive en el adaptador, que es quien conoce la
     * collation con la que la base compara. Aqui solo se fija que llega intacto —el
     * stub exacto mas STRICT_STUBS es la asercion: con cualquier otro valor el stub
     * quedaria sin usar y Mockito rompe el test.
     */
    @Test
    @DisplayName("el termino de busqueda viaja al puerto tal cual, sin normalizar")
    void el_termino_viaja_al_puerto_tal_cual() {
        Medicament medicamento = Medicament.create("Amoxicilina", null, null, true);
        when(repository.findAll("  Amoxi  ", 0, 20))
                .thenReturn(PageResult.of(List.of(medicamento), 0, 20, 1L));

        PageResult<MedicamentDto> pagina = service.listAll("  Amoxi  ", 0, 20);

        assertThat(pagina.content()).extracting(MedicamentDto::name).containsExactly("Amoxicilina");
    }
}
