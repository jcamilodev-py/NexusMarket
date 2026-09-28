package aplication.nexusMarket.domain.services.catalog;

import aplication.nexusMarket.domain.models.Product;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Lists the public catalog: published products only, for any authenticated user (Seccion 6.1 step 4). */
@Service
@RequiredArgsConstructor
public class ConsultCatalogService {

    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;

    public List<Product> consultCatalog(User requestingUser) {
        validateUserStatusService.execute(requestingUser);
        return productRepositoryPort.findPublished();
    }
}
