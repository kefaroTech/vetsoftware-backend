package com.vetsoftware.app.cashregister.application.usecase;

import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.COMPANY_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.SESSION_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionCerrada;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionConMovimientos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportArqueoService")
class ExportArqueoServiceTest {

    @Mock
    private CashSessionRepository repository;

    @InjectMocks
    private ExportArqueoService service;

    @Test
    @DisplayName("construye el arqueo de la sesion cerrada con sus totales")
    void construye_el_arqueo_de_la_sesion_cerrada() {
        when(repository.findByIdAndCompany(SESSION_ID, COMPANY_ID))
                .thenReturn(Optional.of(sesionCerrada()));

        CashArqueoReport reporte = service.arqueo(COMPANY_ID, SESSION_ID);

        assertThat(reporte.sessionId()).isEqualTo(SESSION_ID);
        assertThat(reporte.status()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(reporte.totalExpected()).isEqualByComparingTo("160000");
        assertThat(reporte.totalDifference()).isEqualByComparingTo("-5000");
    }

    @Test
    @DisplayName("una sesion todavia abierta tambien se puede arquear, sin lo contado")
    void una_sesion_abierta_tambien_se_puede_arquear() {
        when(repository.findByIdAndCompany(SESSION_ID, COMPANY_ID))
                .thenReturn(Optional.of(sesionConMovimientos()));

        CashArqueoReport reporte = service.arqueo(COMPANY_ID, SESSION_ID);

        // Es el arqueo de control a mitad de turno: enseña lo que deberia haber en
        // caja, y deja el contado en null porque nadie ha contado todavia.
        assertThat(reporte.totalExpected()).isEqualByComparingTo("160000");
        assertThat(reporte.totalCounted()).isNull();
    }

    @Test
    @DisplayName("una sesion de otra empresa se ve como inexistente")
    void una_sesion_de_otra_empresa_se_ve_como_inexistente() {
        when(repository.findByIdAndCompany(SESSION_ID, OTRA_COMPANY_ID))
                .thenReturn(Optional.empty());

        // El arqueo enseña el dinero de una sede: filtrarlo por empresa no es opcional.
        assertThatThrownBy(() -> service.arqueo(OTRA_COMPANY_ID, SESSION_ID))
                .isInstanceOf(CashSessionNotFoundException.class)
                .hasMessageContaining(String.valueOf(SESSION_ID));
    }
}
