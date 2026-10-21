package no.casevault.controller;

import no.casevault.dto.DistrictOverviewResponse;
import no.casevault.exception.InvalidCredentialsException;
import no.casevault.model.District;
import no.casevault.model.Role;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import no.casevault.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    public ReportController(ReportService reportService, UserRepository userRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

    @GetMapping("/district-overview")
    public DistrictOverviewResponse districtOverview(@RequestParam(required = false) District district,
                                                       Authentication authentication) {
        User caller = currentUser(authentication);
        District target = (caller.getRole() == Role.ADMIN && district != null) ? district : caller.getDistrict();
        return reportService.districtOverview(target);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(InvalidCredentialsException::new);
    }
}
