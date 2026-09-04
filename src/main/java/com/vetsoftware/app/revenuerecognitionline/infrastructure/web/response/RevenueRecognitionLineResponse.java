package com.vetsoftware.app.revenuerecognitionline.infrastructure.web.response;

import com.vetsoftware.app.revenuerecognitionline.application.dto.RevenueRecognitionLineDto;
import com.vetsoftware.app.revenuerecognitionline.domain.RecognitionMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un renglon del libro tal como sale por HTTP. Solo lo ve la consola de
 * plataforma: el reconocimiento de ingreso es contabilidad de Lumbre.
 *
 * <p>
 * <strong>Lleva {@code companyId}, y aqui eso no es una fuga.</strong> El
 * anti-patron que persigue {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} es la empresa
 * que <em>entra</em> por un {@code XxxRequest}; esta sale, en una respuesta que
 * solo alcanza {@code hasRole('SYSTEM')}, y es el dato: la lectura util de este
 * libro es precisamente cruzada —el cierre mensual de todas las clinicas— y sin
 * saber de quien es cada renglon no se puede armar.
 *
 * <p>
 * <strong>No lleva {@code version} porque la entidad no la tiene</strong>: el
 * libro esta exento con codigo {@code E1_APPEND_ONLY}.
 */
public record RevenueRecognitionLineResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long chargeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "El mes al que se imputa el ingreso, en formato yyyy-MM.") String periodKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "El periodo contable en que quedo registrado. Nunca anterior a periodKey.") String postingPeriod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Puede ser negativo: una correccion es otra fila que compensa.") BigDecimal recognizedAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RecognitionMethod method,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static RevenueRecognitionLineResponse from(RevenueRecognitionLineDto dto) {
        return new RevenueRecognitionLineResponse(dto.id(), dto.companyId(), dto.chargeId(),
                dto.periodKey(), dto.postingPeriod(), dto.recognizedAmount(), dto.method(),
                dto.createdDate());
    }
}
