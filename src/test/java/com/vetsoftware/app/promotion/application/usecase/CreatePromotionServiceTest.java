package com.vetsoftware.app.promotion.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.promotion.application.command.CreatePromotionCommand;
import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.application.port.out.PromotionTargetQueryPort;
import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.promotion.domain.Promotion;
import com.vetsoftware.app.promotion.domain.PromotionStatus;
import com.vetsoftware.app.promotion.domain.PromotionType;
import com.vetsoftware.app.promotion.domain.ValueType;
import com.vetsoftware.app.promotion.testsupport.PromotionMother;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePromotionService — creacion de una promocion")
class CreatePromotionServiceTest {

    @Mock
    private PromotionRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private PromotionTargetQueryPort promotionTargetQueryPort;

    private CreatePromotionService service;

    private static CreatePromotionCommand comandoValido() {
        return new CreatePromotionCommand("Enero perruno", PromotionType.DISCOUNT,
                ApplicationType.CATEGORY, PromotionMother.CATEGORY_ID, ValueType.PERCENTAGE,
                new BigDecimal("15.00"), PromotionMother.INICIO, PromotionMother.FIN,
                PromotionStatus.ACTIVE, PromotionMother.COMPANY_ID);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste la promocion creada con la empresa resuelta por el puerto")
        void persiste_la_promocion_creada() {
            service = new CreatePromotionService(repository, companyQueryPort,
                    promotionTargetQueryPort);
            when(companyQueryPort.findById(PromotionMother.COMPANY_ID))
                    .thenReturn(java.util.Optional.of(PromotionMother.CLINICA));
            when(promotionTargetQueryPort.exists(ApplicationType.CATEGORY,
                    PromotionMother.CATEGORY_ID, PromotionMother.COMPANY_ID)).thenReturn(true);
            when(repository.save(any())).thenReturn(PromotionMother.activa());

            PromotionDto dto = service.execute(comandoValido());

            ArgumentCaptor<Promotion> guardada = ArgumentCaptor.forClass(Promotion.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getName()).isEqualTo("Enero perruno");
            assertThat(guardada.getValue().getCompany()).isEqualTo(PromotionMother.CLINICA);
            assertThat(guardada.getValue().getId()).isNull();
            assertThat(dto.id()).isEqualTo(PromotionMother.PROMOTION_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca el repositorio si la empresa no existe")
        void no_toca_el_repositorio_si_la_empresa_no_existe() {
            service = new CreatePromotionService(repository, companyQueryPort,
                    promotionTargetQueryPort);
            when(companyQueryPort.findById(PromotionMother.COMPANY_ID))
                    .thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + PromotionMother.COMPANY_ID);

            verifyNoInteractions(promotionTargetQueryPort);
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("no toca el repositorio si el item de aplicacion no existe en la empresa")
        void no_toca_el_repositorio_si_el_application_item_no_existe() {
            service = new CreatePromotionService(repository, companyQueryPort,
                    promotionTargetQueryPort);
            when(companyQueryPort.findById(PromotionMother.COMPANY_ID))
                    .thenReturn(java.util.Optional.of(PromotionMother.CLINICA));
            when(promotionTargetQueryPort.exists(ApplicationType.CATEGORY,
                    PromotionMother.CATEGORY_ID, PromotionMother.COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "applicationItem not found: " + PromotionMother.CATEGORY_ID);

            verifyNoInteractions(repository);
        }
    }
}
