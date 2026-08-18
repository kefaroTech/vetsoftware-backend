package com.vetsoftware.app.clinicalhistory.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalHistoryReportModel;
import com.vetsoftware.app.clinicalhistory.application.dto.ReportClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.clinicalhistory.testsupport.ClinicalHistoryMother;
import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
@DisplayName("ClinicalHistoryPdfAdapter — arma el contexto de la plantilla PDF")
class ClinicalHistoryPdfAdapterTest {

    @Mock
    private HtmlPdfRenderer renderer;

    private ClinicalHistoryPdfAdapter adapter;

    @org.junit.jupiter.api.BeforeEach
    void construirAdaptador() {
        adapter = new ClinicalHistoryPdfAdapter(renderer);
    }

    private Map<String, Object> renderizarYCapturarContexto(ClinicalHistoryReportModel model) {
        when(renderer.render(any(), any())).thenReturn(new byte[]{1, 2, 3});

        adapter.render(model);

        ArgumentCaptor<String> nombre = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> ctx = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(renderer).render(nombre.capture(), ctx.capture());
        assertThat(nombre.getValue()).isEqualTo("clinical-history");
        return ctx.getValue();
    }

    @Nested
    @DisplayName("banderas del encabezado")
    class BanderasDelEncabezado {

        @Test
        @DisplayName("con alertas, problemas y eventos, las tres banderas quedan en true")
        void con_contenido_las_banderas_quedan_en_true() {
            ClinicalHistoryReportModel model = ClinicalHistoryMother
                    .reportModel(List.of(ClinicalHistoryMother.reportClinicalEvent(
                            ClinicalEventType.CONSULTATION, LocalDate.of(2026, 1, 15))));

            Map<String, Object> ctx = renderizarYCapturarContexto(model);

            assertThat(ctx.get("hasAlerts")).isEqualTo(true);
            assertThat(ctx.get("hasProblems")).isEqualTo(true);
            assertThat(ctx.get("hasEvents")).isEqualTo(true);
            assertThat(ctx.get("eventCount")).isEqualTo(1);
        }

        @Test
        @DisplayName("sin alertas, problemas ni eventos, las tres banderas quedan en false")
        void sin_contenido_las_banderas_quedan_en_false() {
            ClinicalHistoryReportModel vacio = new ClinicalHistoryReportModel(
                    ClinicalHistoryMother.animalReportInfo(), LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 8, 1), List.of(), List.of(), List.of(), List.of(),
                    java.time.LocalDateTime.of(2026, 8, 12, 10, 0));

            Map<String, Object> ctx = renderizarYCapturarContexto(vacio);

            assertThat(ctx.get("hasAlerts")).isEqualTo(false);
            assertThat(ctx.get("hasProblems")).isEqualTo(false);
            assertThat(ctx.get("hasEvents")).isEqualTo(false);
            assertThat(ctx.get("eventCount")).isEqualTo(0);
            assertThat((List<?>) ctx.get("typeCounts")).isEmpty();
            assertThat((List<?>) ctx.get("monthGroups")).isEmpty();
        }

        @Test
        @DisplayName("el resultado de render() es el byte[] que devuelve el renderer")
        void el_resultado_es_el_del_renderer() {
            byte[] esperado = {9, 9, 9};
            when(renderer.render(any(), any())).thenReturn(esperado);

            byte[] resultado = adapter.render(ClinicalHistoryMother.reportModel(List.of()));

            assertThat(resultado).isEqualTo(esperado);
        }
    }

    @Nested
    @DisplayName("typeCounts — conteo por tipo en el orden del enum")
    class TypeCounts {

        @Test
        @DisplayName("solo trae los tipos presentes, en el orden del enum, no el de inserción")
        void trae_solo_los_tipos_presentes_en_orden_de_enum() {
            List<ReportClinicalEvent> eventos = List.of(
                    ClinicalHistoryMother.reportClinicalEvent(ClinicalEventType.SPA,
                            LocalDate.of(2026, 1, 10)),
                    ClinicalHistoryMother.reportClinicalEvent(ClinicalEventType.CONSULTATION,
                            LocalDate.of(2026, 1, 11)),
                    ClinicalHistoryMother.reportClinicalEvent(ClinicalEventType.CONSULTATION,
                            LocalDate.of(2026, 1, 12)));

            Map<String, Object> ctx = renderizarYCapturarContexto(
                    ClinicalHistoryMother.reportModel(eventos));

            List<ClinicalHistoryPdfAdapter.TypeCount> counts = (List<ClinicalHistoryPdfAdapter.TypeCount>) ctx
                    .get("typeCounts");
            // CONSULTATION va antes que SPA en el enum, aunque el primer evento visto sea
            // SPA.
            assertThat(counts).extracting(ClinicalHistoryPdfAdapter.TypeCount::type)
                    .containsExactly("CONSULTATION", "SPA");
            assertThat(counts.get(0).count()).isEqualTo(2L);
            assertThat(counts.get(0).label()).isEqualTo("Consulta");
            assertThat(counts.get(1).count()).isEqualTo(1L);
            assertThat(counts.get(1).label()).isEqualTo("Estética");
        }

        @ParameterizedTest
        @EnumSource(ClinicalEventType.class)
        @DisplayName("los 9 tipos tienen una etiqueta en español no vacía")
        void los_9_tipos_tienen_etiqueta(ClinicalEventType tipo) {
            Map<String, Object> ctx = renderizarYCapturarContexto(
                    ClinicalHistoryMother.reportModel(List.of(ClinicalHistoryMother
                            .reportClinicalEvent(tipo, LocalDate.of(2026, 1, 1)))));

            List<ClinicalHistoryPdfAdapter.TypeCount> counts = (List<ClinicalHistoryPdfAdapter.TypeCount>) ctx
                    .get("typeCounts");
            assertThat(counts).singleElement().satisfies(c -> {
                assertThat(c.type()).isEqualTo(tipo.name());
                assertThat(c.label()).isNotBlank();
            });

            Map<String, String> typeLabels = (Map<String, String>) ctx.get("typeLabels");
            assertThat(typeLabels).containsKey(tipo.name());
            assertThat(typeLabels.get(tipo.name())).isNotBlank();
        }
    }

    @Nested
    @DisplayName("typeLabels — las 9 claves del enum")
    class TypeLabels {

        @Test
        @DisplayName("el mapa trae las 9 claves aunque no haya eventos de todos los tipos")
        void trae_las_9_claves_aunque_no_haya_eventos_de_todos() {
            Map<String, Object> ctx = renderizarYCapturarContexto(ClinicalHistoryMother
                    .reportModel(List.of(ClinicalHistoryMother.reportClinicalEvent(
                            ClinicalEventType.CONSULTATION, LocalDate.of(2026, 1, 1)))));

            Map<String, String> typeLabels = (Map<String, String>) ctx.get("typeLabels");
            assertThat(typeLabels).hasSize(9);
        }
    }

    @Nested
    @DisplayName("monthGroups — agrupado por mes con label en español")
    class MonthGroups {

        @Test
        @DisplayName("agrupa por año-mes, con label en español, incluido un cambio de año")
        void agrupa_por_anio_mes_con_label_en_espanol() {
            List<ReportClinicalEvent> eventos = List.of(
                    ClinicalHistoryMother.reportClinicalEvent(ClinicalEventType.CONSULTATION,
                            LocalDate.of(2025, 12, 20)),
                    ClinicalHistoryMother.reportClinicalEvent(ClinicalEventType.CONSULTATION,
                            LocalDate.of(2026, 1, 5)),
                    ClinicalHistoryMother.reportClinicalEvent(ClinicalEventType.CONSULTATION,
                            LocalDate.of(2026, 1, 20)));

            Map<String, Object> ctx = renderizarYCapturarContexto(
                    ClinicalHistoryMother.reportModel(eventos));

            List<ClinicalHistoryMonthGroup> grupos = (List<ClinicalHistoryMonthGroup>) ctx
                    .get("monthGroups");
            assertThat(grupos).extracting(ClinicalHistoryMonthGroup::label)
                    .containsExactlyInAnyOrder("Diciembre 2025", "Enero 2026");
            assertThat(grupos).filteredOn(g -> g.key().equals("2026-01")).singleElement()
                    .extracting(g -> g.events().size()).isEqualTo(2);
        }
    }
}
