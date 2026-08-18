package com.vetsoftware.app.promotion.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.promotion.application.command.UpdatePromotionCommand;
import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.application.port.out.PromotionTargetQueryPort;
import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.promotion.domain.Promotion;
import com.vetsoftware.app.promotion.domain.PromotionNotFoundException;
import com.vetsoftware.app.promotion.domain.PromotionStatus;
import com.vetsoftware.app.promotion.domain.PromotionType;
import com.vetsoftware.app.promotion.domain.ValueType;
import com.vetsoftware.app.promotion.testsupport.PromotionMother;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePromotionService — actualizacion de una promocion existente")
class UpdatePromotionServiceTest {

    @Mock
    private PromotionRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private PromotionTargetQueryPort promotionTargetQueryPort;

    private UpdatePromotionService service;

    private static UpdatePromotionCommand comandoValido() {
        return new UpdatePromotionCommand(PromotionMother.PROMOTION_ID, "Febrero felino",
                PromotionType.SPECIAL_PRICE, ApplicationType.PRODUCT, 7L, ValueType.VALUE,
                new BigDecimal("8000.00"), PromotionMother.INICIO, PromotionMother.FIN,
                PromotionStatus.INACTIVE, PromotionMother.COMPANY_ID);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("aplica los cambios sobre la promocion encontrada y la persiste")
        void aplica_los_cambios_y_persiste() {
            service = new UpdatePromotionService(repository, companyQueryPort,
                    promotionTargetQueryPort);
            Promotion existente = PromotionMother.activa();
            when(repository.findByIdAndCompanyId(PromotionMother.PROMOTION_ID,
                    PromotionMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(companyQueryPort.findById(PromotionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PromotionMother.CLINICA));
            when(promotionTargetQueryPort.exists(ApplicationType.PRODUCT, 7L,
                    PromotionMother.COMPANY_ID)).thenReturn(true);
            when(repository.save(existente)).thenReturn(existente);

            PromotionDto dto = service.execute(comandoValido());

            assertThat(existente.getName()).isEqualTo("Febrero felino");
            assertThat(existente.getApplicationType()).isEqualTo(ApplicationType.PRODUCT);
            assertThat(existente.getApplicationItem()).isEqualTo(7L);
            assertThat(dto.name()).isEqualTo("Febrero felino");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("una promocion inexistente no toca la empresa ni el target")
        void promocion_inexistente_no_toca_nada_mas() {
            service = new UpdatePromotionService(repository, companyQueryPort,
                    promotionTargetQueryPort);
            when(repository.findByIdAndCompanyId(PromotionMother.PROMOTION_ID,
                    PromotionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(PromotionNotFoundException.class)
                    .hasMessageContaining("Promotion not found: " + PromotionMother.PROMOTION_ID);

            verifyNoInteractions(companyQueryPort);
            verifyNoInteractions(promotionTargetQueryPort);
            verify(repository, org.mockito.Mockito.never())
                    .save(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("la promocion de OTRA empresa es 404 y no se apropia")
        void promocion_de_otra_empresa_es_not_found_y_no_escribe() {
            // El caso que @authz.isMyCompany NO cubre: el atacante declara SU empresa
            // (el gate pasa) y apunta al id de la promocion ajena. Con la lectura acotada
            // no hay fila que cargar, asi que no hay update que le reescriba la company.
            service = new UpdatePromotionService(repository, companyQueryPort,
                    promotionTargetQueryPort);
            when(repository.findByIdAndCompanyId(PromotionMother.PROMOTION_ID,
                    PromotionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(PromotionNotFoundException.class);

            verifyNoInteractions(companyQueryPort);
            verifyNoInteractions(promotionTargetQueryPort);
            verify(repository, org.mockito.Mockito.never())
                    .save(org.mockito.ArgumentMatchers.any());
            verify(repository, org.mockito.Mockito.never())
                    .findById(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("no guarda si la empresa no existe")
        void no_guarda_si_la_empresa_no_existe() {
            service = new UpdatePromotionService(repository, companyQueryPort,
                    promotionTargetQueryPort);
            when(repository.findByIdAndCompanyId(PromotionMother.PROMOTION_ID,
                    PromotionMother.COMPANY_ID)).thenReturn(Optional.of(PromotionMother.activa()));
            when(companyQueryPort.findById(PromotionMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + PromotionMother.COMPANY_ID);

            verifyNoInteractions(promotionTargetQueryPort);
            verify(repository, org.mockito.Mockito.never())
                    .save(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("no guarda si el item de aplicacion no existe en la empresa")
        void no_guarda_si_el_application_item_no_existe() {
            service = new UpdatePromotionService(repository, companyQueryPort,
                    promotionTargetQueryPort);
            when(repository.findByIdAndCompanyId(PromotionMother.PROMOTION_ID,
                    PromotionMother.COMPANY_ID)).thenReturn(Optional.of(PromotionMother.activa()));
            when(companyQueryPort.findById(PromotionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PromotionMother.CLINICA));
            when(promotionTargetQueryPort.exists(ApplicationType.PRODUCT, 7L,
                    PromotionMother.COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("applicationItem not found: 7");

            verify(repository, org.mockito.Mockito.never())
                    .save(org.mockito.ArgumentMatchers.any());
        }
    }
}
