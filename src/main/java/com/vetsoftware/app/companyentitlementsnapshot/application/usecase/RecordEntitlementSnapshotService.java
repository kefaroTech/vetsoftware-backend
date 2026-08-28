package com.vetsoftware.app.companyentitlementsnapshot.application.usecase;

import com.vetsoftware.app.companyentitlementsnapshot.application.command.RecordEntitlementSnapshotCommand;
import com.vetsoftware.app.companyentitlementsnapshot.application.dto.CompanyEntitlementSnapshotDto;
import com.vetsoftware.app.companyentitlementsnapshot.application.port.in.RecordEntitlementSnapshotUseCase;
import com.vetsoftware.app.companyentitlementsnapshot.application.port.out.CompanyEntitlementSnapshotRepository;
import com.vetsoftware.app.companyentitlementsnapshot.domain.CompanyEntitlementSnapshot;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda la foto de un recálculo.
 *
 * <p>
 * Va en la misma transacción que el recálculo a propósito: si el recálculo
 * revierte, no hubo permisos nuevos que fotografiar y una foto de un estado que
 * nunca existió sería peor que no tenerla. Es el caso opuesto al del portazo.
 */
@Service
public class RecordEntitlementSnapshotService implements RecordEntitlementSnapshotUseCase {

    private final CompanyEntitlementSnapshotRepository repository;
    private final Clock clock;

    public RecordEntitlementSnapshotService(CompanyEntitlementSnapshotRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyEntitlementSnapshotDto execute(RecordEntitlementSnapshotCommand command) {
        CompanyEntitlementSnapshot snapshot = CompanyEntitlementSnapshot.take(command.companyId(),
                LocalDateTime.now(clock), command.actor(), command.triggerReason(),
                command.amendmentId(), command.payload(), command.payloadFormatVersion());
        return CompanyEntitlementSnapshotDto.from(repository.append(snapshot));
    }
}
