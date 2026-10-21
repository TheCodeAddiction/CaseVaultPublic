package no.casevault.config;

import no.casevault.model.Case;
import no.casevault.model.CaseStatus;
import no.casevault.model.District;
import no.casevault.repository.CaseRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Profile("!test")
public class DemoDataSeeder implements ApplicationRunner {

    private final CaseRepository caseRepository;

    public DemoDataSeeder(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (caseRepository.count() > 0) {
            return;
        }

        Instant now = Instant.now();
        for (District district : District.values()) {
            int sequence = 1;
            caseRepository.save(new Case(
                caseNumber(district, sequence++),
                "Break-in reported at local business",
                "Responding officer found forced entry through the rear door. Witnesses report a dark-colored van leaving the scene.",
                CaseStatus.OPEN,
                district,
                null,
                now.minus(5, ChronoUnit.DAYS)
            ));
            caseRepository.save(new Case(
                caseNumber(district, sequence++),
                "Vehicle theft from parking garage",
                "Owner reports vehicle missing after a shift change. No signs of forced entry into the garage itself.",
                CaseStatus.IN_PROGRESS,
                district,
                null,
                now.minus(2, ChronoUnit.DAYS)
            ));
            caseRepository.save(new Case(
                caseNumber(district, sequence++),
                "Confidential informant safety review",
                "Internal review of informant handling procedures following a compromised operation.",
                CaseStatus.OPEN,
                district,
                null,
                now.minus(1, ChronoUnit.DAYS)
            ));
        }
    }

    private String caseNumber(District district, int sequence) {
        return "%s-2026-%04d".formatted(district.name(), sequence);
    }
}
