package com.vetsoftware.app.paymentgateway.domain;

/**
 * El medio de pago de otra feature ({@code subscriptionpaymentmethod}) visto
 * desde aquí: solo lo justo para cobrar con él, nunca la marca ni los cuatro
 * últimos dígitos.
 */
public record PaymentMethodRef(Long id, String token) {
}
