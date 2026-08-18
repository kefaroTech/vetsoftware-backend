package com.vetsoftware.app.clinicalhistory.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cubre SOLO las invariantes propias del record. La normalización de ventana
 * (rango por defecto, recorte a 366 días) vive en
 * {@code ListCompanyClinicalEventsService.bound(...)} y ya tiene su test en
 * {@code ListCompanyClinicalEventsServiceTest}.
 */
@DisplayName("ListCompanyClinicalEventsQuery — invariantes propias del record")
class ListCompanyClinicalEventsQueryTest {

    private static final Long COMPANY_ID = 9L;

    @Test
    @DisplayName("companyId nulo se rechaza")
    void company_id_nulo_se_rechaza() {
        assertThatThrownBy(() -> new ListCompanyClinicalEventsQuery(null, List.of(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("companyId is required");
    }

    @Test
    @DisplayName("types nulo se normaliza a lista vacía")
    void types_nulo_se_normaliza_a_lista_vacia() {
        ListCompanyClinicalEventsQuery query = new ListCompanyClinicalEventsQuery(COMPANY_ID, null,
                null, null);

        assertThat(query.types()).isEmpty();
    }

    @Test
    @DisplayName("'to' anterior a 'from' se rechaza")
    void to_anterior_a_from_se_rechaza() {
        LocalDate from = LocalDate.of(2026, 8, 10);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(
                () -> new ListCompanyClinicalEventsQuery(COMPANY_ID, List.of(), from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'to' cannot be before 'from'");
    }

    @Test
    @DisplayName("un rango válido se conserva tal cual, sin recortar")
    void rango_valido_se_conserva_tal_cual() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 15);

        ListCompanyClinicalEventsQuery query = new ListCompanyClinicalEventsQuery(COMPANY_ID,
                List.of(), from, to);

        assertThat(query.from()).isEqualTo(from);
        assertThat(query.to()).isEqualTo(to);
    }
}
