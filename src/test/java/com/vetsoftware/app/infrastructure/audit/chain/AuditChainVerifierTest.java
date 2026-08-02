package com.vetsoftware.app.infrastructure.audit.chain;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.infrastructure.audit.chain.AuditChainRepository.Link;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditChainVerifierTest {

  @Test
  void una_cadena_bien_formada_se_considera_intacta() {
    List<Link> chain = buildChain("uno", "dos", "tres");

    AuditChainVerifier.Result result =
        AuditChainVerifier.verify(chain, 1, AuditChainHash.GENESIS_HASH);

    assertThat(result.intact()).isTrue();
    assertThat(result.checkedCount()).isEqualTo(3);
    assertThat(result.lastVerifiedSequence()).isEqualTo(3);
    assertThat(result.lastVerifiedHash()).isEqualTo(chain.getLast().chainHash());
    assertThat(result.failureReason()).isNull();
  }

  @Test
  void alterar_el_payload_rompe_la_cadena() {
    List<Link> chain = new ArrayList<>(buildChain("uno", "dos", "tres"));
    Link original = chain.get(1);
    // El atacante reescribe el contenido y deja los hashes como estaban.
    chain.set(
        1,
        new Link(
            original.sequence(),
            "payload manipulado",
            original.payloadHash(),
            original.previousHash(),
            original.chainHash()));

    AuditChainVerifier.Result result =
        AuditChainVerifier.verify(chain, 1, AuditChainHash.GENESIS_HASH);

    assertThat(result.intact()).isFalse();
    assertThat(result.failureSequence()).isEqualTo(2);
    assertThat(result.failureReason()).contains("payload no coincide");
    assertThat(result.lastVerifiedSequence()).isEqualTo(1);
  }

  @Test
  void eliminar_un_evento_del_medio_deja_un_hueco_detectable() {
    List<Link> chain = new ArrayList<>(buildChain("uno", "dos", "tres"));
    chain.remove(1);

    AuditChainVerifier.Result result =
        AuditChainVerifier.verify(chain, 1, AuditChainHash.GENESIS_HASH);

    assertThat(result.intact()).isFalse();
    assertThat(result.failureSequence()).isEqualTo(3);
    assertThat(result.failureReason()).contains("hueco en la cadena");
  }

  @Test
  void recalcular_el_eslabon_tras_manipular_el_payload_rompe_la_continuidad() {
    List<Link> chain = new ArrayList<>(buildChain("uno", "dos", "tres"));
    // Atacante más cuidadoso: reescribe el payload Y su hash, y recalcula su propio eslabón.
    // No puede recalcular los posteriores sin que se note, porque el siguiente sigue apuntando
    // al eslabón viejo.
    Link target = chain.get(1);
    String forgedPayload = "payload manipulado";
    String forgedPayloadHash = AuditChainHash.payloadHash(forgedPayload);
    String forgedChainHash =
        AuditChainHash.chainHash(target.previousHash(), target.sequence(), forgedPayloadHash);
    chain.set(
        1,
        new Link(
            target.sequence(),
            forgedPayload,
            forgedPayloadHash,
            target.previousHash(),
            forgedChainHash));

    AuditChainVerifier.Result result =
        AuditChainVerifier.verify(chain, 1, AuditChainHash.GENESIS_HASH);

    assertThat(result.intact()).isFalse();
    assertThat(result.failureSequence()).isEqualTo(3);
    assertThat(result.failureReason()).contains("previous_hash no coincide");
  }

  @Test
  void reescribir_el_eslabon_sin_tocar_el_payload_se_detecta() {
    List<Link> chain = new ArrayList<>(buildChain("uno", "dos"));
    Link target = chain.get(1);
    chain.set(
        1,
        new Link(
            target.sequence(),
            target.payload(),
            target.payloadHash(),
            target.previousHash(),
            AuditChainHash.payloadHash("eslabon inventado")));

    AuditChainVerifier.Result result =
        AuditChainVerifier.verify(chain, 1, AuditChainHash.GENESIS_HASH);

    assertThat(result.intact()).isFalse();
    assertThat(result.failureSequence()).isEqualTo(2);
    assertThat(result.failureReason()).contains("chain_hash no coincide");
  }

  @Test
  void una_ventana_que_arranca_despues_de_la_depuracion_se_verifica_desde_su_primer_eslabon() {
    List<Link> full = buildChain("uno", "dos", "tres", "cuatro");
    // La depuración eliminó los dos primeros; solo quedan las posiciones 3 y 4.
    List<Link> retained = full.subList(2, 4);

    AuditChainVerifier.Result result =
        AuditChainVerifier.verify(retained, 3, retained.getFirst().previousHash());

    assertThat(result.intact()).isTrue();
    assertThat(result.checkedCount()).isEqualTo(2);
    assertThat(result.lastVerifiedSequence()).isEqualTo(4);
  }

  @Test
  void una_lista_vacia_no_reporta_rotura() {
    AuditChainVerifier.Result result =
        AuditChainVerifier.verify(List.of(), 1, AuditChainHash.GENESIS_HASH);

    assertThat(result.intact()).isTrue();
    assertThat(result.checkedCount()).isZero();
    assertThat(result.lastVerifiedSequence()).isZero();
  }

  private static List<Link> buildChain(String... payloads) {
    List<Link> links = new ArrayList<>(payloads.length);
    String previousHash = AuditChainHash.GENESIS_HASH;
    long sequence = 0;
    for (String payload : payloads) {
      sequence++;
      String payloadHash = AuditChainHash.payloadHash(payload);
      String chainHash = AuditChainHash.chainHash(previousHash, sequence, payloadHash);
      links.add(new Link(sequence, payload, payloadHash, previousHash, chainHash));
      previousHash = chainHash;
    }
    return links;
  }
}
