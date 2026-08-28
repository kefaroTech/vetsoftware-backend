package com.vetsoftware.app.accountmapping.testsupport;

import com.vetsoftware.app.accountmapping.application.command.CloseAccountMappingCommand;
import com.vetsoftware.app.accountmapping.application.command.CreateAccountMappingCommand;
import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo accountmapping.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code AccountMapping.create(...)}: eso deja al test decidir el id y la
 * version, que es lo que necesita un caso que cierra un mapeo ya publicado.
 */
public final class AccountMappingMother {

    public static final Long MAPPING_ID = 700L;
    public static final String MAPPING_KEY = "001";
    public static final String DEBIT_CODE = "110501";
    public static final String CREDIT_CODE = "220501";
    public static final String DEFERRED_CODE = "240501";
    public static final Long CATALOG_ITEM_ID = 55L;
    public static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 1, 9, 0);

    private AccountMappingMother() {
    }

    /** Mapeo BANK abierto: clase no refinable, sin articulo ni afinados. */
    public static AccountMapping mapeoBancoAbierto() {
        return mapeoBancoAbierto(MAPPING_ID);
    }

    public static AccountMapping mapeoBancoAbierto(Long id) {
        return new AccountMapping(id, MappingKind.BANK, MAPPING_KEY, null, null, null, DEBIT_CODE,
                CREDIT_CODE, null, VALID_FROM, null, CREADO, true, 2L);
    }

    public static AccountMapping mapeoBancoCerrado(LocalDate cierre) {
        return new AccountMapping(MAPPING_ID, MappingKind.BANK, MAPPING_KEY, null, null, null,
                DEBIT_CODE, CREDIT_CODE, null, VALID_FROM, cierre, CREADO, true, 2L);
    }

    /**
     * Mapeo REVENUE abierto: junto a DEFERRED_REVENUE, la unica clase que lleva
     * articulo, cargo, tratamiento fiscal y cuenta diferida.
     */
    public static AccountMapping mapeoIngresoAbierto() {
        return new AccountMapping(MAPPING_ID, MappingKind.REVENUE, MAPPING_KEY, CATALOG_ITEM_ID,
                "CONSULTA", "GRAVADO", DEBIT_CODE, CREDIT_CODE, DEFERRED_CODE, VALID_FROM, null,
                CREADO, true, 2L);
    }

    public static CreateAccountMappingCommand comandoCrearBanco() {
        return new CreateAccountMappingCommand(MappingKind.BANK, MAPPING_KEY, null, null, null,
                DEBIT_CODE, CREDIT_CODE, null, VALID_FROM, null);
    }

    public static CreateAccountMappingCommand comandoCrearIngreso() {
        return new CreateAccountMappingCommand(MappingKind.REVENUE, MAPPING_KEY, CATALOG_ITEM_ID,
                "CONSULTA", "GRAVADO", DEBIT_CODE, CREDIT_CODE, DEFERRED_CODE, VALID_FROM, null);
    }

    public static CloseAccountMappingCommand comandoCerrar(Long id, LocalDate validTo) {
        return new CloseAccountMappingCommand(id, validTo);
    }
}
