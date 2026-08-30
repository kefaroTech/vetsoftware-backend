package com.vetsoftware.app.aiproposal.infrastructure.ai;

/**
 * El par de bloques que se le manda al modelo, con lo que hay que persistir del
 * turno.
 *
 * @param system
 *            estatico byte a byte salvo el bloque de catalogo. Que sea estable
 *            es lo que hace cacheable el prefijo el dia que el volumen lo
 *            justifique, y lo que hace comparables dos turnos de la misma
 *            propuesta en el golden set
 * @param user
 *            ⛔ <strong>contiene el texto del prospecto en claro.</strong> Es el
 *            unico sitio del backend donde eso es correcto, y por eso
 *            {@link #toString()} no lo imprime
 * @param catalogSnapshotHash
 *            SHA-256 del bloque de catalogo, hints incluidos. La invalidacion
 *            del golden set es una comparacion de 64 bytes contra
 *            {@code ai_proposals.catalog_snapshot_hash}
 */
public record ProposalPrompt(String system, String user, String promptVersion,
        String catalogSnapshotHash) {

    /**
     * <strong>Nunca el bloque {@code user}.</strong> Este objeto viaja por el
     * adaptador entero y es el candidato natural a acabar en un log de depuracion o
     * en un atributo de span; si {@link #toString()} lo imprimiera, R1 del anexo B
     * —"el texto libre no sale por ninguna senal"— se romperia en la primera traza
     * que alguien anada. El {@code record} genera un {@code toString} que vuelca
     * todos los componentes, asi que hay que sobreescribirlo a mano.
     */
    @Override
    public String toString() {
        return "ProposalPrompt[version=" + promptVersion + ", hash=" + catalogSnapshotHash
                + ", userChars=" + (user == null ? 0 : user.length()) + "]";
    }
}
