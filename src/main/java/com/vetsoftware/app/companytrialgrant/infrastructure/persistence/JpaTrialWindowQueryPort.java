package com.vetsoftware.app.companytrialgrant.infrastructure.persistence;

import com.vetsoftware.app.companytrialgrant.application.port.out.TrialWindowQueryPort;
import com.vetsoftware.app.companytrialgrant.domain.TrialWindowRef;
import com.vetsoftware.app.companytrialwindow.infrastructure.persistence.CompanyTrialWindowJpaEntity;
import com.vetsoftware.app.companytrialwindow.infrastructure.persistence.CompanyTrialWindowJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El único archivo de esta feature que conoce {@code companytrialwindow}, por
 * la excepción acotada del {@code CLAUDE.md}.
 *
 * <p>
 * <strong>Las dos búsquedas llevan la empresa.</strong> La segunda existe para
 * el camino en que el id de ventana viene de fuera; sin el {@code companyId} en
 * el {@code WHERE}, una concesión podría colgar de la ventana de otra clínica y
 * heredar un techo ajeno.
 */
@Component
public class JpaTrialWindowQueryPort implements TrialWindowQueryPort {

    private final CompanyTrialWindowJpaRepository windowJpaRepository;

    public JpaTrialWindowQueryPort(CompanyTrialWindowJpaRepository windowJpaRepository) {
        this.windowJpaRepository = windowJpaRepository;
    }

    @Override
    public Optional<TrialWindowRef> findOpenByCompanyId(Long companyId) {
        return windowJpaRepository.findByCompanyIdAndClosedAtIsNull(companyId)
                .map(JpaTrialWindowQueryPort::toRef);
    }

    @Override
    public Optional<TrialWindowRef> findByIdAndCompanyId(Long trialWindowId, Long companyId) {
        return windowJpaRepository.findByIdAndCompanyId(trialWindowId, companyId)
                .map(JpaTrialWindowQueryPort::toRef);
    }

    private static TrialWindowRef toRef(CompanyTrialWindowJpaEntity entity) {
        return new TrialWindowRef(entity.getId(), entity.getCompanyId(), entity.getStartDate(),
                entity.getEndDate(), entity.getClosedAt() == null);
    }
}
