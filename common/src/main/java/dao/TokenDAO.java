package dao;

import entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenDAO extends JpaRepository<Token, Long> {

    @Query("""
        select t from Token t where t.user.id = :id and (t.expired = false and t.revoked = false)
    """)
    List<Token> findAllValidTokensByUser(@Param("id") UUID id);

    Optional<Token> findByToken(String token);
}
