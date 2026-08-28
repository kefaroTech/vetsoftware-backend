package com.vetsoftware.app.accountingexport.infrastructure.persistence;

import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Copia la version en los dos sentidos</strong>, y de eso depende que
 * marcar un desenlace sea una edicion y no un insert: con la version en
 * {@code null} sobre una entidad que ya tiene id, Hibernate la tomaria por
 * transitoria y escribiria una fila nueva, que chocaria contra
 * {@code uq_accounting_exports_attempt}.
 *
 * <p>
 * <strong>No toca {@code current_export_marker}</strong>: la calcula MySQL y no
 * esta mapeada.
 */
@Component
public class AccountingExportJpaMapper {

    public AccountingExportJpaEntity toJpa(AccountingExport export) {
        AccountingExportJpaEntity entity = new AccountingExportJpaEntity();
        entity.setId(export.getId());
        entity.setPeriodKey(export.getPeriodKey());
        entity.setExportKind(export.getExportKind());
        entity.setAttemptNumber(export.getAttemptNumber());
        entity.setStatus(export.getStatus());
        entity.setGeneratedAt(export.getGeneratedAt());
        entity.setGeneratedBySystemUserId(export.getGeneratedBySystemUserId());
        entity.setTotalDebit(export.getTotalDebit());
        entity.setTotalCredit(export.getTotalCredit());
        entity.setTotalsHash(export.getTotalsHash());
        entity.setFileRef(export.getFileRef());
        entity.setDeliveredAt(export.getDeliveredAt());
        entity.setRejectedAt(export.getRejectedAt());
        entity.setRejectionReason(export.getRejectionReason());
        entity.setCreatedDate(export.getCreatedDate());
        entity.setVersion(export.getVersion());
        return entity;
    }

    public AccountingExport toDomain(AccountingExportJpaEntity entity) {
        return new AccountingExport(entity.getId(), entity.getPeriodKey(), entity.getExportKind(),
                entity.getAttemptNumber(), entity.getStatus(), entity.getGeneratedAt(),
                entity.getGeneratedBySystemUserId(), entity.getTotalDebit(),
                entity.getTotalCredit(), entity.getTotalsHash(), entity.getFileRef(),
                entity.getDeliveredAt(), entity.getRejectedAt(), entity.getRejectionReason(),
                entity.getCreatedDate(), entity.getVersion());
    }
}
