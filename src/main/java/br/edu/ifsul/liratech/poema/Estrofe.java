package br.edu.ifsul.liratech.poema;

import java.util.ArrayList;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "estrofe")
public class Estrofe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poema")
    @OnDelete(action = OnDeleteAction.CASCADE) // exclusão em cascata feita pelo PostgreSQL (RNF01)
    private Poema poema;

    @Column(nullable = false)
    private int ordem;

    @OneToMany(mappedBy = "estrofe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<Verso> versos = new ArrayList<>();

    protected Estrofe() {
    }

    public Estrofe(Poema poema, int ordem) {
        this.poema = poema;
        this.ordem = ordem;
    }

    public int getOrdem() {
        return ordem;
    }

    public List<Verso> getVersos() {
        return versos;
    }
}
