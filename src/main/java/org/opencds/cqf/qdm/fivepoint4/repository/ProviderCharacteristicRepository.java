package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.ProviderCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface ProviderCharacteristicRepository extends JpaRepository<ProviderCharacteristic, String>
{
    @Nonnull
    Optional<ProviderCharacteristic> findBySystemId(@Nonnull String id);
}