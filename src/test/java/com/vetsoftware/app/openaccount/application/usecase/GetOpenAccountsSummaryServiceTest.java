package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountsSummaryDto;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetOpenAccountsSummaryService")
class GetOpenAccountsSummaryServiceTest {

    @Mock
    private OpenAccountRepository repository;
    @InjectMocks
    private GetOpenAccountsSummaryService service;

    @Test
    @DisplayName("delega el resumen en el repositorio, scoped a empresa y sede")
    void delega_el_resumen_en_el_repositorio() {
        OpenAccountsSummaryDto resumen = new OpenAccountsSummaryDto(3L, 5L,
                new BigDecimal("40000.00"));
        when(repository.summarize(OpenAccountMother.COMPANY_ID, OpenAccountMother.BRANCH_ID))
                .thenReturn(resumen);

        OpenAccountsSummaryDto obtenido = service.summarize(OpenAccountMother.COMPANY_ID,
                OpenAccountMother.BRANCH_ID);

        assertThat(obtenido).isEqualTo(resumen);
    }
}
