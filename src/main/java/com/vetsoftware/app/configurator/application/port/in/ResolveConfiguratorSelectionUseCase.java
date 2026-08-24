package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.command.ResolveConfiguratorSelectionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorSelectionDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Dadas unas respuestas, qué artículos y en qué cantidad. Es lo que consumirá
 * {@code quote} para armar la cotización.
 *
 * <p>
 * <strong>Por qué no lleva gate:</strong> lo invoca el flujo de cotización de
 * un prospecto anónimo, que por definición no tiene principal. Un
 * {@code hasRole('SYSTEM')} aquí no protegería nada —la entrada son ids de
 * opción y la salida ids de artículo del catálogo global, sin un solo dato de
 * ninguna clínica— y en cambio rompería el único caso de uso para el que
 * existe.
 *
 * <p>
 * Son dos decisiones distintas y conviene no confundirlas: esta anotación dice
 * que el <em>puerto</em> no comprueba permisos; que el <em>endpoint</em> se
 * sirva sin token lo dice {@code PublicRoutes}. Durante un tiempo dijeron cosas
 * contrarias —el puerto abierto, la ruta cerrada—, así que el prospecto que
 * este javadoc describe recibía un 401 al resolver. Hoy
 * {@code POST /configurator/resolve} está en {@code PublicRoutes.BUSINESS},
 * igual que {@link GetPublicQuestionnaireUseCase}, y con su propio límite de
 * tasa por ser un {@code POST} anónimo.
 */
@NoAuthorizationRequired(reason = "Lo invoca el flujo de cotización de un prospecto anónimo, que no tiene principal; un gate lo dejaría inservible. Ni la entrada ni la salida contienen datos de ninguna empresa: son ids del cuestionario y del catálogo global de plataforma, y la operación es de solo lectura.")
public interface ResolveConfiguratorSelectionUseCase {

    /**
     * <strong>Sin gate no significa sin control.</strong> Que este puerto no
     * compruebe permisos hace más importante, no menos, que compruebe las
     * respuestas: quien lo invoca controla el cuerpo entero de la petición. Las
     * respuestas que no encajan en el árbol del cuestionario se rechazan antes de
     * resolver nada.
     *
     * @throws com.vetsoftware.app.configurator.domain.UnreachableAnswerException
     *             si alguna respuesta pertenece a una rama que esas mismas
     *             respuestas no activaron
     * @throws com.vetsoftware.app.configurator.domain.MissingRequiredAnswerException
     *             si una pregunta obligatoria de una rama activa llegó sin
     *             responder
     */
    ConfiguratorSelectionDto resolve(ResolveConfiguratorSelectionCommand command);
}
