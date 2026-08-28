package com.vetsoftware.app.companylimitevent.application.port.in;

import com.vetsoftware.app.companylimitevent.application.command.AdjustCompanyUsageCommand;
import com.vetsoftware.app.companylimitevent.application.dto.CompanyLimitEventDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Corrige el consumo de un contador y deja escrito el hecho que lo compensa.
 *
 * <p>
 * Existe porque el contador acumulativo no perdona: si un cliente migra mal y
 * carga quinientas mascotas duplicadas, sin una válvula de escape auditable
 * soporte acabará escribiendo en la base de producción, que es la peor versión
 * de la misma operación.
 *
 * <h2>Autorización: {@code hasRole('SYSTEM')} a secas, y esto no es
 * negociable</h2>
 *
 * <p>
 * <strong>Si este gate admitiera al tenant, la administradora de la clínica
 * recuperaría su propio cupo cada vez que topa</strong> — y el cupo dejaría de
 * existir sin que ninguna fila del modelo estuviera mal. Por eso la corrección
 * <em>no</em> puede colgar del controller que resuelve la empresa desde quien
 * firma la petición: la empresa llega en el command porque quien la escribe es
 * plataforma, actuando sobre un tenant que no es el suyo.
 *
 * <p>
 * Es deliberadamente <strong>otro caso de uso</strong> que el que mueve el
 * contador durante una operación normal del cliente. Aquel tiene que admitir al
 * tenant —es él quien crea la mascota—; este no. Fusionarlos por parecerse
 * reabriría el agujero.
 *
 * <p>
 * <strong>La corrección no sobrescribe nada</strong>: mueve el contador con la
 * instrucción atómica de siempre y escribe un hecho {@code USAGE_ADJUSTED} con
 * motivo obligatorio, para que la cifra siga siendo demostrable.
 */
public interface AdjustCompanyUsageUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CompanyLimitEventDto execute(AdjustCompanyUsageCommand command);
}
