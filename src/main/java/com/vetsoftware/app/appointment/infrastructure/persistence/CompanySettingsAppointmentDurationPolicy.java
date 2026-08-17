package com.vetsoftware.app.appointment.infrastructure.persistence;

import com.vetsoftware.app.appointment.application.port.out.AppointmentDurationPolicyPort;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.companysettings.infrastructure.persistence.CompanySettingJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Duración por defecto de las citas respaldada por {@code company_settings}
 * (por empresa). El ajuste {@code appointment.default_duration_minutes} lo
 * togglea el admin de cada empresa (ver CompanySettingController); ausencia de
 * la fila = <strong>30 minutos</strong>. Mismo patrón que
 * {@code CompanySettingsNegativeStockPolicy} del inventario y
 * {@code CompanySettingsCashRequiredPolicy} de caja: la tabla es clave-valor,
 * así que añadir un ajuste no necesita migración.
 *
 * <p>
 * El parseo es deliberadamente defensivo. El {@code value} es un
 * {@code VARCHAR(255)} de texto libre que se escribe por el PUT genérico de
 * ajustes: nada impide que un admin teclee «treinta», «0» o «-15». Cualquiera
 * de esos casos cae al respaldo de <strong>30 minutos</strong> en vez de
 * reventar el agendamiento entero (mismo criterio que {@code JpaUvtQueryPort}).
 * También se descarta lo que supere el techo del dominio, porque la consulta de
 * solapes acota su ventana con ese máximo.
 */
@Component
public class CompanySettingsAppointmentDurationPolicy implements AppointmentDurationPolicyPort {

    private static final String KEY = "appointment.default_duration_minutes";

    /** Respaldo cuando la empresa no configuró nada: 30 minutos. */
    private static final int DEFAULT_MINUTES = 30;

    private final CompanySettingJpaRepository companySettingJpaRepository;

    public CompanySettingsAppointmentDurationPolicy(
            CompanySettingJpaRepository companySettingJpaRepository) {
        this.companySettingJpaRepository = companySettingJpaRepository;
    }

    @Override
    public int defaultDurationMinutes(Long companyId) {
        if (companyId == null)
            return DEFAULT_MINUTES;
        return companySettingJpaRepository.findByCompanyIdAndPropertyName(companyId, KEY)
                .map(e -> e.getValue()).flatMap(CompanySettingsAppointmentDurationPolicy::parse)
                .orElse(DEFAULT_MINUTES);
    }

    /**
     * Texto libre → minutos. Devuelve vacío —y con ello el respaldo de 30 minutos—
     * ante un valor no numérico, no positivo o por encima del techo del dominio.
     */
    private static Optional<Integer> parse(String value) {
        if (value == null || value.isBlank())
            return Optional.empty();
        try {
            int minutes = Integer.parseInt(value.trim());
            if (minutes <= 0 || minutes > Appointment.MAX_DURATION_MINUTES)
                return Optional.empty();
            return Optional.of(minutes);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
