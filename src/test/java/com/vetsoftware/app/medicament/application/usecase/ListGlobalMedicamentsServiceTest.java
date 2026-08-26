package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.testsupport.MedicamentMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El catalogo GLOBAL activo, paginado.
 *
 * <p>
 * Que use {@code findAllGlobal} y no {@code findAll} —que devuelve ademas los
 * privados de cada empresa— no se afirma con un {@code verify} de consulta: lo
 * garantiza el stub exacto mas STRICT_STUBS. Si el servicio llamara al otro
 * finder, este stub quedaria sin usar y Mockito rompe el test por
 * {@code UnnecessaryStubbing}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListGlobalMedicamentsService")
class ListGlobalMedicamentsServiceTest {

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private ListGlobalMedicamentsService service;

    @Test
    @DisplayName("mapea la pagina de globales a DTOs conservando los metadatos de paginacion")
    void mapea_la_pagina_de_globales_conservando_metadatos() {
        Medicament global = MedicamentMother.activoGeneral();
        when(repository.findAllGlobal(null, 2, 5))
                .thenReturn(PageResult.of(List.of(global), 2, 5, 47L));

        PageResult<MedicamentDto> pagina = service.listAll(null, 2, 5);

        assertThat(pagina.content()).extracting(MedicamentDto::name).containsExactly("Amoxicilina");
        assertThat(pagina.content()).allSatisfy(dto -> {
            assertThat(dto.general()).isTrue();
            assertThat(dto.company()).isNull();
        });
        // Los totales son los de la consulta, no los del contenido de la pagina:
        // recalcularlos sobre lo ya paginado es como se acaba reportando «5 de 5».
        assertThat(pagina.page()).isEqualTo(2);
        assertThat(pagina.pageSize()).isEqualTo(5);
        assertThat(pagina.totalElements()).isEqualTo(47L);
        assertThat(pagina.totalPages()).isEqualTo(10);
    }

    @Test
    @DisplayName("un catalogo vacio devuelve una pagina vacia, no null")
    void catalogo_vacio_devuelve_pagina_vacia() {
        when(repository.findAllGlobal(null, 0, 20)).thenReturn(PageResult.empty(0, 20));

        PageResult<MedicamentDto> pagina = service.listAll(null, 0, 20);

        assertThat(pagina.content()).isEmpty();
        assertThat(pagina.totalElements()).isZero();
    }

    /**
     * El caso de uso no interpreta el termino: ni lo recorta, ni lo normaliza, ni
     * decide si esta vacio. Eso es del adaptador, que es quien conoce la collation
     * con la que la base compara — normalizar aqui es como se consigue que buscar y
     * chocar dejen de responder al mismo criterio. El stub exacto mas STRICT_STUBS
     * es la asercion.
     */
    @Test
    @DisplayName("el termino de busqueda viaja al puerto tal cual, sin normalizar")
    void el_termino_viaja_al_puerto_tal_cual() {
        when(repository.findAllGlobal("  clavul  ", 0, 20))
                .thenReturn(PageResult.of(List.of(MedicamentMother.activoGeneral()), 0, 20, 1L));

        PageResult<MedicamentDto> pagina = service.listAll("  clavul  ", 0, 20);

        assertThat(pagina.content()).extracting(MedicamentDto::name).containsExactly("Amoxicilina");
    }

    /**
     * Una busqueda sin resultados es una pagina vacia con el total a cero, no una
     * excepcion ni un 404: el front dibuja el estado vacio del listado, no un
     * error.
     */
    @Test
    @DisplayName("un termino sin resultados devuelve pagina vacia, no un error")
    void termino_sin_resultados_devuelve_pagina_vacia() {
        when(repository.findAllGlobal("no-existe", 0, 20)).thenReturn(PageResult.empty(0, 20));

        PageResult<MedicamentDto> pagina = service.listAll("no-existe", 0, 20);

        assertThat(pagina.content()).isEmpty();
        assertThat(pagina.totalElements()).isZero();
    }
}
