package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import com.vetsoftware.app.animalalert.infrastructure.persistence.AnimalAlertJpaEntity;
import com.vetsoftware.app.animalalert.infrastructure.persistence.AnimalAlertJpaRepository;
import com.vetsoftware.app.clinicalhistory.application.dto.ReportAlert;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalAlertsQueryPort — alertas del animal para el banner del PDF")
class JpaAnimalAlertsQueryPortTest {

    private static final Long ANIMAL_ID = 100L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private AnimalAlertJpaRepository animalAlertJpaRepository;
    @InjectMocks
    private JpaAnimalAlertsQueryPort port;

    /**
     * Construye el mock por completo ANTES de que el llamador lo pase a
     * {@code when(...).thenReturn(...)}: encadenar la construcción dentro del
     * propio {@code thenReturn(...)} deja a Mockito con una stub a medias (el
     * {@code when()} externo aún sin cerrar) mientras este helper abre los suyos, y
     * revienta con {@code UnfinishedStubbingException}.
     */
    private static AnimalAlertJpaEntity alerta(AlertType tipo, AlertSeverity severidad) {
        AnimalAlertJpaEntity e = mock(AnimalAlertJpaEntity.class);
        when(e.getType()).thenReturn(tipo);
        when(e.getDescription()).thenReturn("Reacciona mal a la penicilina");
        when(e.getSeverity()).thenReturn(severidad);
        return e;
    }

    @Nested
    @DisplayName("findByAnimal")
    class FindByAnimal {

        @Test
        @DisplayName("consulta ordenado por fecha descendente, acotado a animal y empresa")
        void consulta_acotado_a_animal_y_empresa() {
            when(animalAlertJpaRepository
                    .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(List.of());

            port.findByAnimal(ANIMAL_ID, COMPANY_ID);

            verify(animalAlertJpaRepository)
                    .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("lista vacía cuando el animal no tiene alertas")
        void lista_vacia_sin_alertas() {
            when(animalAlertJpaRepository
                    .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(List.of());

            assertThat(port.findByAnimal(ANIMAL_ID, COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("mapea descripción y severidad tal cual, y traduce el tipo")
        void mapea_descripcion_y_traduce_el_tipo() {
            AnimalAlertJpaEntity entidad = alerta(AlertType.ALLERGY, AlertSeverity.HIGH);
            when(animalAlertJpaRepository
                    .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(List.of(entidad));

            List<ReportAlert> resultado = port.findByAnimal(ANIMAL_ID, COMPANY_ID);

            assertThat(resultado).singleElement().satisfies(a -> {
                assertThat(a.typeLabel()).isEqualTo("Alergia");
                assertThat(a.description()).isEqualTo("Reacciona mal a la penicilina");
                assertThat(a.severityLabel()).isEqualTo("Alta");
            });
        }
    }

    @Nested
    @DisplayName("typeLabel — recorre las 5 ramas del enum")
    class TypeLabel {

        static List<Arguments> tipos() {
            return List.of(Arguments.of(AlertType.ALLERGY, "Alergia"),
                    Arguments.of(AlertType.DRUG_REACTION, "Reacción a fármaco"),
                    Arguments.of(AlertType.CHRONIC_CONDITION, "Condición crónica"),
                    Arguments.of(AlertType.BEHAVIOR, "Comportamiento"),
                    Arguments.of(AlertType.OTHER, "Otra"));
        }

        @ParameterizedTest
        @MethodSource("tipos")
        @DisplayName("cada tipo de alerta trae su etiqueta en español")
        void cada_tipo_trae_su_etiqueta(AlertType tipo, String etiquetaEsperada) {
            AnimalAlertJpaEntity entidad = alerta(tipo, AlertSeverity.LOW);
            when(animalAlertJpaRepository
                    .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(List.of(entidad));

            assertThat(port.findByAnimal(ANIMAL_ID, COMPANY_ID)).singleElement()
                    .extracting(ReportAlert::typeLabel).isEqualTo(etiquetaEsperada);
        }

        @Test
        @DisplayName("tipo nulo produce etiqueta vacía, no null")
        void tipo_nulo_produce_etiqueta_vacia() {
            AnimalAlertJpaEntity entidad = alerta(null, AlertSeverity.LOW);
            when(animalAlertJpaRepository
                    .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(List.of(entidad));

            assertThat(port.findByAnimal(ANIMAL_ID, COMPANY_ID)).singleElement()
                    .extracting(ReportAlert::typeLabel).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("severityLabel — recorre las 3 ramas del enum más null")
    class SeverityLabel {

        static List<Arguments> severidades() {
            return List.of(Arguments.of(AlertSeverity.LOW, "Baja"),
                    Arguments.of(AlertSeverity.MEDIUM, "Media"),
                    Arguments.of(AlertSeverity.HIGH, "Alta"));
        }

        @ParameterizedTest
        @MethodSource("severidades")
        @DisplayName("cada severidad trae su etiqueta en español")
        void cada_severidad_trae_su_etiqueta(AlertSeverity severidad, String etiquetaEsperada) {
            AnimalAlertJpaEntity entidad = alerta(AlertType.OTHER, severidad);
            when(animalAlertJpaRepository
                    .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(List.of(entidad));

            assertThat(port.findByAnimal(ANIMAL_ID, COMPANY_ID)).singleElement()
                    .extracting(ReportAlert::severityLabel).isEqualTo(etiquetaEsperada);
        }

        @Test
        @DisplayName("severidad nula queda null, a diferencia del tipo")
        void severidad_nula_queda_null() {
            AnimalAlertJpaEntity entidad = alerta(AlertType.OTHER, null);
            when(animalAlertJpaRepository
                    .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(List.of(entidad));

            assertThat(port.findByAnimal(ANIMAL_ID, COMPANY_ID)).singleElement()
                    .extracting(ReportAlert::severityLabel).isNull();
        }
    }
}
