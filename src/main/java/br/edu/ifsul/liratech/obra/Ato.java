package br.edu.ifsul.liratech.obra;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.edu.ifsul.liratech.poema.Poema;
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

/** Divisão de uma obra (ato, parte ou seção); agrupa os poemas. */
@Entity
@Table(name = "ato")
public class Ato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_obra")
    @OnDelete(action = OnDeleteAction.CASCADE) // exclusão em cascata feita pelo PostgreSQL (RNF01)
    private Obra obra;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false)
    private int ordem;

    @OneToMany(mappedBy = "ato", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criadoEm")
    private List<Poema> poemas = new ArrayList<>();

    protected Ato() {
    }

    public Ato(Obra obra, String titulo, int ordem) {
        this.obra = obra;
        this.titulo = titulo;
        this.ordem = ordem;
    }

    public UUID getId() {
        return id;
    }

    public Obra getObra() {
        return obra;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public int getOrdem() {
        return ordem;
    }

    public List<Poema> getPoemas() {
        return poemas;
    }
}
