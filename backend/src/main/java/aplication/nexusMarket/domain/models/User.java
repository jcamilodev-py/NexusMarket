package aplication.nexusMarket.domain.models;

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

    private SystemRole role;

    private UserStatus status;
}
