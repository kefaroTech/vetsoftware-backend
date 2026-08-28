package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenExternalInvoicingOutageService")
class OpenExternalInvoicingOutageServiceTest {

    // Distinto de STARTED_AT a proposito: distingue el reloj inyectado
    // (createdDate) de
    // la fecha observada que trae el comando (startedAt). Clock no es un puerto,
    // asi que
    // no se mockea: se inyecta fijo, como manda la seccion de determinismo del
    // CLAUDE.md.
    private static final Clock RELOJ_FIJO = Clock.fixed(
            ExternalInvoicingOutageMother.CREATED_DATE.atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    @Mock
    private ExternalInvoicingOutageRepository repository;

    @Captor
    private ArgumentCaptor<ExternalInvoicingOutage> caidaCaptor;

    private OpenExternalInvoicingOutageService service;

    @BeforeEach
    void setUp() {
        service = new OpenExternalInvoicingOutageService(repository, RELOJ_FIJO);
    }

    @Test
    @DisplayName("abre la caida con la fecha del comando y sella la creacion con el reloj inyectado")
    void abre_la_caida_con_la_fecha_del_comando_y_el_reloj_inyectado() {
        when(repository.save(any())).thenReturn(ExternalInvoicingOutageMother.abierta());

        ExternalInvoicingOutageDto dto = service
                .execute(ExternalInvoicingOutageMother.comandoAbrir());

        verify(repository).save(caidaCaptor.capture());
        ExternalInvoicingOutage guardada = caidaCaptor.getValue();
        assertThat(guardada.getId()).isNull();
        assertThat(guardada.getStartedAt()).isEqualTo(ExternalInvoicingOutageMother.STARTED_AT);
        assertThat(guardada.getCreatedDate()).isEqualTo(ExternalInvoicingOutageMother.CREATED_DATE);
        assertThat(guardada.isOpen()).isTrue();
        assertThat(guardada.getCauseParty()).isEqualTo(ExternalInvoicingOutageMother.CAUSE_PARTY);
        assertThat(dto.startedAt()).isEqualTo(ExternalInvoicingOutageMother.STARTED_AT);
    }
}
