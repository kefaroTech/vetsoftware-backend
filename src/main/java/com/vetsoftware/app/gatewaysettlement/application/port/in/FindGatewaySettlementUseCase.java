package com.vetsoftware.app.gatewaysettlement.application.port.in;

import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindGatewaySettlementUseCase {

    /**
     * <strong>La lectura por id va cerrada a {@code hasRole('SYSTEM')} a secas, sin
     * la alternativa por permiso que llevan las lecturas de otros bloques —y es la
     * anotacion mas importante de la feature.</strong>
     *
     * <p>
     * Un {@code id} lo escribe el cliente en la URL y aqui no hay ningun
     * {@code companyId} con el que acotar la fila, porque la tabla no tiene
     * empresa. Abrirla por {@code hasAuthority} no daria «un poco mas de
     * informacion»: daria a cualquier empleado autenticado <b>el bruto, la comision
     * y el neto de un lote que agrupa los cobros de sesenta clinicas</b>, incluidas
     * sus competidoras, y con la referencia del lote —que el detalle de su propio
     * pago ya le enseña— como llave para pedirlo.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    GatewaySettlementDto findById(Long id);
}
