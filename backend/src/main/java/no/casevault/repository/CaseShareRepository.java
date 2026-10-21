package no.casevault.repository;

import no.casevault.model.CaseShare;
import no.casevault.model.District;
import no.casevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CaseShareRepository extends JpaRepository<CaseShare, Long> {

    boolean existsByCaseRecordIdAndSharedWithDistrict(UUID caseId, District district);

    boolean existsByCaseRecordIdAndSharedWithUserId(UUID caseId, Long userId);

    List<CaseShare> findByCaseRecordIdOrderBySharedAtDesc(UUID caseId);

    List<CaseShare> findBySharedWithDistrict(District district);

    List<CaseShare> findBySharedWithUserId(Long userId);

    boolean existsBySharedByOrSharedWithUser(User sharedBy, User sharedWithUser);
}
