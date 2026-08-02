package com.vetsoftware.app.medicationschedule.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de dominio puro: genera las tomas (MedicationSchedule) de una orden
 * de medicación a partir de su frecuencia, duración y hora de inicio. Sin
 * dependencias de Spring ni de infraestructura.
 */
public final class MedicationScheduleGenerator {

    /** Horizonte por defecto cuando la duración es indefinida o no especificada. */
    private static final int INDEFINITE_HORIZON_DAYS = 14;

    /** Tope de seguridad de tomas generadas. */
    private static final int MAX_SLOTS = 90;

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(8, 0);

    private MedicationScheduleGenerator() {
    }

    public static List<MedicationSchedule> generate(MedicationOrderParams params,
            EmployeeRef createdBy) {
        List<MedicationSchedule> slots = new ArrayList<>();
        if (params.startDate() == null)
            return slots;

        LocalTime time = params.startTime() != null ? params.startTime() : DEFAULT_START_TIME;
        LocalDateTime start = LocalDateTime.of(params.startDate(), time);
        HospitalizationMedicationRef ref = new HospitalizationMedicationRef(params.id(),
                params.name());

        String frequency = params.frequency() == null
                ? ""
                : params.frequency().trim().toUpperCase();
        if ("CONTINUOUS".equals(frequency))
            return slots;
        if ("SINGLE".equals(frequency)) {
            slots.add(slotAt(ref, start, createdBy));
            return slots;
        }

        Integer intervalHours = intervalFromFrequency(frequency);
        if (intervalHours == null)
            return slots; // frecuencia no discreta / desconocida

        int count = doseCount(params.durationMeasure(), params.durationQuantity(), intervalHours);
        for (int i = 0; i < count; i++) {
            slots.add(slotAt(ref, start.plusHours((long) i * intervalHours), createdBy));
        }
        return slots;
    }

    /**
     * Regla de integridad: regenera SOLO las tomas pendientes conservando las ya
     * aplicadas. - Pauta INTERVALO con aplicadas: re-cronometra desde la hora real
     * de la última aplicada (`lastAppliedRealDateTime`) + intervalo. - Pauta FIJA
     * (o INTERVALO sin aplicadas): usa las horas de reloj saltando las primeras
     * `appliedCount` posiciones (ya cubiertas por aplicadas). - DOSES: el total
     * objetivo manda; pendientes = total - appliedCount (nunca negativo).
     */
    public static List<MedicationSchedule> generatePending(MedicationOrderParams params,
            int appliedCount, LocalDateTime lastAppliedRealDateTime, EmployeeRef createdBy) {
        List<MedicationSchedule> slots = new ArrayList<>();
        if (params.startDate() == null)
            return slots;

        LocalTime time = params.startTime() != null ? params.startTime() : DEFAULT_START_TIME;
        LocalDateTime start = LocalDateTime.of(params.startDate(), time);
        HospitalizationMedicationRef ref = new HospitalizationMedicationRef(params.id(),
                params.name());

        String frequency = params.frequency() == null
                ? ""
                : params.frequency().trim().toUpperCase();
        if ("CONTINUOUS".equals(frequency))
            return slots;
        if ("SINGLE".equals(frequency)) {
            if (appliedCount >= 1)
                return slots;
            slots.add(slotAt(ref, start, createdBy));
            return slots;
        }

        Integer intervalHours = intervalFromFrequency(frequency);
        if (intervalHours == null)
            return slots;

        int total = doseCount(params.durationMeasure(), params.durationQuantity(), intervalHours);
        int pendingCount = Math.max(0, total - appliedCount);
        if (pendingCount == 0)
            return slots;

        boolean intervalMode = "INTERVAL".equalsIgnoreCase(params.guidelineType());
        if (intervalMode && lastAppliedRealDateTime != null) {
            LocalDateTime cursor = lastAppliedRealDateTime;
            for (int i = 0; i < pendingCount; i++) {
                cursor = cursor.plusHours(intervalHours);
                slots.add(slotAt(ref, cursor, createdBy));
            }
        } else {
            for (int idx = appliedCount; idx < total; idx++) {
                slots.add(slotAt(ref, start.plusHours((long) idx * intervalHours), createdBy));
            }
        }
        return slots;
    }

    private static MedicationSchedule slotAt(HospitalizationMedicationRef ref, LocalDateTime dt,
            EmployeeRef createdBy) {
        return MedicationSchedule.create(ref, dt, dt, AppliedStatus.PENDING, false, createdBy);
    }

    private static Integer intervalFromFrequency(String frequency) {
        return intervalHours(frequency);
    }

    /** Horas del intervalo de una frecuencia, o null si no es discreta. */
    public static Integer intervalHours(String frequency) {
        String f = frequency == null ? "" : frequency.trim().toUpperCase();
        return switch (f) {
            case "EVERY_4H" -> 4;
            case "EVERY_6H" -> 6;
            case "EVERY_8H" -> 8;
            case "EVERY_12H" -> 12;
            case "EVERY_24H" -> 24;
            default -> null;
        };
    }

    private static int doseCount(String durationMeasure, Integer durationQuantity,
            int intervalHours) {
        String measure = durationMeasure == null ? "" : durationMeasure.trim().toUpperCase();
        int count;
        if ("DOSES".equals(measure) && durationQuantity != null && durationQuantity > 0) {
            count = durationQuantity;
        } else if ("DAYS".equals(measure) && durationQuantity != null && durationQuantity > 0) {
            count = (durationQuantity * 24) / intervalHours;
        } else {
            // INDEFINITE o sin duración → horizonte acotado
            count = (INDEFINITE_HORIZON_DAYS * 24) / intervalHours;
        }
        return Math.max(1, Math.min(count, MAX_SLOTS));
    }
}
