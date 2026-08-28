package com.vetsoftware.app.companyentitlementsnapshot.application.usecase;

import com.vetsoftware.app.companyentitlementsnapshot.application.dto.CompanyEntitlementSnapshotDto;
import com.vetsoftware.app.companyentitlementsnapshot.application.port.in.FindEntitlementSnapshotAsOfUseCase;
import com.vetsoftware.app.companyentitlementsnapshot.application.port.out.CompanyEntitlementSnapshotRepository;
import com.vetsoftware.app.companyentitlementsnapshot.domain.CompanyEntitlementSnapshotNotFoundException;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Qué veía una empresa un día concreto. */
@Service
public class FindEntitlementSnapshotAsOfService implements FindEntitlementSnapshotAsOfUseCase {

    private final CompanyEntitlementSnapshotRepository repository;

    public FindEntitlementSnapshotAsOfService(CompanyEntitlementSnapshotRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyEntitlementSnapshotDto findLatestAsOf(Long companyId, LocalDateTime at) {
        return CompanyEntitlementSnapshotDto.from(repository.findLatestAsOf(companyId, at)
                .orElseThrow(() -> new CompanyEntitlementSnapshotNotFoundException(companyId, at)));
    }
}
