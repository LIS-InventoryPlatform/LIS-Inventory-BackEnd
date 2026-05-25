package com.lis.inventory.shared.exception;

/**
 * Se lanza cuando LIS-InventoryPlatform no puede comunicarse con el
 * servicio de autenticación externo (Auth0 / LIS-Authentication-Service)
 * por indisponibilidad, timeout, error de red o respuesta inválida.
 *
 * <p>El mensaje expuesto al usuario nunca debe contener detalles técnicos.</p>
 */
public class AuthServiceUnavailableException extends RuntimeException {

    public AuthServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
