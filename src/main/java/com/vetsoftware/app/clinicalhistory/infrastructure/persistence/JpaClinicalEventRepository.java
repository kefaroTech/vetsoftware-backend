package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaClinicalEventRepository implements ClinicalEventRepository {
    private static final List<ClinicalEventType> ALL_TYPES =
            Arrays.asList(ClinicalEventType.values());

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
        return jpaRepository.findHistory(
                query.animalId(),
                query.companyId(),
                types,
                query.from(),
                query.to()
        ).stream().map(mapper::toDomain).toList();
    }
}
