package no.casevault.repository;

import no.casevault.model.District;
import no.casevault.model.Role;
import no.casevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<User> findByDistrictAndRole(District district, Role role);

    @Query("SELECT u FROM User u WHERE u.active = true AND "
        + "(LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))) "
        + "ORDER BY u.fullName")
    List<User> searchActiveByUsernameOrFullName(@Param("q") String q);
}
