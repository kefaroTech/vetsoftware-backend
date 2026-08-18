package com.vetsoftware.app.problem.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ListProblemsByAnimalQuery")
class ListProblemsByAnimalQueryTest {

    @Test
    @DisplayName("conserva cada campo en su posicion")
    void conserva_cada_campo_en_su_posicion() {
        ListProblemsByAnimalQuery query = new ListProblemsByAnimalQuery(100L, 9L, 1, 20);

        assertThat(query.animalId()).isEqualTo(100L);
        assertThat(query.companyId()).isEqualTo(9L);
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.pageSize()).isEqualTo(20);
    }
}
