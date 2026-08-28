package com.vetsoftware.app.smmlvvalue.application.dto;

import com.vetsoftware.app.smmlvvalue.domain.SmmlvStatus;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvValue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code inForce} viaja calculado y no como un {@code status == IN_FORCE} que
 * cada consumidor tendria que repetir: es la pregunta que se hace quien va a
 * usar la cifra, y repetirla en cinco features es como una de ellas acaba
 * tratando SUPERSEDED como vigente.
 */
public record SmmlvValueDto(Long id, int fiscalYear, BigDecimal valueAmount, String legalReference,
        SmmlvStatus status, String statusReference, LocalDate statusChangedOn, boolean inForce,
        LocalDateTime createdDate, boolean enabled) {

    public static SmmlvValueDto from(SmmlvValue value) {
        return new SmmlvValueDto(value.getId(), value.getFiscalYear(), value.getValueAmount(),
                value.getLegalReference(), value.getStatus(), value.getStatusReference(),
                value.getStatusChangedOn(), value.isInForce(), value.getCreatedDate(),
                value.isEnabled());
    }
}
