package com.vetsoftware.app.electronicdocument.infrastructure.contingency;

import com.vetsoftware.app.electronicdocument.application.port.out.ContingencyMonitorPort;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Monitor de contingencia EN MEMORIA por empresa (esqueleto). Detecta la caída sostenida del
 * proveedor/DIAN contando fallos de infraestructura (5xx/timeout) consecutivos: al llegar al umbral
 * activa el modo; al primer resultado sano lo desactiva. La recuperación se detecta sola porque el
 * job de contingencia reintenta los documentos CONTINGENCIA → vuelve a llamar a transmit → registra
 * el resultado aquí.
 *
 * <p>En memoria a propósito (skeleton): el estado se reconstruye solo a partir de los intentos en
 * vivo. Si en el futuro se requiere visibilidad multi-instancia / persistente, este adapter se
 * reemplaza por uno con tabla sin tocar el puerto ni los llamadores.
 */
@Component
public class InMemoryContingencyMonitor implements ContingencyMonitorPort {

  private static final Logger log = LoggerFactory.getLogger(InMemoryContingencyMonitor.class);

  /** Fallos de infraestructura consecutivos para entrar en modo contingencia. */
  private final int failureThreshold;

  private final ConcurrentHashMap<Long, State> byCompany = new ConcurrentHashMap<>();

  public InMemoryContingencyMonitor(
      @Value("${dian.contingency.failure-threshold:3}") int failureThreshold) {
    this.failureThreshold = failureThreshold < 1 ? 1 : failureThreshold;
  }

  @Override
  public void recordOutcome(Long companyId, boolean providerResponded) {
    if (companyId == null) return;
    byCompany.compute(
        companyId,
        (id, prev) -> {
          State s = prev == null ? new State() : prev;
          if (providerResponded) {
            if (s.active) {
              log.info(
                  "Empresa {}: proveedor DIAN recuperado → modo contingencia DESACTIVADO "
                      + "(activo desde {}).",
                  id,
                  s.since);
            }
            s.consecutiveFailures = 0;
            s.active = false;
            s.since = null;
          } else {
            s.consecutiveFailures++;
            if (!s.active && s.consecutiveFailures >= failureThreshold) {
              s.active = true;
              s.since = LocalDateTime.now();
              log.warn(
                  "Empresa {}: {} fallos de transmisión consecutivos → modo contingencia ACTIVADO."
                      + " Emitir en contingencia (tipo 04) / comprobante provisional y regularizar"
                      + " al volver.",
                  id,
                  s.consecutiveFailures);
            }
          }
          return s;
        });
  }

  @Override
  public boolean isActive(Long companyId) {
    if (companyId == null) return false;
    State s = byCompany.get(companyId);
    return s != null && s.active;
  }

  private static final class State {
    int consecutiveFailures;
    boolean active;
    LocalDateTime since;
  }
}
