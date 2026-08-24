package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.dto.QuoteTotalsMismatchDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La consulta de vigilancia de la regla R5 —«los totales de una cotizacion
 * cuadran con la suma de sus lineas»— expuesta como caso de uso.
 *
 * <p>
 * <strong>Existia solo dentro de un documento.</strong>
 * {@code QuoteLineJpaEntity} justifica NO llevar
 * {@code @SQLRestriction("enabled = true")} diciendo que el descuadre que eso
 * pueda producir «lo caza la consulta de vigilancia de R5»; esa consulta era
 * SQL en un fichero de diseno y no la ejecutaba nada. El precio pactado por
 * leer las lineas desactivadas era que la alerta sonara en otro sitio, y ese
 * otro sitio no se habia construido. Incidencia #428.
 *
 * <p>
 * El escenario concreto: alguien desactiva una linea por SQL —una correccion
 * manual, un script de migracion, una limpieza—. La cabecera sigue diciendo
 * 119.000 sobre renglones que ya no suman eso, el documento se lee sin error
 * porque la linea desactivada se sigue leyendo, y nadie se entera nunca.
 *
 * <p>
 * Barre todos los tenants por definicion y no filtra por empresa, asi que solo
 * la puede servir {@code hasRole('SYSTEM')} a secas
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). Mismo patron que
 * {@code FindOverlappingSubscriptionItemsUseCase}, la vigilancia de R7.
 * <strong>Cero filas = sano.</strong>
 */
public interface FindQuoteTotalsMismatchesUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    List<QuoteTotalsMismatchDto> findAllTotalsMismatches();
}
