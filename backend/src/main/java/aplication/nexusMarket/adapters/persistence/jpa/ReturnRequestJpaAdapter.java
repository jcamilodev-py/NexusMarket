package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.entities.ReturnRequestJpaEntity;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.ReturnJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataReturnRequestRepository;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.ReturnRequest;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ReturnRequestRepositoryPort;
import aplication.nexusMarket.domain.valueobjects.ReportPeriod;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Every request comes back with its order, so each returned line reaches the order line it returns. */
@Repository
@RequiredArgsConstructor
public class ReturnRequestJpaAdapter implements ReturnRequestRepositoryPort {

    private final SpringDataReturnRequestRepository returnRequestRepository;
    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public ReturnRequest save(ReturnRequest request) {
        request.setIdentifier(Identifiers.orNew(request.getIdentifier()));
        returnRequestRepository.save(ReturnJpaMapper.toEntity(request));
        return request;
    }

    @Override
    public Optional<ReturnRequest> findById(ReturnRequest request) {
        if (request == null || request.getIdentifier() == null) {
            return Optional.empty();
        }
        return returnRequestRepository.findById(request.getIdentifier()).map(this::toDomain);
    }

    @Override
    public List<ReturnRequest> findByOrder(Order order) {
        return returnRequestRepository.findByOrderId(order.getIdentifier()).stream()
                .map(entity -> ReturnJpaMapper.toDomain(entity, order))
                .toList();
    }

    @Override
    public List<ReturnRequest> findByBuyer(User buyer) {
        return returnRequestRepository.findByRequestedById(buyer.getUserId()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ReturnRequest> findAll() {
        return returnRequestRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<ReturnRequest> findByPeriod(ReportPeriod period) {
        return returnRequestRepository.findByRequestDateGreaterThanEqualAndRequestDateLessThan(
                        period.from().atStartOfDay(), period.to().plusDays(1).atStartOfDay()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void update(ReturnRequest request) {
        returnRequestRepository.save(ReturnJpaMapper.toEntity(request));
    }

    private ReturnRequest toDomain(ReturnRequestJpaEntity entity) {
        Order orderReference = new Order();
        orderReference.setIdentifier(entity.getOrderId());
        Order order = orderRepositoryPort.findById(orderReference)
                .orElseThrow(() -> new IllegalStateException("Return request without order: " + entity.getReturnRequestId()));
        return ReturnJpaMapper.toDomain(entity, order);
    }
}
