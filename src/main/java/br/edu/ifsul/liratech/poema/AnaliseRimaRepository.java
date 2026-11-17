package br.edu.ifsul.liratech.poema;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnaliseRimaRepository extends JpaRepository<AnaliseRima, UUID> {

    /** Palavra final e classe gramatical: o que o dicionário de rimas mostra. */
    interface PalavraQueRima {
        String getPalavra();

        String getClasseGramatical();
    }

    /**
     * Palavras finais já usadas na plataforma com a mesma terminação sonora.
     * Devolve só palavras isoladas, nunca versos, para preservar as obras.
     */
    @Query("""
            select distinct a.palavraFinal as palavra, a.classeGramatical as classeGramatical
            from AnaliseRima a
            where a.fonemaTonico = :fonema and a.palavraFinal <> :palavra
            order by a.palavraFinal
            """)
    List<PalavraQueRima> palavrasQueRimam(String fonema, String palavra);
}
