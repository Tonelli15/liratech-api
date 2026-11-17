package br.edu.ifsul.liratech.poema;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Dados de rima de um verso: palavra final, terminação sonora normalizada e
 * classe gramatical. O índice na terminação atende ao dicionário de rimas.
 */
@Entity
@Table(name = "analise_rima", indexes = @Index(name = "idx_analise_rima_fonema", columnList = "fonema_tonico"))
public class AnaliseRima {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_verso", unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE) // exclusão em cascata feita pelo PostgreSQL (RNF01)
    private Verso verso;

    @Column(name = "palavra_final", nullable = false, length = 100)
    private String palavraFinal;

    @Column(name = "fonema_tonico", nullable = false, length = 100)
    private String fonemaTonico;

    @Column(name = "classe_gramatical", nullable = false, length = 40)
    private String classeGramatical;

    protected AnaliseRima() {
    }

    public AnaliseRima(Verso verso, String palavraFinal, String fonemaTonico, String classeGramatical) {
        this.verso = verso;
        this.palavraFinal = palavraFinal;
        this.fonemaTonico = fonemaTonico;
        this.classeGramatical = classeGramatical;
    }

    public String getPalavraFinal() {
        return palavraFinal;
    }

    public String getFonemaTonico() {
        return fonemaTonico;
    }

    public String getClasseGramatical() {
        return classeGramatical;
    }
}
