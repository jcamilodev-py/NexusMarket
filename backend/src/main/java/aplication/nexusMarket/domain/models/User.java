package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidStatusTransitionException;
import aplication.nexusMarket.domain.exceptions.InvalidUserException;
import aplication.nexusMarket.domain.exceptions.InvalidUserStatusException;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import aplication.nexusMarket.domain.valueobjects.UserStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents any participant authorized to interact with the NexusMarket platform.
 *
 * <p>Centralizes the identification, contact and access information shared by every role: Buyer,
 * Seller, LogisticsOperator, Administrator and Supervisor. Each participant holds exactly one role
 * (RG-02), and that role determines which specialization applies.
 *
 * <p>{@code userId} and {@code identificationNumber} are separate on purpose: Seccion 11 requires
 * both the identity document and the email to be unique, and one field cannot carry both concerns.
 *
 * <p>Source: DOMINIO 1; Seccion 5; Seccion 11; RG-01, RG-02, RG-03.
 */
@Getter
@Setter
public abstract class User {

    /** Internal unique identifier of the user within the platform. */
    private String userId;

    /** National identity document number. Unique across the platform (Seccion 11). */
    private String identificationNumber;

    private String fullName;

    /** Primary means of access and communication. Unique across the platform (Seccion 11). */
    private String email;

    /**
     * One-way hash verified at login; the plain password is never stored. Inferred from DOMINIO 1
     * ("base de autenticacion") and RG-01.
     */
    private String passwordHash;

    private SystemRole role;

    private UserStatus status;

    /** Only an ACTIVE user may authenticate or operate (RG-01). */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(status);
    }

    public boolean hasRole(SystemRole expectedRole) {
        return expectedRole != null && expectedRole.equals(role);
    }

    /**
     * Rejects a change to the status already held: an operation that changes nothing must not leave
     * a trace in the audit log. The specification defines no other transition restriction.
     */
    public void changeStatus(UserStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidUserStatusException("Target user status must be provided.");
        }
        if (newStatus.equals(status)) {
            throw new InvalidStatusTransitionException(
                    "User already has status " + newStatus.getCode() + ".");
        }
        this.status = newStatus;
    }

    /** Identity data every registration requires (DOMINIO 1 "No vacio"; Seccion 11). */
    public void validateIdentity() {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidUserException("Full name must not be blank.");
        }
        if (identificationNumber == null || identificationNumber.isBlank()) {
            throw new InvalidUserException("Identification number must not be blank.");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidUserException("Email must not be blank.");
        }
    }

    public void assignPasswordHash(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new InvalidUserException("Password hash must be provided.");
        }
        this.passwordHash = newPasswordHash;
    }
}
