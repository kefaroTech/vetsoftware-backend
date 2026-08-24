package com.vetsoftware.app.pricelist.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.InvalidPriceListTransitionException;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArchivePriceListService")
class ArchivePriceListServiceTest {

    @Mock
    private PriceListRepository repository;

    @InjectMocks
    private ArchivePriceListService service;

    @Test
    @DisplayName("archiva una publicada conservando su firma")
    void archiva_una_publicada() {
        when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.publicada()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PriceListDto dto = service.execute(1L);

        assertThat(dto.status()).isEqualTo(PriceListStatus.ARCHIVED);
        assertThat(dto.publishedBySystemUserId()).isEqualTo(PriceListMother.FIRMANTE);
    }

    @ParameterizedTest
    @EnumSource(value = PriceListStatus.class, names = {"DRAFT", "ARCHIVED"})
    @DisplayName("solo se archiva lo que está publicado, y el rechazo no escribe")
    void solo_se_archiva_lo_publicado(PriceListStatus estado) {
        when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.enEstado(estado)));

        assertThatThrownBy(() -> service.execute(1L))
                .isInstanceOf(InvalidPriceListTransitionException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("una lista inexistente no se archiva")
    void lista_inexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(9L))
                .isInstanceOf(PriceListNotFoundException.class);
    }
}
