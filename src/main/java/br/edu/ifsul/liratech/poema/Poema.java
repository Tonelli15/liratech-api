package br.edu.ifsul.liratech.poema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.edu.ifsul.liratech.obra.Ato;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Poema: guarda o texto como foi digitado e a sua divisão em estrofes e versos. */
@Entity
@Table(name = "poema")
public class Poema {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ato")
    @OnDelete(action = OnDeleteAction.CASCADE) // exclusão em cascata feita pelo PostgreSQL (RNF01)
    private Ato ato;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "conteudo_bruto", nullable = false, columnDefinition = "text")
    private String conteudoBruto = "";

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @OneToMany(mappedBy = "poema", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<Estrofe> estrofes = new ArrayList<>();

    protected Poema() {
    }

    public Poema(Ato ato, String titulo) {
        this.ato = ato;
        this.titulo = titulo;
    }

    public UUID getId() {
        return id;
    }

    public Ato getAto() {
        return ato;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getConteudoBruto() {
        return conteudoBruto;
    }

    public void setConteudoBruto(String conteudoBruto) {
        this.conteudoBruto = conteudoBruto;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public List<Estrofe> getEstrofes() {
        return estrofes;
    }

}
