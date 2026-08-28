package com.vetsoftware.app.companylimitevent.application.port.in;

import com.vetsoftware.app.companylimitevent.application.dto.UsageReconciliationDto;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El recuento periodico de R-LIMIT-30: compara el contador con las filas reales
 * y deja escrito el resultado.
 *
 * <h2>Por que tiene que existir</h2>
 *
 * <p>
 * El modelo admite por escrito que {@code used_quantity} es una cache que puede
 * desviarse, y <strong>ninguna restriccion del motor puede demostrar que
 * cuadra</strong>. Sin un recuento, esa admision es una excusa en vez de una
 * salvaguarda: una migracion que inserta quinientas mascotas saltandose el caso
 * de uso, o un movimiento que se pierde por un fallo de red a mitad de
 * transaccion, dejan el contador diciendo 41 con 541 filas detras. La clinica
 * sigue creando hasta 100 creyendo que le quedan 59 y <em>nadie se entera
 * nunca</em>, porque no hay ningun proceso que compare. Cuando se empiece a
 * facturar excedente sobre esos ejes, la discrepancia se convierte en dinero
 * mal cobrado, en cualquiera de las dos direcciones.
 *
 * <h2>Que hace, y sobre todo que NO hace</h2>
 *
 * <p>
 * <strong>No sobrescribe el contador.</strong> Escribe un hecho compensatorio
 * {@code USAGE_RECONCILED} con el desvio (R-LIMIT-19, R-LIMIT-30). La
 * correccion es otra operacion, la firma una persona de plataforma y tiene su
 * propio caso de uso --{@code AdjustCompanyUsageUseCase}, la valvula de D-12--.
 * Fundirlas dejaria a un barrido nocturno moviendo cifras que acaban en una
 * factura, sin nadie que responda por ellas.
 *
 * <p>
 * <strong>Y sella {@code usage_reconciled_at} solo cuando cuadra</strong>, que
 * es la mitad que faltaba de R-ENT-13. Un contador desviado se queda sin sello
 * a proposito: el sello dice «comprobado y correcto», y ponerlo sobre el dato
 * que se acaba de demostrar malo es un indicador de salud que miente --peor que
 * no tener ninguno--.
 *
 * <h2>Autorizacion: {@code hasRole('SYSTEM')} a secas</h2>
 *
 * <p>
 * Recorre los contadores de <strong>todas</strong> las empresas: es un barrido
 * de plataforma y no hay empresa que acotar
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). Y aunque la hubiera, el mismo
 * razonamiento que cierra la correccion de consumo vale aqui: quien puede
 * declarar comprobado su propio contador puede declararlo sano sin contar nada.
 */
public interface ReconcileCompanyUsageUseCase {

    /**
     * @param staleBefore
     *            se examinan los contadores cuyo sello sea anterior a este
     *            instante; los que no tienen sello entran siempre
     * @param afterId
     *            cursor: id del ultimo contador del lote anterior, {@code 0} para
     *            empezar. Va por cursor y no por prioridad porque un contador con
     *            desvio no se sella y seguiria saliendo en todos los lotes
     * @param batchSize
     *            tope del lote, que acota la transaccion y no el trabajo
     */
    @PreAuthorize("hasRole('SYSTEM')")
    UsageReconciliationDto execute(LocalDateTime staleBefore, long afterId, int batchSize);
}
