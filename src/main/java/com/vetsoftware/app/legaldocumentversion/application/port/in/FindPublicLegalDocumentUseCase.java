package com.vetsoftware.app.legaldocumentversion.application.port.in;

import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * El texto vigente de un documento legal, para quien todavia no tiene cuenta.
 *
 * <p>
 * <strong>Es un puerto nuevo, no una relajacion del que habia.</strong> El
 * anterior -{@code FindCurrentLegalDocumentUseCase.findCurrentByCode(code,
 * companyId)}- exigia {@code hasRole('SYSTEM')} o
 * {@code hasAuthority('legaldocument.read') and @authz.isMyCompany(#companyId)},
 * y un prospecto no puede tener ninguna de las dos: se llevaba un 401. Su
 * parametro {@code companyId} <strong>no se usaba para nada</strong> -el
 * servicio lo ignoraba por completo-: existia solo para alimentar la expresion
 * SpEL, que es la forma exacta de anotacion que se lee bien y no protege nada.
 * Aqui no esta, y por eso no hay nada que revalidar.
 *
 * <p>
 * <strong>El agujero que cierra.</strong> Sin esta ruta el front del tenant
 * pintaba el aviso de privacidad desde una copia local del bundle, asi que el
 * {@code privacy_notice_version_id} que se persiste al lado de cada propuesta
 * apuntaba a una version que el prospecto <em>nunca vio servida desde
 * aqui</em>. Eso no es evidencia debil de consentimiento: es evidencia de otra
 * cosa.
 *
 * <p>
 * <strong>Publicar una version es un acto deliberado y su texto es publico por
 * definicion</strong> -la Ley 1581 exige que el aviso sea conocible antes de
 * recoger el dato-, asi que no hay nada aqui que un anonimo no deba leer. Lo
 * que <em>no</em> se abre es el resto de la rodaja: publicar sigue siendo
 * {@code SYSTEM}, y la relectura por huella y el listado por codigo siguen
 * exigiendo identidad.
 *
 * <p>
 * Hacer publica esta ruta son <strong>tres</strong> cosas y hay que hacer las
 * tres: esta anotacion, la linea literal en {@code PublicRoutes.BUSINESS} y la
 * misma linea en el inventario de {@code PublicRoutesTest}. Con solo la primera
 * el puerto queda abierto y nadie puede alcanzarlo; con solo la segunda,
 * {@code PUERTOS_AUTORIZADOS} rompe el build.
 */
@NoAuthorizationRequired(reason = "Es el aviso legal vigente que un prospecto sin cuenta tiene que poder leer ANTES de dar su correo: la autorizacion previa e informada del articulo 9 de la Ley 1581 no se puede probar si el texto que se le enseño salio de un fichero del bundle del front y no del servidor. El contenido es publico por definicion -publicarlo es el acto que lo hace oponible- y la respuesta no lleva dato de ninguna empresa ni de ningun titular. Solo se abre la lectura del vigente por codigo; publicar sigue siendo SYSTEM, y la relectura por huella y el listado de versiones siguen exigiendo identidad.")
public interface FindPublicLegalDocumentUseCase {

    LegalDocumentVersionDto findCurrentByCode(String code);
}
