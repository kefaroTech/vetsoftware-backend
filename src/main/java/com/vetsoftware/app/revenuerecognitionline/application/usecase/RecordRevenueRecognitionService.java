package com.vetsoftware.app.revenuerecognitionline.application.usecase;

import com.vetsoftware.app.revenuerecognitionline.application.command.RecordRevenueRecognitionCommand;
import com.vetsoftware.app.revenuerecognitionline.application.dto.RevenueRecognitionLineDto;
import com.vetsoftware.app.revenuerecognitionline.application.port.in.RecordRevenueRecognitionUseCase;
import com.vetsoftware.app.revenuerecognitionline.application.port.out.AccountingPeriodQueryPort;
import com.vetsoftware.app.revenuerecognitionline.application.port.out.RevenueRecognitionLineRepository;
import com.vetsoftware.app.revenuerecognitionline.application.port.out.SubscriptionChargeValidationPort;
import com.vetsoftware.app.revenuerecognitionline.domain.NoOpenPostingPeriodException;
import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLine;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra un renglon de reconocimiento de ingreso.
 *
 * <h2>Aqui vive la unica de las cuatro reglas de periodo que la base no puede
 * imponer</h2>
 *
 * <p>
 * «Un hecho tardio se reconoce en el <b>primer periodo abierto</b>» es una
 * comprobacion de conjunto: el motor no la puede resolver dentro de un
 * {@code INSERT} sin una consulta que ademas cambiaria el resultado segun el
 * orden de las filas. Por eso el {@code postingPeriod} <b>no viene en el
 * command</b>: lo resuelve este metodo, y las otras tres reglas quedan de red —
 * {@code chk_rrl_not_backwards} impide ir hacia atras y el disparador
 * {@code trg_rrl_bi_period_open} rechaza escribir en un mes cerrado si la
 * resolucion fallara.
 *
 * <p>
 * <strong>Si no hay ningun periodo abierto, para.</strong> No inventa uno ni
 * escribe en el mes cerrado mas cercano: cualquiera de las dos cosas alteraria
 * un periodo ya declarado en silencio. Ver
 * {@link NoOpenPostingPeriodException}.
 *
 * <h2>El cargo se valida con las DOS columnas</h2>
 *
 * <p>
 * {@code existsByIdAndCompanyId} y no un {@code existsById} pelado: la clave
 * foranea real es compuesta {@code (company_id, charge_id)} y sin las dos el
 * ingreso de una clinica podria colgar del cargo de otra. Es exactamente el
 * defecto que {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} persigue,
 * con el agravante de que aqui el resultado no seria un rechazo sino un libro
 * contable cruzado.
 *
 * <h2>El reloj va inyectado</h2>
 *
 * <p>
 * {@code LocalDateTime.now(clock)}: un {@code now()} pelado seria una fecha que
 * ningun test puede fijar y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el
 * build por ello.
 */
@Observed(name = "revenue.recognition.record")
@Service
public class RecordRevenueRecognitionService implements RecordRevenueRecognitionUseCase {

    private final RevenueRecognitionLineRepository repository;
    private final SubscriptionChargeValidationPort subscriptionChargeValidationPort;
    private final AccountingPeriodQueryPort accountingPeriodQueryPort;
    private final Clock clock;

    public RecordRevenueRecognitionService(RevenueRecognitionLineRepository repository,
            SubscriptionChargeValidationPort subscriptionChargeValidationPort,
            AccountingPeriodQueryPort accountingPeriodQueryPort, Clock clock) {
        this.repository = repository;
        this.subscriptionChargeValidationPort = subscriptionChargeValidationPort;
        this.accountingPeriodQueryPort = accountingPeriodQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RevenueRecognitionLineDto execute(RecordRevenueRecognitionCommand command) {
        validateCharge(command);
        String postingPeriod = accountingPeriodQueryPort
                .findFirstOpenPostingPeriodFrom(command.periodKey())
                .orElseThrow(() -> new NoOpenPostingPeriodException(command.periodKey()));
        RevenueRecognitionLine line = RevenueRecognitionLine.record(command.companyId(),
                command.chargeId(), command.periodKey(), postingPeriod, command.recognizedAmount(),
                command.method(), LocalDateTime.now(clock));
        return RevenueRecognitionLineDto.from(repository.save(line));
    }

    /**
     * El cargo tiene que existir <b>y ser de esa clinica</b>. Sin la segunda mitad,
     * el reconocimiento de la empresa A colgaria del cargo de la B y el ingreso de
     * una acabaria contado en el libro de la otra.
     */
    private void validateCharge(RecordRevenueRecognitionCommand command) {
        if (command.companyId() == null || command.chargeId() == null)
            throw new IllegalArgumentException("companyId and chargeId are required");
        if (!subscriptionChargeValidationPort.existsByIdAndCompanyId(command.chargeId(),
                command.companyId()))
            throw new IllegalArgumentException("Subscription charge " + command.chargeId()
                    + " does not exist for company " + command.companyId());
    }
}
