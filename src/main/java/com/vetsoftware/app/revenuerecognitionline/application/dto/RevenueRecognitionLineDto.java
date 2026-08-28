package com.vetsoftware.app.revenuerecognitionline.application.dto;

import com.vetsoftware.app.revenuerecognitionline.domain.RecognitionMethod;
import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLine;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code version} porque la entidad no la tiene</strong>: el libro
 * solo se agrega y figura en {@code ENTIDADES_EXENTAS_DE_VERSION} con codigo
 * {@code E1_APPEND_ONLY}.
 *
 * <p>
 * <strong>Si lleva {@code companyId}</strong>, al contrario que la mayoria de
 * DTOs del arbol: este libro solo lo lee plataforma, y su lectura util es
 * precisamente cruzada —el cierre mensual de todas las clinicas—, asi que saber
 * de quien es cada renglon es el dato, no una fuga.
 */
public record RevenueRecognitionLineDto(Long id, Long companyId, Long chargeId, String periodKey,
        String postingPeriod, BigDecimal recognizedAmount, RecognitionMethod method,
        LocalDateTime createdDate) {

    public static RevenueRecognitionLineDto from(RevenueRecognitionLine line) {
        return new RevenueRecognitionLineDto(line.getId(), line.getCompanyId(), line.getChargeId(),
                line.getPeriodKey(), line.getPostingPeriod(), line.getRecognizedAmount(),
                line.getMethod(), line.getCreatedDate());
    }
}
