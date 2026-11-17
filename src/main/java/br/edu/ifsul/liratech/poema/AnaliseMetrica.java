package br.edu.ifsul.liratech.poema;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Escansão de um verso, como devolvida pelo motor de PLN. */
@Entity
@Table(name = "analise_metrica")
public class AnaliseMetrica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_verso", unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE) // exclusão em cascata feita pelo PostgreSQL (RNF01)
    private Verso verso;

    @Column(name = "separacao_gramatical", nullable = false, length = 1000)
    private String separacaoGramatical;

    @Column(name = "separacao_poetica", nullable = false, length = 1000)
    private String separacaoPoetica;

    @Column(name = "total_silabas", nullable = false)
    private int totalSilabas;

    @Column(nullable = false, length = 60)
    private String classificacao;

    protected AnaliseMetrica() {
    }

    public AnaliseMetrica(Verso verso, String separacaoGramatical, String separacaoPoetica, int totalSilabas,
            String classificacao) {
        this.verso = verso;
        this.separacaoGramatical = separacaoGramatical;
        this.separacaoPoetica = separacaoPoetica;
        this.totalSilabas = totalSilabas;
        this.classificacao = classificacao;
    }

    public String getSeparacaoGramatical() {
        return separacaoGramatical;
    }

    public String getSeparacaoPoetica() {
        return separacaoPoetica;
    }

    public int getTotalSilabas() {
        return totalSilabas;
    }

    public String getClassificacao() {
        return classificacao;
    }
}
