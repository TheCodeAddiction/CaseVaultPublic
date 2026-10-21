package no.casevault.controller;

import jakarta.validation.Valid;
import no.casevault.dto.ActivityLogEntryResponse;
import no.casevault.dto.AssignCaseRequest;
import no.casevault.dto.CaseDetailResponse;
import no.casevault.dto.CasePageResponse;
import no.casevault.dto.CaseShareResponse;
import no.casevault.dto.CaseStatsResponse;
import no.casevault.dto.CaseSummaryResponse;
import no.casevault.dto.CreateCaseRequest;
import no.casevault.dto.MessageResponse;
import no.casevault.dto.ShareCaseRequest;
import no.casevault.dto.UpdateCaseStatusRequest;
import no.casevault.exception.CaseNotFoundException;
import no.casevault.exception.InvalidCaseOperationException;
import no.casevault.exception.InvalidCredentialsException;
import no.casevault.model.Case;
import no.casevault.model.CaseShare;
import no.casevault.model.CaseStatus;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import no.casevault.service.AuditLogService;
import no.casevault.service.CaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
@PreAuthorize("hasAnyRole('OFFICER','SUPERVISOR','ADMIN')")
public class CaseController {

    private static final String ENTITY_TYPE = "CASE";

    private final CaseService caseService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public CaseController(CaseService caseService, AuditLogService auditLogService, UserRepository userRepository) {
        this.caseService = caseService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public CasePageResponse listCases(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) CaseStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        User user = currentUser(authentication);
        List<Case> matches = caseService.listCasesVisibleTo(user, q, status);

        int totalElements = matches.size();
        int totalPages = (int) Math.ceil(totalElements / (double) size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<CaseSummaryResponse> content = matches.subList(fromIndex, toIndex).stream()
            .map(CaseSummaryResponse::from)
            .toList();

        return new CasePageResponse(content, page, size, totalElements, totalPages);
    }

    @GetMapping("/stats")
    public CaseStatsResponse caseStats(Authentication authentication) {
        User user = currentUser(authentication);
        List<Case> visible = caseService.listCasesVisibleTo(user);

        long open = visible.stream().filter(c -> c.getStatus() == CaseStatus.OPEN).count();
        long inProgress = visible.stream().filter(c -> c.getStatus() == CaseStatus.IN_PROGRESS).count();
        long closed = visible.stream().filter(c -> c.getStatus() == CaseStatus.CLOSED).count();

        List<CaseSummaryResponse> recentCases = visible.stream()
            .sorted(Comparator.comparing(Case::getCreatedAt).reversed())
            .limit(5)
            .map(CaseSummaryResponse::from)
            .toList();

        return new CaseStatsResponse(visible.size(), open, inProgress, closed, recentCases);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportCases(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) CaseStatus status,
        Authentication authentication
    ) {
        User user = currentUser(authentication);
        List<Case> matches = caseService.listCasesVisibleTo(user, q, status);

        StringBuilder csv = new StringBuilder("Case Number,Title,Status,District,Assigned Officer,Created At\n");
        for (Case c : matches) {
            csv.append(csvField(c.getCaseNumber())).append(',')
                .append(csvField(c.getTitle())).append(',')
                .append(csvField(c.getStatus().name())).append(',')
                .append(csvField(c.getDistrict().name())).append(',')
                .append(csvField(c.getAssignedOfficer() != null ? c.getAssignedOfficer().getUsername() : "")).append(',')
                .append(csvField(c.getCreatedAt().toString())).append('\n');
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header("Content-Disposition", "attachment; filename=\"cases-export.csv\"")
            .body(csv.toString());
    }

    @PostMapping
    public ResponseEntity<CaseDetailResponse> createCase(@Valid @RequestBody CreateCaseRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        Case newCase = caseService.createCase(user, request);

        auditLogService.record(user, ENTITY_TYPE, newCase.getId().toString(), "CASE_CREATED", "Case created");

        return ResponseEntity.status(HttpStatus.CREATED).body(CaseDetailResponse.from(newCase));
    }

    @GetMapping("/{id}")
    public CaseDetailResponse getCase(@PathVariable UUID id, Authentication authentication) {
        User user = currentUser(authentication);
        Case c = requireVisibleCase(user, id);
        return CaseDetailResponse.from(c);
    }

    @PutMapping("/{id}/status")
    public CaseDetailResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateCaseStatusRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        Case c = requireVisibleCase(user, id);

        CaseStatus previousStatus = c.getStatus();
        Case updated = caseService.changeStatus(c, request.status());

        auditLogService.record(user, ENTITY_TYPE, id.toString(), "STATUS_CHANGED",
            "Status changed from " + previousStatus + " to " + request.status());

        return CaseDetailResponse.from(updated);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PutMapping("/{id}/assignee")
    public CaseDetailResponse assignOfficer(@PathVariable UUID id, @Valid @RequestBody AssignCaseRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        Case c = requireVisibleCase(user, id);
        User officer = userRepository.findByUsernameIgnoreCase(request.officerUsername())
            .orElseThrow(InvalidCredentialsException::new);

        Case updated = caseService.assignOfficer(c, officer);

        auditLogService.record(user, ENTITY_TYPE, id.toString(), "CASE_ASSIGNED", "Assigned to " + officer.getUsername());

        return CaseDetailResponse.from(updated);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/{id}/shares")
    public ResponseEntity<CaseShareResponse> shareCase(@PathVariable UUID id, @RequestBody ShareCaseRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        Case c = caseService.findById(id);

        boolean hasDistrict = request.district() != null;
        boolean hasUsername = request.username() != null && !request.username().isBlank();
        if (hasDistrict == hasUsername) {
            throw new InvalidCaseOperationException("Provide exactly one of district or username");
        }

        CaseShare share;
        String detail;
        if (hasDistrict) {
            share = caseService.shareCaseWithDistrict(user, c, request.district());
            detail = "Shared with " + request.district();
        } else {
            User targetUser = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(InvalidCredentialsException::new);
            share = caseService.shareCaseWithUser(user, c, targetUser);
            detail = "Shared with " + targetUser.getUsername();
        }

        auditLogService.record(user, ENTITY_TYPE, id.toString(), "CASE_SHARED", detail);

        return ResponseEntity.status(HttpStatus.CREATED).body(CaseShareResponse.from(share));
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @DeleteMapping("/{id}/shares/{shareId}")
    public MessageResponse revokeShare(@PathVariable UUID id, @PathVariable Long shareId, Authentication authentication) {
        User user = currentUser(authentication);
        Case c = requireVisibleCase(user, id);

        CaseShare share = caseService.revokeShare(user, c, shareId);
        String detail = share.getSharedWithDistrict() != null
            ? "Removed share with " + share.getSharedWithDistrict()
            : "Removed share with " + share.getSharedWithUser().getUsername();

        auditLogService.record(user, ENTITY_TYPE, id.toString(), "CASE_UNSHARED", detail);

        return new MessageResponse("Share removed.");
    }

    @GetMapping("/{id}/shares")
    public List<CaseShareResponse> listShares(@PathVariable UUID id, Authentication authentication) {
        User user = currentUser(authentication);
        requireVisibleCase(user, id);

        return caseService.listShares(id).stream()
            .map(CaseShareResponse::from)
            .toList();
    }

    @GetMapping("/{id}/activity")
    public List<ActivityLogEntryResponse> getCaseActivity(@PathVariable UUID id, Authentication authentication) {
        User user = currentUser(authentication);
        requireVisibleCase(user, id);

        return auditLogService.findFor(ENTITY_TYPE, id.toString()).stream()
            .map(ActivityLogEntryResponse::from)
            .toList();
    }

    private String csvField(String value) {
        if (!value.isEmpty()) {
            char first = value.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                value = "'" + value;
            }
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private Case requireVisibleCase(User user, UUID id) {
        return caseService.findVisibleCase(user, id).orElseThrow(() -> new CaseNotFoundException(id));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(InvalidCredentialsException::new);
    }
}
