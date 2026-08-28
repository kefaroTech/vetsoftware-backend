package com.vetsoftware.app.companytrialwindow.infrastructure.persistence;

import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindow;
import org.springframework.stereotype.Component;

/** El único sitio que conoce a la vez la ventana de dominio y su fila. */
@Component
public class CompanyTrialWindowJpaMapper {

    public CompanyTrialWindowJpaEntity toJpa(CompanyTrialWindow window) {
        CompanyTrialWindowJpaEntity entity = new CompanyTrialWindowJpaEntity();
        entity.setId(window.getId());
        entity.setCompanyId(window.getCompanyId());
        entity.setStartDate(window.getStartDate());
        entity.setEndDate(window.getEndDate());
        entity.setWindowDays(window.getWindowDays());
        entity.setSourceQuoteId(window.getSourceQuoteId());
        entity.setClosedAt(window.getClosedAt());
        entity.setCreatedDate(window.getCreatedDate());
        entity.setVersion(window.getVersion());
        return entity;
    }

    public CompanyTrialWindow toDomain(CompanyTrialWindowJpaEntity entity) {
        return new CompanyTrialWindow(entity.getId(), entity.getCompanyId(), entity.getStartDate(),
                entity.getEndDate(), entity.getWindowDays(), entity.getSourceQuoteId(),
                entity.getClosedAt(), entity.getCreatedDate(), entity.getVersion());
    }
}
