package com.vetsoftware.app.animal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.domain.OwnerRef;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaOwnerQueryPort — adaptador sobre OwnerJpaRepository")
class JpaOwnerQueryPortTest {

    @Mock
    private OwnerJpaRepository ownerJpaRepository;
    @Mock
    private OwnerJpaEntity ownerEntity;
    @InjectMocks
    private JpaOwnerQueryPort port;

    @Nested
    @DisplayName("busqueda por empresa")
    class Busqueda {

        @Test
        @DisplayName("mapea el propietario encontrado a su companion VO")
        void mapea_el_propietario_encontrado_a_su_companion_vo() {
            when(ownerEntity.getId()).thenReturn(AnimalMother.DUENO.id());
            when(ownerEntity.getName()).thenReturn(AnimalMother.DUENO.name());
            when(ownerEntity.getDocument()).thenReturn(AnimalMother.DUENO.document());
            when(ownerJpaRepository.findByIdAndCompanyId(AnimalMother.DUENO.id(),
                    AnimalMother.COMPANY_ID)).thenReturn(Optional.of(ownerEntity));

            Optional<OwnerRef> resultado = port.findByIdAndCompanyId(AnimalMother.DUENO.id(),
                    AnimalMother.COMPANY_ID);

            assertThat(resultado).contains(AnimalMother.DUENO);
        }

        @Test
        @DisplayName("un propietario de otra empresa no se entrega")
        void un_propietario_de_otra_empresa_no_se_entrega() {
            when(ownerJpaRepository.findByIdAndCompanyId(AnimalMother.DUENO.id(),
                    AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

            Optional<OwnerRef> resultado = port.findByIdAndCompanyId(AnimalMother.DUENO.id(),
                    AnimalMother.COMPANY_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
