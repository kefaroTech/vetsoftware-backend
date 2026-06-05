package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("productChargeOpenAccountJpaOpenAccountQueryPort")
public class JpaOpenAccountQueryPort implements OpenAccountQueryPort {
    private final OpenAccountJpaRepository openAccountJpaRepository;

    public JpaOpenAccountQueryPort(OpenAccountJpaRepository openAccountJpaRepository) {
        this.openAccountJpaRepository = openAccountJpaRepository;
    }

    @Override
    public Optional<OpenAccountRef> findById(Long openAccountId) {
        return openAccountJpaRepository.findById(openAccountId)
            .map(e -> new OpenAccountRef(e.getId(), e.getCompany().getId()));
    }
}
