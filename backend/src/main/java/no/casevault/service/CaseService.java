package no.casevault.service;

import no.casevault.dto.CreateCaseRequest;
import no.casevault.exception.CaseNotFoundException;
import no.casevault.exception.CaseShareNotFoundException;
import no.casevault.exception.InvalidCaseOperationException;
import no.casevault.model.Case;
import no.casevault.model.CaseShare;
import no.casevault.model.CaseStatus;
import no.casevault.model.District;
import no.casevault.model.Role;
import no.casevault.model.User;
import no.casevault.repository.CaseRepository;
import no.casevault.repository.CaseShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseShareRepository caseShareRepository;

    public CaseService(CaseRepository caseRepository, CaseShareRepository caseShareRepository) {
        this.caseRepository = caseRepository;
        this.caseShareRepository = caseShareRepository;
    }

    public List<Case> listCasesVisibleTo(User user, String query, CaseStatus status) {
        List<Case> base = listCasesVisibleTo(user);

        return base.stream()
            .filter(c -> status == null || c.getStatus() == status)
            .filter(c -> !StringUtils.hasText(query) || matchesKeyword(c, query))
            .toList();
    }

    public List<Case> listCasesVisibleTo(User user) {
        if (user.getRole() == Role.ADMIN) {
            return caseRepository.findAll();
        }

        List<Case> owned = caseRepository.findByDistrict(user.getDistrict());
        List<Case> sharedWithDistrict = caseShareRepository.findBySharedWithDistrict(user.getDistrict()).stream()
            .map(CaseShare::getCaseRecord)
            .toList();
        List<Case> sharedWithMe = caseShareRepository.findBySharedWithUserId(user.getId()).stream()
            .map(CaseShare::getCaseRecord)
            .toList();

        Map<UUID, Case> merged = new LinkedHashMap<>();
        owned.forEach(c -> merged.put(c.getId(), c));
        sharedWithDistrict.forEach(c -> merged.put(c.getId(), c));
        sharedWithMe.forEach(c -> merged.put(c.getId(), c));

        return merged.values().stream().toList();
    }

    public Optional<Case> findVisibleCase(User user, UUID caseId) {
        return caseRepository.findById(caseId)
            .filter(c -> isVisibleTo(user, c));
    }

    public Case findById(UUID caseId) {
        return caseRepository.findById(caseId).orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    public List<Case> listAll() {
        return caseRepository.findAll();
    }

    public Case createCase(User creator, CreateCaseRequest request) {
        Instant now = Instant.now();
        String caseNumber = generateCaseNumber(creator.getDistrict(), now);

        Case newCase = new Case(
            caseNumber,
            request.title(),
            request.description(),
            CaseStatus.OPEN,
            creator.getDistrict(),
            creator,
            now
        );
        return caseRepository.save(newCase);
    }

    public Case changeStatus(Case caseRecord, CaseStatus newStatus) {
        caseRecord.changeStatus(newStatus);
        return caseRepository.save(caseRecord);
    }

    public Case assignOfficer(Case caseRecord, User officer) {
        if (officer.getRole() != Role.OFFICER) {
            throw new InvalidCaseOperationException("Cases can only be assigned to officers");
        }
        if (!officer.getDistrict().equals(caseRecord.getDistrict())) {
            throw new InvalidCaseOperationException("Officer is not in the case's district");
        }
        caseRecord.assignOfficer(officer);
        return caseRepository.save(caseRecord);
    }

    public CaseShare shareCaseWithDistrict(User actor, Case caseRecord, District targetDistrict) {
        if (targetDistrict.equals(caseRecord.getDistrict())) {
            throw new InvalidCaseOperationException("Case is already in this district");
        }
        if (caseShareRepository.existsByCaseRecordIdAndSharedWithDistrict(caseRecord.getId(), targetDistrict)) {
            throw new InvalidCaseOperationException("Case is already shared with this district");
        }
        return caseShareRepository.save(CaseShare.withDistrict(caseRecord, targetDistrict, actor, Instant.now()));
    }

    public CaseShare shareCaseWithUser(User actor, Case caseRecord, User targetUser) {
        if (targetUser.getId().equals(actor.getId())) {
            throw new InvalidCaseOperationException("Cannot share a case with yourself");
        }
        if (caseShareRepository.existsByCaseRecordIdAndSharedWithUserId(caseRecord.getId(), targetUser.getId())) {
            throw new InvalidCaseOperationException("Case is already shared with this user");
        }
        return caseShareRepository.save(CaseShare.withUser(caseRecord, targetUser, actor, Instant.now()));
    }

    public List<CaseShare> listShares(UUID caseId) {
        return caseShareRepository.findByCaseRecordIdOrderBySharedAtDesc(caseId);
    }

    public CaseShare revokeShare(User actor, Case caseRecord, Long shareId) {
        requireCanShare(actor, caseRecord);

        CaseShare share = caseShareRepository.findById(shareId)
            .orElseThrow(() -> new CaseShareNotFoundException(shareId));
        if (!share.getCaseRecord().getId().equals(caseRecord.getId())) {
            throw new CaseShareNotFoundException(shareId);
        }

        caseShareRepository.delete(share);
        return share;
    }

    private void requireCanShare(User actor, Case caseRecord) {
        if (actor.getRole() == Role.SUPERVISOR && !actor.getDistrict().equals(caseRecord.getDistrict())) {
            throw new InvalidCaseOperationException("Supervisors can only share cases from their own district");
        }
    }

    private boolean matchesKeyword(Case c, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        return c.getTitle().toLowerCase(Locale.ROOT).contains(needle)
            || c.getDescription().toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean isVisibleTo(User user, Case c) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        boolean sameDistrict = c.getDistrict().equals(user.getDistrict());
        boolean sharedWithDistrict = !sameDistrict
            && caseShareRepository.existsByCaseRecordIdAndSharedWithDistrict(c.getId(), user.getDistrict());
        boolean sharedWithMe = caseShareRepository.existsByCaseRecordIdAndSharedWithUserId(c.getId(), user.getId());

        return sameDistrict || sharedWithDistrict || sharedWithMe;
    }

    private String generateCaseNumber(District district, Instant now) {
        int year = now.atZone(ZoneOffset.UTC).getYear();
        long sequence = caseRepository.findByDistrict(district).size() + 1L;
        return "%s-%d-%04d".formatted(district.name(), year, sequence);
    }
}
