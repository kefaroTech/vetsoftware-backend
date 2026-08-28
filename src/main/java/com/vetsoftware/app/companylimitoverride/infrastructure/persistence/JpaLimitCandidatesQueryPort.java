package com.vetsoftware.app.companylimitoverride.infrastructure.persistence;

import com.vetsoftware.app.companylimitoverride.application.port.out.LimitCandidatesQueryPort;
import com.vetsoftware.app.companylimitoverride.domain.LimitCandidates;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * El único archivo de esta rodaja que conoce las tablas de contrato y de
 * catálogo, por la excepción acotada del {@code CLAUDE.md}.
 *
 * <p>
 * <strong>Traduce el par (modo, cantidad) a la convención del
 * resolutor</strong>, que es lo único que hace además de consultar: un techo en
 * modo {@code FULL} se convierte en {@code null} —«sin techo», el candidato que
 * gana a cualquier número— y un {@code LIMITED} viaja con su cifra. Esa
 * traducción vive aquí y en un solo sitio porque es donde muere el vocabulario
 * de la base; de la precedencia no sabe nada.
 */
@Component
public class JpaLimitCandidatesQueryPort implements LimitCandidatesQueryPort {

    private static final String UNLIMITED_MODE = "FULL";

    private final EffectiveLimitCandidateJpaRepository candidateJpaRepository;

    public JpaLimitCandidatesQueryPort(
            EffectiveLimitCandidateJpaRepository candidateJpaRepository) {
        this.candidateJpaRepository = candidateJpaRepository;
    }

    @Override
    public LimitCandidates findCandidates(Long companyId, Long limitDimensionId) {
        return new LimitCandidates(
                toQuantities(
                        candidateJpaRepository.findContractedCeilings(companyId, limitDimensionId)),
                toQuantities(
                        candidateJpaRepository.findFreeTierCeilings(companyId, limitDimensionId)),
                candidateJpaRepository.countContractsSignedAfterAxis(companyId,
                        limitDimensionId) > 0);
    }

    /**
     * {@code FULL} vacía la cantidad. <strong>Lista mutable y no
     * {@code List.copyOf}</strong>: el resultado admite elementos nulos a
     * propósito, y las fábricas inmutables de la JDK los rechazan con una
     * {@code NullPointerException} que aparecería solo en la empresa que sí tiene
     * un techo ilimitado.
     */
    private static List<Integer> toQuantities(List<LimitCeilingView> ceilings) {
        List<Integer> quantities = new ArrayList<>(ceilings.size());
        for (LimitCeilingView ceiling : ceilings)
            quantities.add(
                    UNLIMITED_MODE.equals(ceiling.getMode()) ? null : ceiling.getLimitQuantity());
        return quantities;
    }
}
