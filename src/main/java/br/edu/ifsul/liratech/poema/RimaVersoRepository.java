package br.edu.ifsul.liratech.poema;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RimaVersoRepository extends JpaRepository<RimaVerso, UUID> {

    List<RimaVerso> findByPoemaId(UUID idPoema);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("delete from RimaVerso r where r.poema.id = :idPoema")
    void excluirDoPoema(UUID idPoema);
}
