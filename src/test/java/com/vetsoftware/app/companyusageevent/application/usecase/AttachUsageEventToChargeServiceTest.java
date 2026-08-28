package com.vetsoftware.app.companyusageevent.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.companyusageevent.application.port.out.CompanyUsageEventRepository;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEventNotFoundException;
import com.vetsoftware.app.companyusageevent.domain.UsageEventAlreadyChargedException;
import com.vetsoftware.app.companyusageevent.testsupport.CompanyUsageEventMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachUsageEventToChargeService")
class AttachUsageEventToChargeServiceTest {

    @Mock
    private CompanyUsageEventRepository repository;

    private AttachUsageEventToChargeService service;

    @BeforeEach
    void setUp() {
        service = new AttachUsageEventToChargeService(repository);
    }

    @Nested
    @DisplayName("colgar el cargo")
    class ColgarCargo {

        @Test
        @DisplayName("cuelga el hecho del cargo, cargando por id acotado por empresa")
        void cuelga_el_hecho_del_cargo_acotado_por_empresa() {
            when(repository.findByIdAndCompanyId(CompanyUsageEventMother.EVENT_ID,
                    CompanyUsageEventMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyUsageEventMother.hechoSinCargo()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CompanyUsageEventDto dto = service
                    .execute(CompanyUsageEventMother.comandoColgarCargo());

            ArgumentCaptor<CompanyUsageEvent> captor = ArgumentCaptor
                    .forClass(CompanyUsageEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getChargeId())
                    .isEqualTo(CompanyUsageEventMother.CHARGE_ID);
            assertThat(dto.chargeId()).isEqualTo(CompanyUsageEventMother.CHARGE_ID);
        }

        @Test
        @DisplayName("un hecho de otra empresa no se encuentra")
        void un_hecho_de_otra_empresa_no_se_encuentra() {
            when(repository.findByIdAndCompanyId(CompanyUsageEventMother.EVENT_ID,
                    CompanyUsageEventMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyUsageEventMother.comandoColgarCargo()))
                    .isInstanceOf(CompanyUsageEventNotFoundException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un hecho ya cobrado no se puede recolgar")
        void un_hecho_ya_cobrado_no_se_puede_recolgar() {
            when(repository.findByIdAndCompanyId(CompanyUsageEventMother.EVENT_ID,
                    CompanyUsageEventMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyUsageEventMother.hechoConCargo()));

            assertThatThrownBy(() -> service.execute(CompanyUsageEventMother.comandoColgarCargo()))
                    .isInstanceOf(UsageEventAlreadyChargedException.class)
                    .hasMessageContaining("is already attached to charge");

            verify(repository, never()).save(any());
        }
    }
}
