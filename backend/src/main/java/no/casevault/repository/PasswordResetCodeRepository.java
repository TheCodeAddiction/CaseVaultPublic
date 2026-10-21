package no.casevault.repository;

import no.casevault.model.PasswordResetCode;
import no.casevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    void deleteAllByUser(User user);
}
