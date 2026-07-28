package com.vetsoftware.app.infrastructure.pdf;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("vetsoftware.pdf")
public record PdfProperties(
        @Min(1) @DefaultValue("2") int maxConcurrentRenders,
        @NotNull @DefaultValue("30s") Duration acquireTimeout,
        @NotNull @DefaultValue("5MB") DataSize maxHtmlSize,
        @NotNull @DefaultValue("25MB") DataSize maxPdfSize
) {
    public PdfProperties {
        if (acquireTimeout.isZero() || acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("acquireTimeout debe ser positivo");
        }
        validateDataSize("maxHtmlSize", maxHtmlSize);
        validateDataSize("maxPdfSize", maxPdfSize);
    }

    private static void validateDataSize(String property, DataSize value) {
        if (value.toBytes() <= 0 || value.toBytes() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    property + " debe estar entre 1 byte y " + Integer.MAX_VALUE + " bytes");
        }
    }
}
