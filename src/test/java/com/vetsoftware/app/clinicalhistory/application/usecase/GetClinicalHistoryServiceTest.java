package com.vetsoftware.app.clinicalhistory.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetClinicalHistoryServiceTest {

    private final StubRepository repository = new StubRepository();
    private final GetClinicalHistoryService service = new GetClinicalHistoryService(repository);

    @Test
    void returns_empty_list_when_animal_has_no_events() {
        GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(
                1L, 10L, List.of(), null, null);

        List<ClinicalEventDto> result = service.execute(query);

        assertTrue(result.isEmpty());
        assertEquals(query, repository.lastQuery);
    }

    @Test
    void maps_repository_events_to_dtos_preserving_order() {
        repository.events.add(new ClinicalEvent(
                42L, 1L, 10L, 17L, LocalDate.of(2026, 5, 10), null,
                ClinicalEventType.SURGERY, "Esterilización"));
        repository.events.add(new ClinicalEvent(
                17L, 1L, 10L, null, LocalDate.of(2026, 3, 1), null,
                ClinicalEventType.CONSULTATION, "Control rutinario"));

        List<ClinicalEventDto> result = service.execute(
                new GetClinicalHistoryQuery(1L, 10L, List.of(), null, null));

        assertEquals(2, result.size());
        assertEquals(42L, result.get(0).sourceId());
        assertEquals(ClinicalEventType.SURGERY, result.get(0).eventType());
        assertEquals("Esterilización", result.get(0).summary());
        assertEquals(17L, result.get(1).sourceId());
        assertEquals(ClinicalEventType.CONSULTATION, result.get(1).eventType());
    }

    @Test
    void forwards_filters_to_repository() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<ClinicalEventType> types = List.of(
                ClinicalEventType.SURGERY, ClinicalEventType.VACCINATION);
        GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(1L, 10L, types, from, to);

        service.execute(query);

        assertEquals(query, repository.lastQuery);
        assertEquals(types, repository.lastQuery.types());
        assertEquals(from, repository.lastQuery.from());
        assertEquals(to, repository.lastQuery.to());
    }

    @Test
    void rejects_query_when_to_is_before_from() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);

        assertThrows(IllegalArgumentException.class,
                () -> new GetClinicalHistoryQuery(1L, 10L, List.of(), from, to));
    }

    @Test
    void rejects_query_when_animal_id_is_null() {
        assertThrows(IllegalArgumentException.class,
                () -> new GetClinicalHistoryQuery(null, 10L, List.of(), null, null));
    }

    @Test
    void rejects_query_when_company_id_is_null() {
        assertThrows(IllegalArgumentException.class,
                () -> new GetClinicalHistoryQuery(1L, null, List.of(), null, null));
    }

    @Test
    void normalizes_null_types_to_empty_list() {
        GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(
                1L, 10L, null, null, null);

        assertTrue(query.types().isEmpty());
    }

    private static class StubRepository implements ClinicalEventRepository {
        private final List<ClinicalEvent> events = new ArrayList<>();
        private GetClinicalHistoryQuery lastQuery;

        @Override
        public List<ClinicalEvent> findHistory(GetClinicalHistoryQuery query) {
            lastQuery = query;
            return List.copyOf(events);
        }
    }
}
