package com.vetsoftware.app.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyIdentifierChecker")
class JpaCompanyIdentifierCheckerTest {

    @Mock
    private CompanyJpaRepository jpaRepository;
    @InjectMocks
    private JpaCompanyIdentifierChecker checker;

    @Test
    @DisplayName("un NIT ya registrado existe")
    void un_nit_ya_registrado_existe() {
        when(jpaRepository.existsByIdentifier("900123456")).thenReturn(true);

        assertThat(checker.exists("900123456")).isTrue();
    }

    @Test
    @DisplayName("un NIT nuevo no existe")
    void un_nit_nuevo_no_existe() {
        when(jpaRepository.existsByIdentifier("900999999")).thenReturn(false);

        assertThat(checker.exists("900999999")).isFalse();
    }
}
