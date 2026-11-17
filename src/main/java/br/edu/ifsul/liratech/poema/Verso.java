package br.edu.ifsul.liratech.poema;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Verso de uma estrofe. As análises são opcionais (cardinalidade 0,1): o verso
 * é salvo mesmo quando o motor de PLN está fora do ar ou não consegue analisá-lo.
 */
@Entity
@Table(name = "verso")
public class Verso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estrofe")
    @OnDelete(action = OnDeleteAction.CASCADE) // exclusão em cascata feita pelo PostgreSQL (RNF01)
    private Estrofe estrofe;

    @Column(name = "texto_original", nullable = false, length = 500)
    private String textoOriginal;

    @Column(name = "texto_limpo", length = 500)
    private String textoLimpo;

    @Column(nullable = false)
    private int ordem;

    /** Versão do motor que produziu as análises; nula enquanto o verso não foi analisado. */
    @Column(name = "versao_motor", length = 20)
    private String versaoMotor;

    @Column(name = "modelo_spacy", length = 60)
    private String modeloSpacy;

    /** Avisos do motor, um por linha. */
    @Column(length = 1000)
    private String avisos;

    @OneToOne(mappedBy = "verso", cascade = CascadeType.ALL, orphanRemoval = true)
    private AnaliseMetrica analiseMetrica;

    @OneToOne(mappedBy = "verso", cascade = CascadeType.ALL, orphanRemoval = true)
    private AnaliseRima analiseRima;

    protected Verso() {
    }

    public Verso(Estrofe estrofe, String textoOriginal, int ordem) {
        this.estrofe = estrofe;
        this.textoOriginal = textoOriginal;
        this.ordem = ordem;
    }

    public UUID getId() {
        return id;
    }

    public String getTextoOriginal() {
        return textoOriginal;
    }

    public String getTextoLimpo() {
        return textoLimpo;
    }

    public void setTextoLimpo(String textoLimpo) {
        this.textoLimpo = textoLimpo;
    }

    public int getOrdem() {
        return ordem;
    }

    public String getVersaoMotor() {
        return versaoMotor;
    }

    public String getModeloSpacy() {
        return modeloSpacy;
    }

    public void registrarMotor(String versaoMotor, String modeloSpacy) {
        this.versaoMotor = versaoMotor;
        this.modeloSpacy = modeloSpacy;
    }

    public List<String> getAvisos() {
        return avisos == null || avisos.isBlank() ? List.of() : List.of(avisos.split("\n"));
    }

    public void setAvisos(List<String> avisos) {
        this.avisos = avisos.isEmpty() ? null : String.join("\n", avisos);
    }

    public AnaliseMetrica getAnaliseMetrica() {
        return analiseMetrica;
    }

    public void setAnaliseMetrica(AnaliseMetrica analiseMetrica) {
        this.analiseMetrica = analiseMetrica;
    }

    public AnaliseRima getAnaliseRima() {
        return analiseRima;
    }

    public void setAnaliseRima(AnaliseRima analiseRima) {
        this.analiseRima = analiseRima;
    }
}
