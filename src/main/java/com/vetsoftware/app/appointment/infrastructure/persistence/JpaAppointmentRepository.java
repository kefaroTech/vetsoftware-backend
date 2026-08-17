package com.vetsoftware.app.appointment.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAppointmentRepository implements AppointmentRepository {
    private final AppointmentJpaRepository jpaRepository;
    private final AppointmentJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final OwnerJpaRepository ownerJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;
    private final BranchJpaRepository branchJpaRepository;

    public JpaAppointmentRepository(AppointmentJpaRepository jpaRepository,
            AppointmentJpaMapper mapper, AnimalJpaRepository animalJpaRepository,
            OwnerJpaRepository ownerJpaRepository, EmployeeJpaRepository employeeJpaRepository,
            CompanyJpaRepository companyJpaRepository, BranchJpaRepository branchJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.ownerJpaRepository = ownerJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
        this.branchJpaRepository = branchJpaRepository;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AnimalJpaEntity animal = appointment.getAnimal() == null
                ? null
                : animalJpaRepository.getReferenceById(appointment.getAnimal().id());
        OwnerJpaEntity owner = appointment.getOwner() == null
                ? null
                : ownerJpaRepository.getReferenceById(appointment.getOwner().id());
        EmployeeJpaEntity employee = employeeJpaRepository
                .getReferenceById(appointment.getEmployee().id());
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(appointment.getCompany().id());
        BranchJpaEntity branch = branchJpaRepository.getReferenceById(appointment.getBranch().id());
        AppointmentJpaEntity saved = jpaRepository
                .save(mapper.toJpa(appointment, animal, owner, employee, company, branch));
        return mapper.toDomain(saved, appointment.getAnimal(), appointment.getOwner(),
                appointment.getEmployee(), appointment.getCompany(), appointment.getBranch());
    }

    @Override
    public Optional<Appointment> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Appointment> findByFilters(Long companyId, LocalDateTime from, LocalDateTime to,
            Long employeeId, AppointmentStatus status, Long branchId) {
        String statusName = status == null ? null : status.name();
        return jpaRepository.findByFilters(companyId, from, to, employeeId, statusName, branchId)
                .stream().map(mapper::toDomain).toList();
    }

    /**
     * Intersección de intervalos <strong>semiabiertos</strong>:
     * {@code nuevoInicio < finExistente AND finNuevo > inicioExistente}. La
     * consulta acota la ventana y aquí se decide el cruce exacto — ver el javadoc
     * de {@code AppointmentJpaRepository#findOverlapCandidates} para por qué la
     * aritmética no está en el SQL.
     *
     * <p>
     * El semiabierto es la mitad del valor de la regla: una cita 10:00-10:30 y otra
     * 10:30-11:00 <strong>no</strong> se cruzan. Con intervalos cerrados, cada
     * agenda encadenada daría un falso conflicto y el bloqueo duraría una semana
     * antes de que alguien lo desactivara.
     */
    @Override
    public List<Overlap> findOverlapping(Long companyId, Long employeeId, LocalDateTime startAt,
            LocalDateTime endAt, int defaultDurationMinutes, Long excludeId) {
        // Cota inferior de la ventana: ninguna cita que empiece antes de
        // `startAt - la duración máxima posible` puede seguir en curso. El máximo es
        // el techo del dominio, salvo que llegue un default mayor (la política lo
        // descarta, pero el puerto acepta cualquier int).
        int longestPossible = Math.max(Appointment.MAX_DURATION_MINUTES, defaultDurationMinutes);
        LocalDateTime earliestStartAt = startAt.minusMinutes(longestPossible);

        return jpaRepository
                .findOverlapCandidates(companyId, employeeId, earliestStartAt, endAt,
                        AppointmentStatus.namesNotOccupyingSchedule(), excludeId)
                .stream().filter(slot -> overlaps(slot, startAt, endAt, defaultDurationMinutes))
                .map(slot -> new Overlap(slot.getId(), slot.getBranchId())).toList();
    }

    private static boolean overlaps(AppointmentJpaRepository.AppointmentSlotProjection slot,
            LocalDateTime startAt, LocalDateTime endAt, int defaultDurationMinutes) {
        Integer own = slot.getDurationMinutes();
        int minutes = own != null
                ? own
                : (defaultDurationMinutes > 0
                        ? defaultDurationMinutes
                        : Appointment.FALLBACK_DURATION_MINUTES);
        LocalDateTime slotEnd = slot.getStartAt().plusMinutes(minutes);
        // Semiabierto en los dos extremos: los `isBefore` son estrictos.
        return startAt.isBefore(slotEnd) && slot.getStartAt().isBefore(endAt);
    }

    @Override
    public void delete(Long id, Long companyId) {
        jpaRepository.softDelete(id, companyId);
    }
}
