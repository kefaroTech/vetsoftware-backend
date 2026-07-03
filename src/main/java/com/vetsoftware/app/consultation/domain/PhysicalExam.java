package com.vetsoftware.app.consultation.domain;

import java.math.BigDecimal;

/**
 * Value object del examen físico / constantes vitales de una consulta (la "O" de SOAP).
 * Todos los campos son opcionales (una consulta puede no registrar examen). Las invariantes
 * (rangos y longitudes) viven aquí, en el dominio. Un examen "vacío" es válido.
 */
public record PhysicalExam(
        BigDecimal temperature,
        Integer heartRate,
        Integer respiratoryRate,
        String mucousMembranes,
        String capillaryRefill,
        String hydration,
        Integer bodyConditionScore,
        Integer painScore,
        String attitude,
        String findings
) {
    private static final BigDecimal MAX_TEMPERATURE = BigDecimal.valueOf(60);

    public PhysicalExam {
        mucousMembranes = blankToNull(mucousMembranes);
        capillaryRefill = blankToNull(capillaryRefill);
        hydration = blankToNull(hydration);
        attitude = blankToNull(attitude);
        findings = blankToNull(findings);

        if (temperature != null && (temperature.signum() < 0 || temperature.compareTo(MAX_TEMPERATURE) > 0))
            throw new IllegalArgumentException("temperature must be between 0 and 60");
        if (heartRate != null && (heartRate < 0 || heartRate > 1000))
            throw new IllegalArgumentException("heartRate must be between 0 and 1000");
        if (respiratoryRate != null && (respiratoryRate < 0 || respiratoryRate > 1000))
            throw new IllegalArgumentException("respiratoryRate must be between 0 and 1000");
        if (bodyConditionScore != null && (bodyConditionScore < 1 || bodyConditionScore > 9))
            throw new IllegalArgumentException("bodyConditionScore must be between 1 and 9");
        if (painScore != null && (painScore < 0 || painScore > 10))
            throw new IllegalArgumentException("painScore must be between 0 and 10");
        if (mucousMembranes != null && mucousMembranes.length() > 40)
            throw new IllegalArgumentException("mucousMembranes must be 40 chars or less");
        if (capillaryRefill != null && capillaryRefill.length() > 20)
            throw new IllegalArgumentException("capillaryRefill must be 20 chars or less");
        if (hydration != null && hydration.length() > 20)
            throw new IllegalArgumentException("hydration must be 20 chars or less");
        if (attitude != null && attitude.length() > 40)
            throw new IllegalArgumentException("attitude must be 40 chars or less");
        if (findings != null && findings.length() > 2000)
            throw new IllegalArgumentException("findings must be 2000 chars or less");
    }

    public static PhysicalExam empty() {
        return new PhysicalExam(null, null, null, null, null, null, null, null, null, null);
    }

    public boolean isEmpty() {
        return temperature == null && heartRate == null && respiratoryRate == null
                && mucousMembranes == null && capillaryRefill == null && hydration == null
                && bodyConditionScore == null && painScore == null && attitude == null
                && findings == null;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }
}
