package com.vetsoftware.app.medicationschedule.application.port.out;

import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import java.util.List;
import java.util.Optional;

public interface MedicationScheduleRepository {
    MedicationSchedule save(MedicationSchedule medicationSchedule);

    Optional<MedicationSchedule> findById(Long id);

    List<MedicationSchedule> findByHospitalizationMedicationId(Long hospitalizationMedicationId);

    /**
     * Las tomas de la orden, solo si la orden es de {@code companyId}. La empresa
     * no cuelga de la toma: sube por la orden hasta la hospitalización.
     */
    List<MedicationSchedule> findByHospitalizationMedicationIdAndCompanyId(
            Long hospitalizationMedicationId, Long companyId);

    List<MedicationSchedule> findByHospitalizationId(Long hospitalizationId);

    /**
     * Las tomas de la hospitalización, solo si la hospitalización es de
     * {@code companyId}.
     */
    List<MedicationSchedule> findByHospitalizationIdAndCompanyId(Long hospitalizationId,
            Long companyId);

    void delete(Long id);

    /**
     * Sin acotar: solo el camino SYSTEM ({@code companyId == null}).
     */
    void disableByHospitalizationMedicationId(Long hospitalizationMedicationId);

    /**
     * Acotado al tenant: el {@code EXISTS} sube de la orden a la hospitalización.
     */
    void disableByHospitalizationMedicationId(Long hospitalizationMedicationId, Long companyId);

    /**
     * Deshabilita solo las tomas NO aplicadas (conserva el histórico de las
     * aplicadas).
     */
    void disablePendingByHospitalizationMedicationId(Long hospitalizationMedicationId);

    /**
     * Igual que {@link #disablePendingByHospitalizationMedicationId(Long)} pero
     * acotado al tenant. Aquí no hay lectura previa que valide la propiedad —el
     * servicio decide qué devolver mirando lo que quedó vivo—, así que este filtro
     * es la única barrera.
     */
    void disablePendingByHospitalizationMedicationId(Long hospitalizationMedicationId,
            Long companyId);
}
