package com.vetsoftware.app.entitlement.application.port.in;

import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Los contadores que el recuento periodico tiene pendientes: los que nadie ha
 * comprobado nunca contra las filas reales y los comprobados hace demasiado.
 *
 * <h2>Autorizacion: {@code hasRole('SYSTEM')} a secas, y no puede ser otra
 * cosa</h2>
 *
 * <p>
 * <strong>Es un listado que no filtra por empresa</strong>, porque la pregunta
 * es de plataforma: «que contadores hay sin comprobar, en todo el sistema». La
 * regla dura {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} exige exactamente este
 * gate para ese caso, y acotar por una clave foranea ajena no contaria como
 * acotar por empresa. Abrirlo al tenant dejaria a cualquier empleado de
 * cualquier clinica leer el estado de los contadores de todas las demas.
 *
 * <p>
 * Devuelve el contador tal cual esta, sin contar nada: quien cuenta las filas
 * reales es el recuento, que vive en {@code companylimitevent} porque su
 * producto es un hecho de la bitacora.
 */
public interface ListUnreconciledCapacityCountersUseCase {

    /**
     * @param afterId
     *            id del ultimo contador del lote anterior; {@code 0} para empezar.
     *            El recorrido avanza por cursor porque un contador con desvio no se
     *            sella y sigue siendo «pendiente» despues de examinarlo: sin cursor
     *            el barrido volveria a leer las mismas filas para siempre
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<CompanyCapacityDto> list(LocalDateTime staleBefore, long afterId, int limit);
}
