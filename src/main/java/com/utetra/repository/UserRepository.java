package com.utetra.repository;
import com.utetra.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface UserRepository extends JpaRepository<User,Long> {
    @EntityGraph(attributePaths="role") Optional<User> findByEmail(String email);
}
