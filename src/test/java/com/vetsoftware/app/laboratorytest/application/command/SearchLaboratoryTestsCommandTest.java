package com.vetsoftware.app.laboratorytest.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Record de entrada del filtro de busqueda: sin invariantes propias, pero cada
 * campo tiene que llegar al mismo slot con el que se construyo — es lo que
 * {@link com.vetsoftware.app.laboratorytest.infrastructure.persistence.JpaLaboratoryTestRepository#buildSpec}
 * lee para armar el {@code Specification}.
 */
@DisplayName("SearchLaboratoryTestsCommand")
class SearchLaboratoryTestsCommandTest {

    @Test
    @DisplayName("expone cada campo en la misma posicion con la que se construyo")
    void expone_cada_campo_en_su_posicion() {
        List<LaboratoryTestStatus> estados = List.of(LaboratoryTestStatus.PENDING_COLLECTION,
                LaboratoryTestStatus.IN_PROGRESS);
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);

        SearchLaboratoryTestsCommand command = new SearchLaboratoryTestsCommand(9L, 5L, estados, 7L,
                4L, LaboratoryTestPriority.URGENTE, desde, hasta, 2, 50);

        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(command.branchId()).isEqualTo(5L);
        assertThat(command.statuses()).containsExactly(LaboratoryTestStatus.PENDING_COLLECTION,
                LaboratoryTestStatus.IN_PROGRESS);
        assertThat(command.animalId()).isEqualTo(7L);
        assertThat(command.testTypeId()).isEqualTo(4L);
        assertThat(command.prioridad()).isEqualTo(LaboratoryTestPriority.URGENTE);
        assertThat(command.dateFrom()).isEqualTo(desde);
        assertThat(command.dateTo()).isEqualTo(hasta);
        assertThat(command.page()).isEqualTo(2);
        assertThat(command.pageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("todos los filtros opcionales admiten null: es una busqueda sin criterio")
    void todos_los_filtros_opcionales_admiten_null() {
        SearchLaboratoryTestsCommand command = new SearchLaboratoryTestsCommand(9L, null, null,
                null, null, null, null, null, 0, 20);

        assertThat(command.branchId()).isNull();
        assertThat(command.statuses()).isNull();
        assertThat(command.animalId()).isNull();
        assertThat(command.testTypeId()).isNull();
        assertThat(command.prioridad()).isNull();
        assertThat(command.dateFrom()).isNull();
        assertThat(command.dateTo()).isNull();
    }
}
