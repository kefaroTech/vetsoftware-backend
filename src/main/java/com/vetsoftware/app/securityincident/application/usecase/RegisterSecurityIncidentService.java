package com.vetsoftware.app.securityincident.application.usecase;

import com.vetsoftware.app.securityincident.application.command.RegisterSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.application.port.in.RegisterSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.application.port.out.BusinessDayDeadlinePort;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de alta el incidente y le fija el vencimiento del reporte.
 *
 * <h2>Quince dias habiles desde el escalamiento</h2>
 *
 * <p>
 * Circular Unica de la SIC, Titulo V, numeral 2.1, literal f), romanillo (ii).
 * El <em>dies a quo</em> es el escalamiento interno —cuando el incidente llega
 * al area que lo atiende—, <strong>no la deteccion</strong>. Contarlo desde
 * {@code detectedAt} alarga el plazo, y un plazo mas largo del real es un
 * incumplimiento que nadie ve venir: el error siempre cae del lado de
 * incumplir.
 *
 * <h2>El calendario no se reimplementa</h2>
 *
 * <p>
 * {@link BusinessDayDeadlinePort} delega en el bloque de festivos, que es el
 * unico sitio del producto que sabe convertir «N dias habiles» en una fecha.
 * <strong>Si el tramo cargado no cubre el recorrido, esto falla</strong> con
 * {@code HolidayCalendarGapException} (409) en vez de degradar a dias corridos:
 * un incidente sin plazo calculable tiene que ser ruidoso, porque el arreglo es
 * sembrar los festivos del año y no aproximar.
 *
 * <h2>De fecha a instante</h2>
 *
 * <p>
 * El calendario devuelve un dia; la columna es {@code DATETIME(6)}. El
 * vencimiento se sella al <strong>final</strong> de ese dia habil
 * ({@code 23:59:59.999999}) porque el plazo se agota cuando el dia termina, no
 * cuando empieza: usar {@code atStartOfDay()} recortaria una jornada entera de
 * un plazo que la ley concede. Los microsegundos son el limite de
 * {@code DATETIME(6)} —{@code LocalTime.MAX} lleva nanos y MySQL los truncaria,
 * asi que se escribe el valor exacto que la columna sabe guardar—.
 *
 * <p>
 * Que {@code deadline_at > escalated_at} se cumple <strong>por
 * construccion</strong> y no por suerte: el calendario no cuenta el dia de
 * partida, asi que la fecha que devuelve es siempre posterior al dia del
 * escalamiento, y el final de un dia posterior es posterior a cualquier hora
 * del dia del escalamiento. El dominio lo vuelve a comprobar de todos modos,
 * que es donde vive la invariante.
 */
@Observed(name = "security.incident.register")
@Service
public class RegisterSecurityIncidentService implements RegisterSecurityIncidentUseCase {

    /**
     * Fin del dia habil con la precision exacta de {@code DATETIME(6)}.
     * {@code LocalTime.MAX} no sirve: sus nanos los trunca MySQL y el valor leido
     * dejaria de ser el escrito.
     */
    private static final LocalTime FIN_DE_JORNADA = LocalTime.of(23, 59, 59, 999_999_000);

    private final SecurityIncidentRepository repository;
    private final BusinessDayDeadlinePort businessDayDeadlinePort;
    private final Clock clock;

    public RegisterSecurityIncidentService(SecurityIncidentRepository repository,
            BusinessDayDeadlinePort businessDayDeadlinePort, Clock clock) {
        this.repository = repository;
        this.businessDayDeadlinePort = businessDayDeadlinePort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SecurityIncidentDto execute(RegisterSecurityIncidentCommand command) {
        if (command.escalatedAt() == null)
            throw new IllegalArgumentException("escalatedAt is required: the fifteen business day"
                    + " deadline runs from the internal escalation, not from the detection");
        LocalDateTime deadlineAt = resolveDeadline(command.escalatedAt());
        SecurityIncident incident = SecurityIncident.register(command.detectedAt(),
                command.occurredAt(), command.escalatedAt(), command.kind(), command.severity(),
                command.summary(), command.affectedSubjectCount(), deadlineAt,
                LocalDateTime.now(clock));
        return SecurityIncidentDto.from(repository.save(incident));
    }

    /**
     * Quince dias habiles desde el dia del escalamiento, sellados al final de la
     * jornada del dia de vencimiento.
     */
    private LocalDateTime resolveDeadline(LocalDateTime escalatedAt) {
        LocalDate dueDate = businessDayDeadlinePort.resolve(escalatedAt.toLocalDate(),
                SecurityIncident.PLAZO_REPORTE_SIC_DIAS_HABILES);
        return dueDate.atTime(FIN_DE_JORNADA);
    }
}
