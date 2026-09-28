package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.entities.RefundJpaEntity;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.ReturnJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataRefundRepository;
import aplication.nexusMarket.domain.models.Administrator;
import aplication.nexusMarket.domain.models.Refund;
import aplication.nexusMarket.domain.models.ReturnRequest;
import aplication.nexusMarket.domain.ports.out.RefundRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ReturnRequestRepositoryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefundJpaAdapter implements RefundRepositoryPort {

    private final SpringDataRefundRepository refundRepository;
    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final JpaReferenceResolver referenceResolver;

    @Override
    public Refund save(Refund refund) {
        refund.setIdentifier(Identifiers.orNew(refund.getIdentifier()));
        refundRepository.save(ReturnJpaMapper.toEntity(refund));
        return refund;
    }

    @Override
    public Optional<Refund> findById(Refund refund) {
        if (refund == null || refund.getIdentifier() == null) {
            return Optional.empty();
        }
        return refundRepository.findById(refund.getIdentifier()).map(entity -> {
            ReturnRequest requestReference = new ReturnRequest();
            requestReference.setIdentifier(entity.getReturnRequestId());
            return toDomain(entity, returnRequestRepositoryPort.findById(requestReference).orElse(null));
        });
    }

    @Override
    public Optional<Refund> findByReturnRequest(ReturnRequest request) {
        return refundRepository.findByReturnRequestId(request.getIdentifier()).map(entity -> toDomain(entity, request));
    }

    @Override
    public void update(Refund refund) {
        refundRepository.save(ReturnJpaMapper.toEntity(refund));
    }

    private Refund toDomain(RefundJpaEntity entity, ReturnRequest request) {
        return ReturnJpaMapper.toDomain(entity, request,
                referenceResolver.user(entity.getProcessedById(), Administrator.class));
    }
}
