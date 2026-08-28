package com.vetsoftware.app.accountmapping.application.dto;

import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** <strong>Sin {@code version}</strong>: es una barandilla del que escribe. */
public record AccountMappingDto(Long id, MappingKind mappingKind, String mappingKey,
        Long catalogItemId, String chargeType, String taxTreatment, String debitAccountCode,
        String creditAccountCode, String deferredAccountCode, LocalDate validFrom,
        LocalDate validTo, LocalDateTime createdDate, boolean enabled) {

    public static AccountMappingDto from(AccountMapping mapping) {
        return new AccountMappingDto(mapping.getId(), mapping.getMappingKind(),
                mapping.getMappingKey(), mapping.getCatalogItemId(), mapping.getChargeType(),
                mapping.getTaxTreatment(), mapping.getDebitAccountCode(),
                mapping.getCreditAccountCode(), mapping.getDeferredAccountCode(),
                mapping.getValidFrom(), mapping.getValidTo(), mapping.getCreatedDate(),
                mapping.isEnabled());
    }
}
