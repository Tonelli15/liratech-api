package br.edu.ifsul.liratech.obra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AtoRepository extends JpaRepository<Ato, UUID> {

    Optional<Ato> findByIdAndObraUsuarioId(UUID id, UUID idUsuario);
}
