package com.vetsoftware.app.registration.application.command;

public record RegisterUserCommand(String companyName, String documentType, String companyIdentifier,
        String companyAddress, String companyContactNumber, Long cityId, String employeeName,
        String employeeEmail, String rawPassword, String taxRegime, String fiscalEmail,
        String recaptchaToken, String remoteIp, String aiProposalToken) {

    /**
     * Alta sin propuesta del asistente detras: el registro directo desde la
     * portada, y todo lo anterior a DC-2. Constructor secundario y no valor por
     * defecto, para que la atribucion de embudo no obligara a reescribir los sitios
     * que ya construian este command.
     */
    public RegisterUserCommand(String companyName, String documentType, String companyIdentifier,
            String companyAddress, String companyContactNumber, Long cityId, String employeeName,
            String employeeEmail, String rawPassword, String taxRegime, String fiscalEmail,
            String recaptchaToken, String remoteIp) {
        this(companyName, documentType, companyIdentifier, companyAddress, companyContactNumber,
                cityId, employeeName, employeeEmail, rawPassword, taxRegime, fiscalEmail,
                recaptchaToken, remoteIp, null);
    }
}
