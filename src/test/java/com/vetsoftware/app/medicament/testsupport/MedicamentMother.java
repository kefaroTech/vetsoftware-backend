package com.vetsoftware.app.medicament.testsupport;

import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;

public final class MedicamentMother {

    private MedicamentMother() {
    }

    public static CompanyRef companyRef() {
        return new CompanyRef(9L, "Clinica Norte", "900123456");
    }

    public static Medicament general() {
        return Medicament.create("Amoxicilina", "Antibiotico de amplio espectro", null, true);
    }

    public static Medicament propioDeEmpresa(CompanyRef company) {
        return Medicament.create("Suero especial", "Formula propia", company, false);
    }
}
