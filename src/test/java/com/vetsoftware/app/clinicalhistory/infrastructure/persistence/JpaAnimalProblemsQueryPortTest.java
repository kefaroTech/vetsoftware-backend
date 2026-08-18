package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.clinicalhistory.application.dto.ReportProblem;
import com.vetsoftware.app.problem.domain.ProblemStatus;
import com.vetsoftware.app.problem.infrastructure.persistence.ProblemJpaEntity;
import com.vetsoftware.app.problem.infrastructure.persistence.ProblemJpaRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalProblemsQueryPort — lista de problemas (POMR) para el PDF")
class JpaAnimalProblemsQueryPortTest {

    private static final Long ANIMAL_ID = 100L;
    private static final Long COMPANY_ID = 9L;
    private static final Pageable PAGINA_200 = Pageable.ofSize(200);

    @Mock
    private ProblemJpaRepository problemJpaRepository;
    @InjectMocks
    private JpaAnimalProblemsQueryPort port;

    /**
     * Construido por completo antes de pasarlo a {@code thenReturn(...)}: ver
     * comentario equivalente en {@code JpaAnimalAlertsQueryPortTest} — anidar este
     * helper dentro de otro {@code when(...).thenReturn(...)} deja a Mockito con
     * una stub a medias.
     */
    private static ProblemJpaEntity problema(ProblemStatus status) {
        ProblemJpaEntity e = mock(ProblemJpaEntity.class);
        when(e.getDescription()).thenReturn("Displasia de cadera");
        when(e.getStatus()).thenReturn(status);
        when(e.getOnsetDate()).thenReturn(LocalDate.of(2025, 1, 1));
        when(e.getResolvedDate()).thenReturn(null);
        when(e.getNotes()).thenReturn("Seguimiento trimestral");
        return e;
    }

    @Nested
    @DisplayName("findByAnimal")
    class FindByAnimal {

        @Test
        @DisplayName("pagina la consulta a 200 filas, acotada a animal y empresa")
        void pagina_la_consulta_a_200_filas() {
            when(problemJpaRepository.findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID,
                    COMPANY_ID, PAGINA_200)).thenReturn(Page.empty());

            port.findByAnimal(ANIMAL_ID, COMPANY_ID);

            verify(problemJpaRepository).findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(
                    ANIMAL_ID, COMPANY_ID, PAGINA_200);
        }

        @Test
        @DisplayName("lista vacía cuando el animal no tiene problemas")
        void lista_vacia_sin_problemas() {
            when(problemJpaRepository.findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID,
                    COMPANY_ID, PAGINA_200)).thenReturn(Page.empty());

            assertThat(port.findByAnimal(ANIMAL_ID, COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("mapea descripción, fechas y notas tal cual")
        void mapea_descripcion_fechas_y_notas() {
            ProblemJpaEntity entidad = problema(ProblemStatus.ACTIVE);
            when(problemJpaRepository.findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID,
                    COMPANY_ID, PAGINA_200)).thenReturn(new PageImpl<>(List.of(entidad)));

            List<ReportProblem> resultado = port.findByAnimal(ANIMAL_ID, COMPANY_ID);

            assertThat(resultado).singleElement().satisfies(p -> {
                assertThat(p.description()).isEqualTo("Displasia de cadera");
                assertThat(p.onsetDate()).isEqualTo(LocalDate.of(2025, 1, 1));
                assertThat(p.resolvedDate()).isNull();
                assertThat(p.notes()).isEqualTo("Seguimiento trimestral");
            });
        }
    }

    @Nested
    @DisplayName("statusLabel y active — recorren las 3 ramas del enum más null")
    class StatusLabel {

        static List<Arguments> estados() {
            return List.of(Arguments.of(ProblemStatus.ACTIVE, "Activo", true),
                    Arguments.of(ProblemStatus.RESOLVED, "Resuelto", false),
                    Arguments.of(ProblemStatus.CHRONIC, "Crónico", true));
        }

        @ParameterizedTest
        @MethodSource("estados")
        @DisplayName("cada estado trae su etiqueta y solo RESOLVED marca active=false")
        void cada_estado_trae_su_etiqueta_y_su_bandera(ProblemStatus status, String etiqueta,
                boolean activoEsperado) {
            ProblemJpaEntity entidad = problema(status);
            when(problemJpaRepository.findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID,
                    COMPANY_ID, PAGINA_200)).thenReturn(new PageImpl<>(List.of(entidad)));

            ReportProblem resultado = port.findByAnimal(ANIMAL_ID, COMPANY_ID).getFirst();

            assertThat(resultado.statusLabel()).isEqualTo(etiqueta);
            assertThat(resultado.active()).isEqualTo(activoEsperado);
        }

        @Test
        @DisplayName("estado nulo produce etiqueta vacía y activo=true")
        void estado_nulo_produce_etiqueta_vacia_y_activo() {
            ProblemJpaEntity entidad = problema(null);
            when(problemJpaRepository.findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID,
                    COMPANY_ID, PAGINA_200)).thenReturn(new PageImpl<>(List.of(entidad)));

            ReportProblem resultado = port.findByAnimal(ANIMAL_ID, COMPANY_ID).getFirst();

            assertThat(resultado.statusLabel()).isEqualTo("");
            assertThat(resultado.active()).isTrue();
        }
    }
}
