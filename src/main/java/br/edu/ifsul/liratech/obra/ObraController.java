package br.edu.ifsul.liratech.obra;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifsul.liratech.erro.RecursoNaoEncontradoException;
import br.edu.ifsul.liratech.poema.Poema;
import br.edu.ifsul.liratech.poema.PoemaRepository;
import br.edu.ifsul.liratech.seguranca.ServicoDeToken;
import br.edu.ifsul.liratech.usuario.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Gerenciamento da hierarquia Obra > Ato > Poema (RF01). */
@RestController
@Transactional
public class ObraController {

    // --- Formatos de entrada -------------------------------------------------
    public record DadosDaObra(@NotBlank @Size(max = 200) String titulo, @Size(max = 2000) String sinopse) {
    }

    public record DadosDoTitulo(@NotBlank @Size(max = 200) String titulo) {
    }

    // --- Formatos de saída ---------------------------------------------------
    public record ResumoDoPoema(UUID id, String titulo, Instant criadoEm) {
    }

    public record AtoDetalhado(UUID id, String titulo, int ordem, List<ResumoDoPoema> poemas) {
    }

    public record ResumoDaObra(UUID id, String titulo, String sinopse, Instant criadoEm, int atos, int poemas) {
    }

    public record ObraDetalhada(UUID id, String titulo, String sinopse, Instant criadoEm, List<AtoDetalhado> atos) {
    }

    private final ObraRepository obras;
    private final AtoRepository atos;
    private final PoemaRepository poemas;
    private final UsuarioRepository usuarios;

    public ObraController(ObraRepository obras, AtoRepository atos, PoemaRepository poemas,
            UsuarioRepository usuarios) {
        this.obras = obras;
        this.atos = atos;
        this.poemas = poemas;
        this.usuarios = usuarios;
    }

    // --- Obras -----------------------------------------------------------------
    @GetMapping("/obras")
    public List<ResumoDaObra> listar(@AuthenticationPrincipal Jwt token) {
        return obras.findByUsuarioIdOrderByCriadoEmDesc(ServicoDeToken.idDoUsuario(token)).stream()
                .map(obra -> new ResumoDaObra(obra.getId(), obra.getTitulo(), obra.getSinopse(), obra.getCriadoEm(),
                        obra.getAtos().size(),
                        obra.getAtos().stream().mapToInt(ato -> ato.getPoemas().size()).sum()))
                .toList();
    }

    @PostMapping("/obras")
    @ResponseStatus(HttpStatus.CREATED)
    public ObraDetalhada criar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody DadosDaObra dados) {
        var usuario = usuarios.getReferenceById(ServicoDeToken.idDoUsuario(token));
        return detalhar(obras.save(new Obra(usuario, dados.titulo().trim(), dados.sinopse())));
    }

    @GetMapping("/obras/{id}")
    public ObraDetalhada consultar(@AuthenticationPrincipal Jwt token, @PathVariable UUID id) {
        return detalhar(obraDoUsuario(token, id));
    }

    @PutMapping("/obras/{id}")
    public ObraDetalhada alterar(@AuthenticationPrincipal Jwt token, @PathVariable UUID id,
            @Valid @RequestBody DadosDaObra dados) {
        Obra obra = obraDoUsuario(token, id);
        obra.setTitulo(dados.titulo().trim());
        obra.setSinopse(dados.sinopse());
        return detalhar(obra);
    }

    @DeleteMapping("/obras/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt token, @PathVariable UUID id) {
        obras.delete(obraDoUsuario(token, id));
    }

    // --- Atos --------------------------------------------------------------------
    @PostMapping("/obras/{id}/atos")
    @ResponseStatus(HttpStatus.CREATED)
    public AtoDetalhado criarAto(@AuthenticationPrincipal Jwt token, @PathVariable UUID id,
            @Valid @RequestBody DadosDoTitulo dados) {
        Obra obra = obraDoUsuario(token, id);
        int ordem = obra.getAtos().stream().mapToInt(Ato::getOrdem).max().orElse(0) + 1;
        Ato ato = atos.save(new Ato(obra, dados.titulo().trim(), ordem));
        obra.getAtos().add(ato);
        return detalhar(ato);
    }

    @PutMapping("/atos/{id}")
    public AtoDetalhado alterarAto(@AuthenticationPrincipal Jwt token, @PathVariable UUID id,
            @Valid @RequestBody DadosDoTitulo dados) {
        Ato ato = atoDoUsuario(token, id);
        ato.setTitulo(dados.titulo().trim());
        return detalhar(ato);
    }

    @DeleteMapping("/atos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirAto(@AuthenticationPrincipal Jwt token, @PathVariable UUID id) {
        Ato ato = atoDoUsuario(token, id);
        ato.getObra().getAtos().remove(ato); // orphanRemoval exclui o ato e os seus poemas
    }

    // --- Poemas (criação; o conteúdo é tratado pelo PoemaController) ---------------
    @PostMapping("/atos/{id}/poemas")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumoDoPoema criarPoema(@AuthenticationPrincipal Jwt token, @PathVariable UUID id,
            @Valid @RequestBody DadosDoTitulo dados) {
        Ato ato = atoDoUsuario(token, id);
        Poema poema = poemas.save(new Poema(ato, dados.titulo().trim()));
        ato.getPoemas().add(poema);
        return new ResumoDoPoema(poema.getId(), poema.getTitulo(), poema.getCriadoEm());
    }

    // --- Apoio -----------------------------------------------------------------------
    private Obra obraDoUsuario(Jwt token, UUID id) {
        return obras.findByIdAndUsuarioId(id, ServicoDeToken.idDoUsuario(token))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra"));
    }

    private Ato atoDoUsuario(Jwt token, UUID id) {
        return atos.findByIdAndObraUsuarioId(id, ServicoDeToken.idDoUsuario(token))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ato"));
    }

    private ObraDetalhada detalhar(Obra obra) {
        return new ObraDetalhada(obra.getId(), obra.getTitulo(), obra.getSinopse(), obra.getCriadoEm(),
                obra.getAtos().stream().map(this::detalhar).toList());
    }

    private AtoDetalhado detalhar(Ato ato) {
        return new AtoDetalhado(ato.getId(), ato.getTitulo(), ato.getOrdem(), ato.getPoemas().stream()
                .map(poema -> new ResumoDoPoema(poema.getId(), poema.getTitulo(), poema.getCriadoEm())).toList());
    }
}
