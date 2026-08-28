package com.vetsoftware.app.companylimitevent.application.port.in;

import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.dto.CompanyLimitEventDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Escribe un hecho de cupo <strong>en su propia transacción</strong>, para que
 * sobreviva a la vuelta atrás del rechazo que documenta.
 *
 * <p>
 * Ese es el punto entero: la operación que topa con el techo se deshace, y si
 * el hecho viviera dentro de esa transacción se desharía con ella. Hoy no hay
 * ni una sola transacción independiente en el árbol de suscripciones, y por eso
 * el portazo no deja huella.
 *
 * <h2>Por qué el gate es la empresa y NO una autoridad</h2>
 *
 * <p>
 * <strong>Este puerto es un efecto del sistema, no una capacidad
 * concedible.</strong> No lo expone ningún endpoint —{@code
 * CompanyLimitEventController} solo declara lecturas—: lo llama
 * {@code LimitDenialAdapter} bajo el principal del empleado que acaba de topar
 * con el techo, como consecuencia de una operación que ese empleado <em>ya</em>
 * tenía permiso para intentar. Pedirle además un
 * {@code hasAuthority('companyLimitEvent.create')} sería un segundo candado en
 * la misma puerta, y modelar como permiso de usuario algo que el usuario nunca
 * pide.
 *
 * <p>
 * <strong>Y no habría dónde sembrar esa autoridad.</strong> {@code base_roles}
 * tiene una sola fila —{@code ADMIN}, desde el changeset 266—: el rol base de
 * empleado no existe en este esquema. Colgar el código de {@code ADMIN}, que es
 * lo único disponible, dejaría <strong>el portazo por cupo sin registrar para
 * todo empleado que no sea administrador</strong> —recepción, peluquería—, en
 * silencio y sin fallar nada visible, porque {@code LimitDenialAdapter} se
 * traga la {@code AccessDeniedException} y la registra como error. Es decir: la
 * bitácora se quedaría vacía justo en el caso que existe para probar. Por eso
 * {@code companyLimitEvent.create} nunca se sembró (ver el comentario del
 * changeset 370) y por eso el guard no lo nombra.
 *
 * <p>
 * Lo que sí está cerrado a plataforma es la <em>corrección del consumo</em>,
 * que es otro caso de uso y otro gate.
 */
public interface RecordLimitEventUseCase {

    @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#command.companyId)")
    CompanyLimitEventDto execute(RecordLimitEventCommand command);
}
