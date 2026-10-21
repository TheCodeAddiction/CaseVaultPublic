package no.casevault.repository;

import no.casevault.model.Case;
import no.casevault.model.District;
import no.casevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CaseRepository extends JpaRepository<Case, UUID> {

    List<Case> findByDistrict(District district);

    boolean existsByAssignedOfficer(User assignedOfficer);
}
