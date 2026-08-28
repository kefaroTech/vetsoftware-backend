package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapper NO toca {@code open_outage_marker}: la calcula MySQL y no esta
 * mapeada en {@link ExternalInvoicingOutageJpaEntity}, asi que no hay nada de
 * esa columna que este test pueda ejercitar.
 */
@DisplayName("ExternalInvoicingOutageJpaMapper")
class ExternalInvoicingOutageJpaMapperTest {

    private final ExternalInvoicingOutageJpaMapper mapper = new ExternalInvoicingOutageJpaMapper();

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            ExternalInvoicingOutage caida = ExternalInvoicingOutageMother.cerrada();

            ExternalInvoicingOutageJpaEntity entity = mapper.toJpa(caida);

            assertThat(entity.getId()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
            assertThat(entity.getStartedAt()).isEqualTo(ExternalInvoicingOutageMother.STARTED_AT);
            assertThat(entity.getEndedAt()).isEqualTo(ExternalInvoicingOutageMother.ENDED_AT);
            assertThat(entity.getCauseParty()).isEqualTo(ExternalInvoicingOutageMother.CAUSE_PARTY);
            assertThat(entity.getSummary()).isEqualTo(ExternalInvoicingOutageMother.SUMMARY);
            assertThat(entity.getAffectedCompanyCount())
                    .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANY_COUNT);
            assertThat(entity.getNotifiedCompaniesAt())
                    .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT);
            assertThat(entity.getExternalIncidentRef())
                    .isEqualTo(ExternalInvoicingOutageMother.EXTERNAL_INCIDENT_REF);
            assertThat(entity.getCreatedDate())
                    .isEqualTo(ExternalInvoicingOutageMother.CREATED_DATE);
        }

        @Test
        @DisplayName("la version viaja hacia la entidad: sin ella el merge se vuelve un insert")
        void la_version_viaja_hacia_la_entidad() {
            ExternalInvoicingOutage caida = ExternalInvoicingOutageMother.abierta();

            ExternalInvoicingOutageJpaEntity entity = mapper.toJpa(caida);

            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getVersion()).isEqualTo(ExternalInvoicingOutageMother.VERSION);
        }
    }

    @Nested
    @DisplayName("toDomain — entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("reconstruye el agregado con cada campo en su sitio")
        void reconstruye_el_agregado_con_cada_campo_en_su_sitio() {
            ExternalInvoicingOutageJpaEntity entity = mapper
                    .toJpa(ExternalInvoicingOutageMother.cerrada());

            ExternalInvoicingOutage caida = mapper.toDomain(entity);

            assertThat(caida.getId()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
            assertThat(caida.getEndedAt()).isEqualTo(ExternalInvoicingOutageMother.ENDED_AT);
            assertThat(caida.getExternalIncidentRef())
                    .isEqualTo(ExternalInvoicingOutageMother.EXTERNAL_INCIDENT_REF);
            assertThat(caida.isOpen()).isFalse();
        }

        @Test
        @DisplayName("la version vuelve tambien desde la entidad")
        void la_version_vuelve_desde_la_entidad() {
            ExternalInvoicingOutageJpaEntity entity = mapper
                    .toJpa(ExternalInvoicingOutageMother.abierta());

            ExternalInvoicingOutage caida = mapper.toDomain(entity);

            assertThat(caida.getVersion()).isEqualTo(ExternalInvoicingOutageMother.VERSION);
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            ExternalInvoicingOutage original = ExternalInvoicingOutageMother.cerrada();

            ExternalInvoicingOutage vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }
}
