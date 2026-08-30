package com.vetsoftware.app.aiproposal.testsupport;

import com.vetsoftware.app.aiproposal.application.dto.ModelUsage;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.CapacityHint;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.LineAction;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalStatus;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.SanitizedReason;
import com.vetsoftware.app.aiproposal.domain.TurnStatus;
import com.vetsoftware.app.aiproposal.domain.TurnType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Las piezas persistidas de una propuesta: cabecera, turnos y lineas.
 *
 * <p>
 * <b>Todo se construye de verdad, nunca con un mock.</b> {@link AiProposal},
 * {@link ProposalTurn} y {@link ProposalLine} validan sus propias invariantes
 * en el constructor -que una linea aceptada resuelva a un articulo del
 * catalogo, que un turno de edicion no lleve salida de modelo, que una
 * propuesta anonimizada no conserve el correo-, y un doble se saltaria
 * justamente eso: el test pasaria con datos que produccion rechaza.
 */
public final class ProposalMother {

    /** 43 caracteres base64url, que es lo unico que {@code AiProposal} acepta. */
    public static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ";

    public static final String OTRO_TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPR";

    public static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    public static final String CLAVE = "11111111-2222-3333-4444-555555555555";

    public static final String CORREO = "laura@vetchapinero.co";

    public static final String MODELO = "anthropic.claude-sonnet-5";

    public static final String PROMPT = "v1";

    public static final Long ID_PROPUESTA = 7L;

    public static final Long ID_TARIFA = 4L;

    public static final Long ID_AVISO = 10L;

    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-08-30T15:00:00Z"),
            ZoneOffset.UTC);

    private ProposalMother() {
    }

    public static AiProposal propuesta(Long id) {
        return propuesta(id, CORREO);
    }

    public static AiProposal propuesta(Long id, String contactEmail) {
        AiProposal propuesta = AiProposal.create(TOKEN, ID_TARIFA, ProposalBillingCycle.MONTHLY,
                HASH, ID_AVISO, CLAVE, contactEmail, "es-CO", 14, RELOJ);
        propuesta.setId(id);
        return propuesta;
    }

    /** La unica forma de tener una cabecera con {@code version} ya asignada. */
    public static AiProposal propuestaConVersion(Long id, Long version) {
        LocalDateTime ahora = LocalDateTime.now(RELOJ);
        return new AiProposal(id, TOKEN, ProposalStatus.PROPOSED, ID_TARIFA,
                ProposalBillingCycle.MONTHLY, HASH, ID_AVISO, CLAVE, CORREO, "es-CO", 1, 100, 50,
                ahora, ahora, ahora.plusDays(14), null, ahora, version, true);
    }

    public static ProposalTurn turnoDeModelo(Long id, int numero, TurnType tipo, String texto) {
        LocalDateTime ahora = LocalDateTime.now(RELOJ);
        ProposalTurn turno = new ProposalTurn(id, ID_PROPUESTA, numero, tipo, TurnStatus.SUCCEEDED,
                texto, texto == null ? null : texto.length(), MODELO, PROMPT, 100, 50, 900,
                "end_turn", "{}", null, null, ahora, ahora, 0L);
        return turno;
    }

    public static ProposalTurn turnoInicial(Long id, String texto) {
        return turnoDeModelo(id, 1, TurnType.MODEL_INITIAL, texto);
    }

    public static ProposalTurn turnoDeRefinamiento(Long id, int numero, String texto) {
        return turnoDeModelo(id, numero, TurnType.MODEL_REFINEMENT, texto);
    }

    public static ProposalTurn turnoDeEdicion(Long id, int numero) {
        ProposalTurn turno = ProposalTurn.edicionDelCliente(ID_PROPUESTA, numero, null, RELOJ);
        turno.setId(id);
        return turno;
    }

    public static ProposalLine lineaDelModelo(Long turnId, String code, String precio, int orden) {
        return new ProposalLine(null, turnId, code, idsPorCodigo().get(code), LineAction.ADDED,
                LineSource.MODEL, LineVerdict.ACCEPTED, 1, new BigDecimal(precio),
                "El asistente propuso " + code, null, orden, LocalDateTime.now(RELOJ), null);
    }

    public static ProposalLine lineaPorCierre(Long turnId, String code, String precio, int orden) {
        return new ProposalLine(null, turnId, code, idsPorCodigo().get(code), LineAction.ADDED,
                LineSource.DEPENDENCY_CLOSURE, LineVerdict.ACCEPTED, 1, new BigDecimal(precio),
                null, null, orden, LocalDateTime.now(RELOJ), null);
    }

    public static ProposalLine lineaAnadidaPorElCliente(Long turnId, String code, String precio,
            int orden) {
        return new ProposalLine(null, turnId, code, idsPorCodigo().get(code), LineAction.ADDED,
                LineSource.CUSTOMER, LineVerdict.ACCEPTED, 1, new BigDecimal(precio), null, null,
                orden, LocalDateTime.now(RELOJ), null);
    }

    /** La huella de un {@code REMOVED} del cliente: sin importe y sin motivo. */
    public static ProposalLine lineaRetiradaPorElCliente(Long turnId, String code, int orden) {
        return new ProposalLine(null, turnId, code, idsPorCodigo().get(code), LineAction.REMOVED,
                LineSource.CUSTOMER, LineVerdict.ACCEPTED, 1, null, null, null, orden,
                LocalDateTime.now(RELOJ), null);
    }

    public static ProposalLine lineaRechazada(Long turnId, String code, LineVerdict verdict,
            int orden) {
        return new ProposalLine(null, turnId, code, null, LineAction.ADDED, LineSource.MODEL,
                verdict, 1, null, "El asistente propuso " + code, null, orden,
                LocalDateTime.now(RELOJ), null);
    }

    public static ModelUsage uso() {
        return new ModelUsage(MODELO, PROMPT, 1200, 340, 3800, "end_turn", "{\"ok\":true}",
                new BigDecimal("0.0042"));
    }

    public static ProposalGenerationResult exito(ProposalDraft draft) {
        return new ProposalGenerationResult(GenerationOutcome.SUCCEEDED, draft, uso(), null, null);
    }

    /**
     * El fallo del modelo: sin {@code usage} porque no hubo respuesta, con
     * latencia.
     */
    public static ProposalGenerationResult falloDelModelo() {
        return new ProposalGenerationResult(GenerationOutcome.MODEL_FAILED,
                ProposalDraft.sinLineas(false, false), null, "TIMEOUT", 4200);
    }

    public static ProposalGenerationResult resultadoDe(GenerationOutcome outcome,
            ProposalDraft draft) {
        if (outcome == GenerationOutcome.SUCCEEDED)
            return exito(draft);
        if (outcome == GenerationOutcome.MODEL_FAILED)
            return falloDelModelo();
        return ProposalGenerationResult.degradado(outcome);
    }

    public static ProposalDraft borrador(List<String> necesarios, List<String> recomendados) {
        Map<String, SanitizedReason> motivos = new LinkedHashMap<>();
        necesarios.forEach(
                code -> motivos.put(code, SanitizedReason.intacto("El asistente propuso " + code)));
        recomendados.forEach(
                code -> motivos.put(code, SanitizedReason.intacto("El asistente sugirio " + code)));
        return new ProposalDraft(true, false, necesarios, recomendados, motivos,
                CapacityHint.desconocido(), 0, List.of());
    }

    /**
     * Los identificadores de {@code catalog_items} de todo el catalogo del mother.
     */
    public static Map<String, Long> idsPorCodigo() {
        Map<String, Long> ids = new LinkedHashMap<>();
        ids.put("CORE", 101L);
        ids.put("CLINICAL_HISTORY", 102L);
        ids.put("VACCINATION", 103L);
        ids.put("SCHEDULING", 104L);
        ids.put("CASH_REGISTER", 105L);
        ids.put("LAB_IMAGING", 106L);
        ids.put("CAPACITY_TERMINAL", 107L);
        ids.put("EXTRA_USER", 108L);
        ids.put("DRAFT_MODULE", 109L);
        return ids;
    }
}
