package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.math.BigDecimal;

/**
 * Telemetría de hechos comerciales de una venta. No es una fuente contable: su
 * objetivo es detectar cambios operativos y alimentar dashboards.
 */
public interface SalesMetrics {

    void completed(Channel channel, ElectronicDocumentType documentType, BigDecimal amount,
            int lineCount);

    void failed(Channel channel, ElectronicDocumentType documentType, Result result);

    enum Channel {
        POS("pos"), OPEN_ACCOUNT("open_account");

        private final String value;

        Channel(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    enum Result {
        REJECTED("rejected"), CANCELLED("cancelled"), ERROR("error");

        private final String value;

        Result(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
