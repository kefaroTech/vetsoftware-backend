package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventTypeCountDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class JpaClinicalEventRepository implements ClinicalEventRepository {
    private static final List<ClinicalEventType> ALL_TYPES = Arrays
            .asList(ClinicalEventType.values());

    private final ClinicalEventJpaRepository jpaRepository;
    private final ClinicalEventJpaMapper mapper;

    public JpaClinicalEventRepository(ClinicalEventJpaRepository jpaRepository,
            ClinicalEventJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<ClinicalEvent> findHistory(GetClinicalHistoryQuery query) {
        List<ClinicalEventType> types = query.types().isEmpty() ? ALL_TYPES : query.types();
        return jpaRepository.findHistory(query.animalId(), query.companyId(), types, query.from(),
                query.to(), query.q(), query.consultationId()).stream().map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<ClinicalEvent> findHistoryPage(GetClinicalHistoryQuery query, int page,
            int pageSize) {
        List<ClinicalEventType> types = query.types().isEmpty() ? ALL_TYPES : query.types();
        // Sin Sort explicito: el orden lo fija la propia consulta.
        Page<ClinicalEventViewJpaEntity> result = jpaRepository.findHistoryPage(query.animalId(),
                query.companyId(), types, query.from(), query.to(), query.q(),
                query.consultationId(), Pages.request(page, pageSize));
        return Pages.result(result, mapper::toDomain);
    }

    @Override
    public List<ClinicalEventTypeCountDto> countByType(Long animalId, Long companyId) {
        return jpaRepository.countByType(animalId, companyId);
    }

    @Override
    public List<ClinicalEvent> findByCompany(ListCompanyClinicalEventsQuery query) {
        List<ClinicalEventType> types = query.types().isEmpty() ? ALL_TYPES : query.types();
        return jpaRepository.findByCompany(query.companyId(), types, query.from(), query.to())
                .stream().map(mapper::toDomain).toList();
    }
}
