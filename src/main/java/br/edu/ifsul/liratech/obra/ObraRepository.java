package br.edu.ifsul.liratech.obra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Todas as buscas levam o id do usuário: ninguém enxerga a obra de outra pessoa. */
public interface ObraRepository extends JpaRepository<Obra, UUID> {

    List<Obra> findByUsuarioIdOrderByCriadoEmDesc(UUID idUsuario);

    Optional<Obra> findByIdAndUsuarioId(UUID id, UUID idUsuario);
}
