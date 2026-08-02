package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.time.Duration;

/**
 * Telemetría de resultados y duración del ciclo de comunicación con la DIAN.
 */
public interface BillingMetrics {

    void finished(DianStatus status, Origin origin, ElectronicDocumentType documentType,
            Duration duration);

    void failed(Origin origin, ElectronicDocumentType documentType, Duration duration);

    enum Origin {
        INITIAL("initial"), RETRY("retry"), WEBHOOK("webhook"), RECONCILIATION("reconciliation");

        private final String value;

        Origin(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
