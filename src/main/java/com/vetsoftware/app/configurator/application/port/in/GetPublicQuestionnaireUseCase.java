package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.dto.QuestionnaireQuestionDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import java.util.List;

/**
 * El cuestionario tal como lo lee un prospecto que todavía no es cliente.
 *
 * <p>
 * Es el único caso de lectura anónima del slice y vive
 * <strong>separado</strong> de los puertos de administración a propósito:
 * mezclar «lo que puede ver el mundo» con «lo que puede editar SYSTEM» en un
 * mismo puerto convierte cualquier campo nuevo del lado de administración en
 * una fuga silenciosa hacia la respuesta pública. Por eso devuelve
 * {@code QuestionnaireQuestionDto} y no {@code ConfiguratorQuestionDto}: la
 * forma pública es más pobre por diseño.
 *
 * <p>
 * Su ruta ({@code GET /configurator/questionnaire}) está declarada en
 * {@code PublicRoutes.BUSINESS}; sin eso el {@code AuthFilter} la rechazaría
 * con un 401 antes de llegar aquí.
 */
@NoAuthorizationRequired(reason = "Lo lee el asistente de venta del front público: quien lo consulta es un prospecto sin cuenta, y exigir token haría imposible cotizar antes de ser cliente. No devuelve dato alguno de ninguna empresa — las tres tablas del configurador no tienen company_id — y es de solo lectura.")
public interface GetPublicQuestionnaireUseCase {

    /** Las preguntas activas con sus opciones activas, en orden de presentación. */
    List<QuestionnaireQuestionDto> get();
}
