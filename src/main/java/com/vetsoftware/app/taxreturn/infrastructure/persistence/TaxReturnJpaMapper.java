package com.vetsoftware.app.taxreturn.infrastructure.persistence;

import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Copia la version en los dos sentidos</strong>, y de eso depende que
 * presentar o anular sea una edicion y no un insert: con la version en
 * {@code null} sobre una entidad que ya tiene id, Hibernate la tomaria por
 * transitoria y escribiria una fila nueva que chocaria contra
 * {@code uq_tax_returns_case}.
 *
 * <p>
 * <strong>Es tambien el unico sitio donde el año cambia de forma.</strong> El
 * dominio lo expone como {@code int}; la entidad lo declara {@code short}
 * porque la columna es {@code SMALLINT} y con {@code ddl-auto: validate} un
 * {@code int} ahi impide construir el {@code SessionFactory}. El {@code cast}
 * es seguro porque el constructor del dominio ya rechazo cualquier año fuera de
 * 2020..2100.
 *
 * <p>
 * <strong>No toca ninguna de las tres columnas generadas</strong>
 * ({@code municipality_key}, {@code vat_frequency_year},
 * {@code current_return_marker}): las calcula MySQL y no estan mapeadas.
 */
@Component
public class TaxReturnJpaMapper {

    public TaxReturnJpaEntity toJpa(TaxReturn taxReturn) {
        TaxReturnJpaEntity entity = new TaxReturnJpaEntity();
        entity.setId(taxReturn.getId());
        entity.setTaxKind(taxReturn.getTaxKind());
        entity.setFiscalYear((short) taxReturn.getFiscalYear());
        entity.setFiscalPeriodKey(taxReturn.getFiscalPeriodKey());
        entity.setSequenceNumber(taxReturn.getSequenceNumber());
        entity.setMunicipalityCode(taxReturn.getMunicipalityCode());
        entity.setVatFrequency(taxReturn.getVatFrequency());
        entity.setStatus(taxReturn.getStatus());
        entity.setFiledAt(taxReturn.getFiledAt());
        entity.setFiledBySystemUserId(taxReturn.getFiledBySystemUserId());
        entity.setReceiptRef(taxReturn.getReceiptRef());
        entity.setFileRef(taxReturn.getFileRef());
        entity.setTotalGenerated(taxReturn.getTotalGenerated());
        entity.setTotalDeductible(taxReturn.getTotalDeductible());
        entity.setBalancePayable(taxReturn.getBalancePayable());
        entity.setBalanceCredit(taxReturn.getBalanceCredit());
        entity.setFirmezaUntil(taxReturn.getFirmezaUntil());
        entity.setCorrectsReturnId(taxReturn.getCorrectsReturnId());
        entity.setCreatedDate(taxReturn.getCreatedDate());
        entity.setVersion(taxReturn.getVersion());
        return entity;
    }

    public TaxReturn toDomain(TaxReturnJpaEntity entity) {
        return new TaxReturn(entity.getId(), entity.getTaxKind(), entity.getFiscalYear(),
                entity.getFiscalPeriodKey(), entity.getSequenceNumber(),
                entity.getMunicipalityCode(), entity.getVatFrequency(), entity.getStatus(),
                entity.getFiledAt(), entity.getFiledBySystemUserId(), entity.getReceiptRef(),
                entity.getFileRef(), entity.getTotalGenerated(), entity.getTotalDeductible(),
                entity.getBalancePayable(), entity.getBalanceCredit(), entity.getFirmezaUntil(),
                entity.getCorrectsReturnId(), entity.getCreatedDate(), entity.getVersion());
    }
}
