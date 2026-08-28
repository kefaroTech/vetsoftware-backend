package com.vetsoftware.app.companytrialgrant.application.port.in;

import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El barrido de vencimientos: las pruebas vivas que ya pasaron su fecha.
 *
 * <p>
 * <strong>Barre por la fecha de cada línea, no por el estado del
 * contrato</strong> (R-TRIAL-15): un día de mora no puede matar la prueba para
 * siempre, y el reloj de la prueba no se pausa por mora —pausarlo exigiría
 * mover la fecha, que es justo el vector que la capa cierra—.
 *
 * <p>
 * <strong>El último día es inclusivo</strong>: una prueba que termina el 30 de
 * septiembre sigue viva todo ese día. Quien llama pasa el día que corresponda
 * en la zona horaria del negocio; este puerto no lo deriva del reloj del
 * servidor.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas, que es lo que exige
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} a un listado que no filtra por
 * empresa. Su hermano acotado es {@code ListCompanyTrialGrantsUseCase}.
 */
public interface ListExpiredTrialGrantsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    List<CompanyTrialGrantDto> listLiveExpiredOn(LocalDate day);
}
