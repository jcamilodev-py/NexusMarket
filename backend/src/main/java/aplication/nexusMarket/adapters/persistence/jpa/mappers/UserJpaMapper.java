package aplication.nexusMarket.adapters.persistence.jpa.mappers;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.jpa.entities.UserJpaEntity;
import aplication.nexusMarket.domain.models.Administrator;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.Cart;
import aplication.nexusMarket.domain.models.LogisticsOperator;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.Supervisor;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.valueobjects.BuyerCommercialStatus;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import aplication.nexusMarket.domain.valueobjects.UserStatus;
import java.util.ArrayList;

/**
 * Rebuilds the specialization named by the stored role. A buyer's active cart comes back as a
 * reference (identifier and owner); the cart adapter loads its lines when they are needed.
 */
public final class UserJpaMapper {

    private UserJpaMapper() {
    }

    public static UserJpaEntity toEntity(User domain) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setUserId(domain.getUserId());
        entity.setIdentificationNumber(domain.getIdentificationNumber());
        entity.setFullName(domain.getFullName());
        entity.setEmail(domain.getEmail());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRole(CatalogCodes.code(domain.getRole()));
        entity.setStatus(CatalogCodes.code(domain.getStatus()));
        if (domain instanceof Buyer buyer) {
            entity.setPrimaryAddress(buyer.getPrimaryAddress());
            entity.setAdditionalAddresses(new ArrayList<>(buyer.getAdditionalAddresses()));
            entity.setCommercialStatus(CatalogCodes.code(buyer.getCommercialStatus()));
            entity.setActiveCartId(buyer.getActiveCart() == null ? null : buyer.getActiveCart().getIdentifier());
        }
        if (domain instanceof Seller seller) {
            entity.setLegalBusinessName(seller.getLegalBusinessName());
            entity.setTaxId(seller.getTaxId());
            entity.setTradeName(seller.getTradeName());
        }
        return entity;
    }

    public static User toDomain(UserJpaEntity entity) {
        SystemRole role = CatalogCodes.fromCode(SystemRole.class, entity.getRole());
        User domain = switch (role) {
            case BUYER -> buyer(entity);
            case SELLER -> seller(entity);
            case LOGISTICS_OPERATOR -> new LogisticsOperator();
            case ADMINISTRATOR -> new Administrator();
            case SUPERVISOR -> new Supervisor();
        };
        domain.setUserId(entity.getUserId());
        domain.setIdentificationNumber(entity.getIdentificationNumber());
        domain.setFullName(entity.getFullName());
        domain.setEmail(entity.getEmail());
        domain.setPasswordHash(entity.getPasswordHash());
        domain.setRole(role);
        domain.setStatus(CatalogCodes.fromCode(UserStatus.class, entity.getStatus()));
        return domain;
    }

    private static Buyer buyer(UserJpaEntity entity) {
        Buyer buyer = new Buyer();
        buyer.setPrimaryAddress(entity.getPrimaryAddress());
        buyer.setAdditionalAddresses(new ArrayList<>(entity.getAdditionalAddresses()));
        buyer.setCommercialStatus(CatalogCodes.fromCode(BuyerCommercialStatus.class, entity.getCommercialStatus()));
        if (entity.getActiveCartId() != null) {
            Cart activeCart = new Cart();
            activeCart.setIdentifier(entity.getActiveCartId());
            activeCart.setBuyer(buyer);
            buyer.setActiveCart(activeCart);
        }
        return buyer;
    }

    private static Seller seller(UserJpaEntity entity) {
        Seller seller = new Seller();
        seller.setLegalBusinessName(entity.getLegalBusinessName());
        seller.setTaxId(entity.getTaxId());
        seller.setTradeName(entity.getTradeName());
        return seller;
    }
}
