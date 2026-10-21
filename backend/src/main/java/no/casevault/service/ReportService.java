package no.casevault.service;

import no.casevault.dto.DistrictOverviewResponse;
import no.casevault.dto.OfficerWorkloadResponse;
import no.casevault.model.Case;
import no.casevault.model.CaseStatus;
import no.casevault.model.District;
import no.casevault.model.Role;
import no.casevault.repository.CaseRepository;
import no.casevault.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;

    public ReportService(CaseRepository caseRepository, UserRepository userRepository) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
    }

    public DistrictOverviewResponse districtOverview(District district) {
        List<Case> cases = caseRepository.findByDistrict(district);

        long openCases = countByStatus(cases, CaseStatus.OPEN);
        long inProgressCases = countByStatus(cases, CaseStatus.IN_PROGRESS);
        long closedCases = countByStatus(cases, CaseStatus.CLOSED);

        Map<Long, Long> assignedCountsByOfficerId = cases.stream()
            .filter(c -> c.getAssignedOfficer() != null)
            .collect(Collectors.groupingBy(c -> c.getAssignedOfficer().getId(), Collectors.counting()));

        List<OfficerWorkloadResponse> officerWorkload = userRepository.findByDistrictAndRole(district, Role.OFFICER).stream()
            .map(officer -> new OfficerWorkloadResponse(
                officer.getUsername(),
                officer.getFullName(),
                assignedCountsByOfficerId.getOrDefault(officer.getId(), 0L)
            ))
            .toList();

        return new DistrictOverviewResponse(district, cases.size(), openCases, inProgressCases, closedCases, officerWorkload);
    }

    private long countByStatus(List<Case> cases, CaseStatus status) {
        return cases.stream().filter(c -> c.getStatus() == status).count();
    }
}
