package br.edu.ifsul.liratech.obra;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.edu.ifsul.liratech.usuario.Usuario;
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

/** Obra literária de um usuário; agrupa os atos (RF01). */
@Entity
@Table(name = "obra")
public class Obra {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    @OnDelete(action = OnDeleteAction.CASCADE) // exclusão em cascata feita pelo PostgreSQL (RNF01)
    private Usuario usuario;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String sinopse;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    // Excluir a obra exclui os atos (ON DELETE CASCADE do modelo conceitual).
    @OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<Ato> atos = new ArrayList<>();

    protected Obra() {
    }

    public Obra(Usuario usuario, String titulo, String sinopse) {
        this.usuario = usuario;
        this.titulo = titulo;
        this.sinopse = sinopse;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getSinopse() {
        return sinopse;
    }

    public void setSinopse(String sinopse) {
        this.sinopse = sinopse;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public List<Ato> getAtos() {
        return atos;
    }
}
