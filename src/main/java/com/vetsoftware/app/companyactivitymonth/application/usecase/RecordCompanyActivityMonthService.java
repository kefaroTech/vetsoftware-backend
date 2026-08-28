package com.vetsoftware.app.companyactivitymonth.application.usecase;

import com.vetsoftware.app.companyactivitymonth.application.command.RecordCompanyActivityMonthCommand;
import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.companyactivitymonth.application.port.in.RecordCompanyActivityMonthUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyActivityMonthRepository;
import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.companyactivitymonth.domain.ActivityPeriodKey;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de alta la fila de actividad de una clinica en un mes.
 *
 * <p>
 * <strong>El service hace exactamente dos cosas, y ninguna es validar los
 * numeros.</strong> Comprueba que la empresa existe —que es un hecho externo,
 * hay que preguntarselo a otra tabla— y sella la fecha de creacion con el reloj
 * inyectado. Todo lo demas —que el periodo tenga forma de mes, que los dias
 * activos quepan en ese mes concreto, que los contadores no sean negativos, que
 * el MRR no traiga un tercer decimal— son invariantes y viven en el constructor
 * de {@link CompanyActivityMonth}. Ahi no se pueden saltar; aqui si, llamando
 * al constructor desde otro sitio.
 *
 * <p>
 * <strong>La unicidad no se comprueba preguntando antes, y es a
 * proposito.</strong> {@code uq_cam_month (company_id, period_key)} la cuida la
 * base. Un {@code exists} previo seria una comprobacion que dos peticiones
 * concurrentes pasarian las dos —o los dos reintentos del proceso nocturno, que
 * es el caso realista— y dejaria dos filas para el mismo mes. Aqui el duplicado
 * llega como violacion de integridad y el adaptador lo traduce a
 * {@code CompanyActivityMonthAlreadyExistsException}, que ademas dice cual es
 * la salida correcta: recalcular, no reinsertar.
 *
 * <p>
 * <strong>El reloj entra inyectado</strong> y no se llama a
 * {@code LocalDateTime.now()} aqui dentro: una fila que cruza medianoche entre
 * dos lineas solo se reproduce en CI y de noche
 * ({@code RELOJ_INYECTADO_EN_VEZ_DE_NOW}).
 */
@Observed(name = "companyactivitymonth.record")
@Service
public class RecordCompanyActivityMonthService implements RecordCompanyActivityMonthUseCase {

    private final CompanyActivityMonthRepository repository;
    private final CompanyValidationPort companyValidationPort;
    private final Clock clock;

    public RecordCompanyActivityMonthService(CompanyActivityMonthRepository repository,
            CompanyValidationPort companyValidationPort, Clock clock) {
        this.repository = repository;
        this.companyValidationPort = companyValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyActivityMonthDto execute(RecordCompanyActivityMonthCommand command) {
        validateCompany(command.companyId());
        CompanyActivityMonth month = CompanyActivityMonth.record(command.companyId(),
                new ActivityPeriodKey(command.periodKey()), command.commercialState(),
                command.activeDays(), command.activeUsers(), command.recordsCreated(),
                command.mrrSnapshot(), LocalDateTime.now(clock));
        return CompanyActivityMonthDto.from(repository.save(month));
    }

    /**
     * La empresa tiene que existir, o {@code fk_cam_company} rechazaria la fila mas
     * tarde y como un error de integridad en vez de como el «esa empresa no existe»
     * que corresponde.
     */
    private void validateCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required");
        }
        if (!companyValidationPort.existsById(companyId)) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
    }
}
