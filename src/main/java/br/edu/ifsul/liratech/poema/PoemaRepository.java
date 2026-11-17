package br.edu.ifsul.liratech.poema;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PoemaRepository extends JpaRepository<Poema, UUID> {

    /** Percorre Poema > Ato > Obra > Usuario para garantir que o poema é de quem pede. */
    Optional<Poema> findByIdAndAtoObraUsuarioId(UUID id, UUID idUsuario);
}
