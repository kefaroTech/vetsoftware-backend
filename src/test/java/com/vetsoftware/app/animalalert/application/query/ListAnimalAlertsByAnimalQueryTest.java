package com.vetsoftware.app.animalalert.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ListAnimalAlertsByAnimalQuery")
class ListAnimalAlertsByAnimalQueryTest {

    @Test
    @DisplayName("expone el animal y la empresa por los que filtra")
    void expone_el_animal_y_la_empresa() {
        ListAnimalAlertsByAnimalQuery query = new ListAnimalAlertsByAnimalQuery(100L, 9L);

        assertThat(query.animalId()).isEqualTo(100L);
        assertThat(query.companyId()).isEqualTo(9L);
    }
}
