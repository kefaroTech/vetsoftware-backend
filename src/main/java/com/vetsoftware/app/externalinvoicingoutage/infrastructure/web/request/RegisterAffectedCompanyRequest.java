package com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.request;

import com.vetsoftware.app.externalinvoicingoutage.domain.OutageResolution;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * <strong>Sin {@code companyId} en el cuerpo, a proposito y por regla
 * dura.</strong> {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} prohibe que la empresa
 * llegue en el JSON —ahi convierte cualquier comprobacion de propiedad en una
 * comparacion del numero consigo mismo—, asi que en este endpoint de plataforma
 * viaja como {@code @PathVariable}, que es donde la cubre la familia «por id».
 *
 * <p>
 * <strong>Por que lleva nombre propio en el contrato.</strong> Hay otro
 * {@code RegisterAffectedCompanyRequest} en
 * {@code securityincident.infrastructure.web.request}, y springdoc funde los
 * esquemas <em>por nombre simple de clase</em>, no por paquete. Los dos records
 * tienen campos <strong>disjuntos</strong> —aquel declara {@code affectedScope}
 * y {@code affectedSubjectCount}; este, {@code failedDocumentCount} y
 * {@code resolvedBy}—, asi que la fusion no degrada un campo: publica <em>el
 * cuerpo del otro endpoint</em>. Ganaba el de incidentes de seguridad por orden
 * de escaneo, y
 * {@code POST /system/external-invoicing-outages/{id}/companies/{companyId}}
 * quedaba anunciando un cuerpo que rechaza: un front generado desde el contrato
 * mandaria {@code affectedScope} a un endpoint que exige
 * {@code failedDocumentCount}, y el generador de tipos lo daria por bueno
 * porque el contrato lo respalda. Los dos campos de esta caida no aparecian en
 * ninguna parte del {@code openapi.json}.
 *
 * <p>
 * <strong>Por que el prefijo va en este lado y no en el otro.</strong> No es
 * arbitrario ni es «el ultimo que llego»: es el molde que la propia pareja de
 * respuestas ya publica. {@code securityincident} sirve
 * {@code AffectedCompanyResponse} y esta feature sirve
 * {@code OutageAffectedCompanyResponse} — el prefijo {@code Outage} es de esta
 * feature en el contrato, y aqui se repite para que el par peticion/respuesta
 * de cada slice lea igual en el {@code openapi.json}. El precedente de la
 * tecnica es {@code LimitDimensionSubModuleSummary}.
 *
 * <p>
 * El nombre de la clase Java <strong>no</strong> cambia: renombrarla obligaria
 * a tocar controller, tests y la rodaja web sin ganar nada, porque lo que
 * colisiona es el nombre publicado, no el simbolo.
 *
 * @param failedDocumentCount
 *            documentos que se quedaron sin transmitir. <b>Es el numero que
 *            sostiene la reclamacion</b>, y cero es legitimo: una clinica puede
 *            estar dentro del alcance sin haber intentado emitir nada en esa
 *            franja
 * @param resolvedBy
 *            como salio adelante. {@code CONTINGENCY_NUMBERING} es el que hay
 *            que poder demostrar ante la autoridad: justifica una serie de
 *            documentos emitidos fuera del camino normal
 */
@Schema(name = "RegisterOutageAffectedCompanyRequest")
public record RegisterAffectedCompanyRequest(
        @PositiveOrZero(message = "El numero de documentos fallidos no puede ser negativo.") int failedDocumentCount,
        @NotNull(message = "Debes indicar como se resolvio.") @Schema(description = "CONTINGENCY_NUMBERING justifica la numeracion usada durante la caida.") OutageResolution resolvedBy) {
}
