package br.edu.ifsul.liratech.poema;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Classe de associação do autorrelacionamento "rima com": liga dois versos do
 * mesmo poema e guarda a riqueza da rima, que é uma propriedade do par.
 * As chaves estrangeiras usam ON DELETE CASCADE: quando um verso ou o poema é
 * excluído, o próprio banco remove os pares correspondentes.
 */
@Entity
@Table(name = "rima_verso")
public class RimaVerso {

    public static final String RICA = "rica";
    public static final String POBRE = "pobre";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poema")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Poema poema;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_verso_a")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Verso versoA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_verso_b")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Verso versoB;

    @Column(name = "classificacao_riqueza", nullable = false, length = 10)
    private String classificacaoRiqueza;

    protected RimaVerso() {
    }

    public RimaVerso(Poema poema, Verso versoA, Verso versoB, String classificacaoRiqueza) {
        this.poema = poema;
        this.versoA = versoA;
        this.versoB = versoB;
        this.classificacaoRiqueza = classificacaoRiqueza;
    }

    public Verso getVersoA() {
        return versoA;
    }

    public Verso getVersoB() {
        return versoB;
    }

    public String getClassificacaoRiqueza() {
        return classificacaoRiqueza;
    }
}
