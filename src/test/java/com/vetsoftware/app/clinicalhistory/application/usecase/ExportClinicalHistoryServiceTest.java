package com.vetsoftware.app.clinicalhistory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.clinicalhistory.application.dto.AnimalReportInfo;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDetail;
import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalHistoryReportModel;
import com.vetsoftware.app.clinicalhistory.application.dto.DetailField;
import com.vetsoftware.app.clinicalhistory.application.dto.ReportAlert;
import com.vetsoftware.app.clinicalhistory.application.dto.ReportProblem;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalAlertsQueryPort;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalProblemsQueryPort;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalReportQueryPort;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventDetailQueryPort;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalHistoryPdfPort;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.clinicalhistory.testsupport.ClinicalHistoryMother;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El aislamiento multi-tenant de esta exportación depende ÍNTEGRAMENTE de
 * {@code animalQueryPort.findByIdAndCompanyId}: si el animal no existe para la
 * empresa del caller (sea porque no existe o porque es de otra empresa), el
 * servicio no debe consultar ningún otro puerto — nada de historia, nada de
 * alertas, nada de PDF renderizado con datos ajenos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExportClinicalHistoryService — arma el reporte PDF respetando el tenant")
class ExportClinicalHistoryServiceTest {

    private static final Long ANIMAL_ID = ClinicalHistoryMother.ANIMAL_ID;
    private static final Long COMPANY_ID = ClinicalHistoryMother.COMPANY_ID;

    @Mock
    private ClinicalEventRepository eventRepository;
    @Mock
    private AnimalReportQueryPort animalQueryPort;
    @Mock
    private ClinicalEventDetailQueryPort detailQueryPort;
    @Mock
    private AnimalAlertsQueryPort alertsQueryPort;
    @Mock
    private AnimalProblemsQueryPort problemsQueryPort;
    @Mock
    private ClinicalHistoryPdfPort pdfPort;

    private ExportClinicalHistoryService service;

    @org.junit.jupiter.api.BeforeEach
    void construirServicio() {
        service = new ExportClinicalHistoryService(eventRepository, animalQueryPort,
                detailQueryPort, alertsQueryPort, problemsQueryPort, pdfPort);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("arma el modelo con animal, eventos enriquecidos, alertas y problemas")
        void arma_el_modelo_completo() {
            GetClinicalHistoryQuery query = ClinicalHistoryMother.getHistoryQuery();
            AnimalReportInfo animal = ClinicalHistoryMother.animalReportInfo();
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(animal));

            ClinicalEvent conDetalle = new ClinicalEvent(1L, ANIMAL_ID, COMPANY_ID, 1L,
                    LocalDate.of(2026, 8, 1), null, ClinicalEventType.CONSULTATION, "Control");
            ClinicalEvent sinDetalle = new ClinicalEvent(2L, ANIMAL_ID, COMPANY_ID, null,
                    LocalDate.of(2026, 8, 2), null, ClinicalEventType.SURGERY, "");
            when(eventRepository.findHistory(query)).thenReturn(List.of(conDetalle, sinDetalle));

            ClinicalEventDetail detalle = ClinicalEventDetail.of("Consulta",
                    List.of(new DetailField("Diagnóstico", "Sano")));
            when(detailQueryPort.load(ClinicalEventType.CONSULTATION, 1L, COMPANY_ID))
                    .thenReturn(Optional.of(detalle));
            when(detailQueryPort.load(ClinicalEventType.SURGERY, 2L, COMPANY_ID))
                    .thenReturn(Optional.empty());

            List<ReportAlert> alertas = List.of(ClinicalHistoryMother.reportAlert());
            List<ReportProblem> problemas = List.of(ClinicalHistoryMother.reportProblem());
            when(alertsQueryPort.findByAnimal(ANIMAL_ID, COMPANY_ID)).thenReturn(alertas);
            when(problemsQueryPort.findByAnimal(ANIMAL_ID, COMPANY_ID)).thenReturn(problemas);

            byte[] pdfEsperado = {1, 2, 3};
            when(pdfPort.render(org.mockito.ArgumentMatchers.any())).thenReturn(pdfEsperado);

            byte[] resultado = service.execute(query);

            assertThat(resultado).isEqualTo(pdfEsperado);

            ArgumentCaptor<ClinicalHistoryReportModel> captor = ArgumentCaptor
                    .forClass(ClinicalHistoryReportModel.class);
            verify(pdfPort).render(captor.capture());
            ClinicalHistoryReportModel modelo = captor.getValue();
            assertThat(modelo.animal()).isEqualTo(animal);
            assertThat(modelo.from()).isEqualTo(query.from());
            assertThat(modelo.to()).isEqualTo(query.to());
            assertThat(modelo.typeFilters()).isEqualTo(query.types());
            assertThat(modelo.alerts()).isEqualTo(alertas);
            assertThat(modelo.problems()).isEqualTo(problemas);
            assertThat(modelo.events()).hasSize(2);
            // El evento CON detalle trae el titulo cargado por el puerto...
            assertThat(modelo.events().get(0).title()).isEqualTo("Consulta");
            assertThat(modelo.events().get(0).fields()).isEqualTo(detalle.fields());
            // ...el SIN detalle cae al fallback y, con summary vacio, al texto por defecto.
            assertThat(modelo.events().get(1).title()).isEqualTo("Sin descripción");
            assertThat(modelo.events().get(1).fields()).isEmpty();
        }

        @Test
        @DisplayName("un evento sin detalle pero con summary usa el summary como título de reserva")
        void fallback_usa_el_summary_cuando_existe() {
            GetClinicalHistoryQuery query = ClinicalHistoryMother.getHistoryQuery();
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(ClinicalHistoryMother.animalReportInfo()));
            ClinicalEvent evento = new ClinicalEvent(3L, ANIMAL_ID, COMPANY_ID, null,
                    LocalDate.of(2026, 8, 1), null, ClinicalEventType.SPA, "Baño y corte");
            when(eventRepository.findHistory(query)).thenReturn(List.of(evento));
            when(detailQueryPort.load(ClinicalEventType.SPA, 3L, COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(alertsQueryPort.findByAnimal(ANIMAL_ID, COMPANY_ID)).thenReturn(List.of());
            when(problemsQueryPort.findByAnimal(ANIMAL_ID, COMPANY_ID)).thenReturn(List.of());
            when(pdfPort.render(org.mockito.ArgumentMatchers.any())).thenReturn(new byte[0]);

            service.execute(query);

            ArgumentCaptor<ClinicalHistoryReportModel> captor = ArgumentCaptor
                    .forClass(ClinicalHistoryReportModel.class);
            verify(pdfPort).render(captor.capture());
            assertThat(captor.getValue().events().getFirst().title()).isEqualTo("Baño y corte");
        }

        @Test
        @DisplayName("sin eventos, el modelo se renderiza igual con la lista de eventos vacía")
        void sin_eventos_se_renderiza_igual() {
            GetClinicalHistoryQuery query = ClinicalHistoryMother.getHistoryQuery();
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(ClinicalHistoryMother.animalReportInfo()));
            when(eventRepository.findHistory(query)).thenReturn(List.of());
            when(alertsQueryPort.findByAnimal(ANIMAL_ID, COMPANY_ID)).thenReturn(List.of());
            when(problemsQueryPort.findByAnimal(ANIMAL_ID, COMPANY_ID)).thenReturn(List.of());
            when(pdfPort.render(org.mockito.ArgumentMatchers.any())).thenReturn(new byte[0]);

            service.execute(query);

            verifyNoInteractions(detailQueryPort);
            ArgumentCaptor<ClinicalHistoryReportModel> captor = ArgumentCaptor
                    .forClass(ClinicalHistoryReportModel.class);
            verify(pdfPort).render(captor.capture());
            assertThat(captor.getValue().events()).isEmpty();
        }
    }

    @Nested
    @DisplayName("tenancy — animal inexistente o de otra empresa")
    class Tenancy {

        @Test
        @DisplayName("animal no encontrado para la empresa actual aborta sin consultar nada más")
        void animal_no_encontrado_aborta_sin_consultar_nada_mas() {
            GetClinicalHistoryQuery query = ClinicalHistoryMother.getHistoryQuery();
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(query))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal " + ANIMAL_ID + " not found for current company");

            // Mismo puerto para "no existe" que para "es de otra empresa": el query port
            // ya filtra por companyId, así que el servicio no distingue los dos casos —
            // y por eso no debe tocar ningun otro colaborador en ninguno de los dos.
            verifyNoInteractions(eventRepository, detailQueryPort, alertsQueryPort,
                    problemsQueryPort, pdfPort);
        }
    }
}
