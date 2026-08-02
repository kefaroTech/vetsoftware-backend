package com.vetsoftware.app.infrastructure.audit.chain;

import java.util.List;

/**
 * Recalcula la cadena de hash y localiza la primera divergencia.
 *
 * <p>
 * Función pura sobre una lista de eslabones consecutivos: no toca base de datos
 * ni reloj, de modo que las reglas de integridad se pueden probar de forma
 * aislada.
 */
public final class AuditChainVerifier {

    private AuditChainVerifier() {
    }

    /**
     * @param links
     *            eslabones ordenados por posición ascendente
     * @param expectedFirstSequence
     *            posición que debe tener el primer eslabón
     * @param expectedPreviousHash
     *            {@code chain_hash} del eslabón anterior al primero de la lista, o
     *            {@link AuditChainHash#GENESIS_HASH} si se arranca desde el origen
     */
    public static Result verify(List<AuditChainRepository.Link> links, long expectedFirstSequence,
            String expectedPreviousHash) {

        long sequence = expectedFirstSequence - 1;
        String previousHash = expectedPreviousHash;
        int checked = 0;

        for (AuditChainRepository.Link link : links) {
            long expectedSequence = sequence + 1;

            // Un hueco significa que se eliminó un evento de la mitad de la cadena.
            if (link.sequence() != expectedSequence) {
                return Result.broken(checked, sequence, previousHash, link.sequence(),
                        "hueco en la cadena: se esperaba la posición " + expectedSequence
                                + " y se encontró " + link.sequence());
            }

            // El payload cambió después de insertarse.
            String recomputedPayloadHash = AuditChainHash.payloadHash(link.payload());
            if (!recomputedPayloadHash.equals(link.payloadHash())) {
                return Result.broken(checked, sequence, previousHash, link.sequence(),
                        "el payload no coincide con su hash almacenado");
            }

            // Se rompió la continuidad con el eslabón anterior.
            if (!previousHash.equals(link.previousHash())) {
                return Result.broken(checked, sequence, previousHash, link.sequence(),
                        "previous_hash no coincide con el chain_hash del eslabón anterior");
            }

            // Se reescribió el eslabón para encajar con un payload alterado.
            String recomputedChainHash = AuditChainHash.chainHash(link.previousHash(),
                    link.sequence(), link.payloadHash());
            if (!recomputedChainHash.equals(link.chainHash())) {
                return Result.broken(checked, sequence, previousHash, link.sequence(),
                        "chain_hash no coincide con el recálculo");
            }

            sequence = link.sequence();
            previousHash = link.chainHash();
            checked++;
        }

        return new Result(true, checked, sequence, previousHash, 0, null);
    }

    /**
     * @param intact
     *            si no se encontró divergencia
     * @param checkedCount
     *            eslabones verificados correctamente
     * @param lastVerifiedSequence
     *            última posición válida; punto de partida de la siguiente pasada
     * @param lastVerifiedHash
     *            {@code chain_hash} de esa posición
     * @param failureSequence
     *            posición donde se detectó la divergencia, 0 si no hubo
     * @param failureReason
     *            motivo de la divergencia, nulo si no hubo
     */
    public record Result(boolean intact, int checkedCount, long lastVerifiedSequence,
            String lastVerifiedHash, long failureSequence, String failureReason) {

        static Result broken(int checked, long lastVerifiedSequence, String lastVerifiedHash,
                long failureSequence, String reason) {
            return new Result(false, checked, lastVerifiedSequence, lastVerifiedHash,
                    failureSequence, reason);
        }
    }
}
