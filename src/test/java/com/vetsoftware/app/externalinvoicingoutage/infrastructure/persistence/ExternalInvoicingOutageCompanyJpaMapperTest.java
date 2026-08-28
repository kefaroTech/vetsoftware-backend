package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExternalInvoicingOutageCompanyJpaMapper")
class ExternalInvoicingOutageCompanyJpaMapperTest {

    private final ExternalInvoicingOutageCompanyJpaMapper mapper = new ExternalInvoicingOutageCompanyJpaMapper();

    /**
     * Se construye con el constructor protegido (visible en este mismo paquete) en
     * vez de mockearla: no tiene logica y un mock aqui no vale mas que el objeto
     * real.
     */
    private ExternalInvoicingOutageJpaEntity outageEntity() {
        ExternalInvoicingOutageJpaEntity entity = new ExternalInvoicingOutageJpaEntity();
        entity.setId(ExternalInvoicingOutageMother.OUTAGE_ID);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar y engancha la caida recibida")
        void copia_cada_campo_y_engancha_la_caida() {
            ExternalInvoicingOutageCompany afectada = ExternalInvoicingOutageMother.afectada();
            ExternalInvoicingOutageJpaEntity outage = outageEntity();

            ExternalInvoicingOutageCompanyJpaEntity entity = mapper.toJpa(afectada, outage);

            assertThat(entity.getId()).isEqualTo(ExternalInvoicingOutageMother.AFFECTED_ID);
            assertThat(entity.getOutage()).isSameAs(outage);
            assertThat(entity.getCompanyId()).isEqualTo(ExternalInvoicingOutageMother.COMPANY_ID);
            assertThat(entity.getFailedDocumentCount())
                    .isEqualTo(ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT);
            assertThat(entity.getResolvedBy()).isEqualTo(ExternalInvoicingOutageMother.RESOLVED_BY);
        }
    }

    @Nested
    @DisplayName("toDomain — entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("lee el id de la caida desde la asociacion ya hidratada, sin navegar mas adentro")
        void lee_el_id_de_la_caida_desde_la_asociacion() {
            ExternalInvoicingOutageJpaEntity outage = outageEntity();
            ExternalInvoicingOutageCompanyJpaEntity entity = mapper
                    .toJpa(ExternalInvoicingOutageMother.afectada(), outage);

            ExternalInvoicingOutageCompany afectada = mapper.toDomain(entity);

            assertThat(afectada.getId()).isEqualTo(ExternalInvoicingOutageMother.AFFECTED_ID);
            assertThat(afectada.getOutageId()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
            assertThat(afectada.getCompanyId()).isEqualTo(ExternalInvoicingOutageMother.COMPANY_ID);
            assertThat(afectada.getFailedDocumentCount())
                    .isEqualTo(ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT);
            assertThat(afectada.getResolvedBy())
                    .isEqualTo(ExternalInvoicingOutageMother.RESOLVED_BY);
        }
    }
}
