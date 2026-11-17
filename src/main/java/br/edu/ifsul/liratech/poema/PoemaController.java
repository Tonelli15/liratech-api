package br.edu.ifsul.liratech.poema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifsul.liratech.analise.ClienteDoMotor;
import br.edu.ifsul.liratech.analise.ServicoDeAnalise;
import br.edu.ifsul.liratech.erro.RecursoNaoEncontradoException;
import br.edu.ifsul.liratech.seguranca.ServicoDeToken;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edição do poema e relatório estrutural devolvido ao editor (RF02 a RF05). */
@RestController
@Transactional
public class PoemaController {

    public record DadosDoTitulo(@NotBlank @Size(max = 200) String titulo) {
    }

    public record DadosDoConteudo(@NotNull @Size(max = 200_000) String conteudo) {
    }

    // --- Relatório estrutural ---------------------------------------------------
    public record MetricaDoVerso(String separacaoGramatical, String separacaoPoetica, int totalSilabas,
            String classificacao) {
    }

    public record RimaDoVerso(String palavraFinal, String fonemaTonico, String classeGramatical) {
    }

    /** linha: posição do verso no poema (1, 2, 3...); letraRima: letra no esquema, ou null se não rima. */
    public record VersoAnalisado(UUID id, int linha, String textoOriginal, String textoLimpo, MetricaDoVerso metrica,
            RimaDoVerso rima, String letraRima, List<String> avisos) {
    }

    public record EstrofeAnalisada(int ordem, String esquemaRimas, List<VersoAnalisado> versos) {
    }

    public record ParDeRima(int linhaA, int linhaB, String palavraA, String palavraB, String riqueza) {
    }

    public record PoemaDetalhado(UUID id, String titulo, UUID idAto, UUID idObra, String conteudoBruto,
            boolean motorDisponivel, List<EstrofeAnalisada> estrofes, List<ParDeRima> rimas) {
    }

    private final PoemaRepository poemas;
    private final ServicoDeAnalise analise;
    private final ClienteDoMotor motor;
    private final RimaVersoRepository paresDeRima;

    public PoemaController(PoemaRepository poemas, ServicoDeAnalise analise, ClienteDoMotor motor,
            RimaVersoRepository paresDeRima) {
        this.paresDeRima = paresDeRima;
        this.poemas = poemas;
        this.analise = analise;
        this.motor = motor;
    }

    @GetMapping("/poemas/{id}")
    public PoemaDetalhado consultar(@AuthenticationPrincipal Jwt token, @PathVariable UUID id) {
        return detalhar(poemaDoUsuario(token, id), motor.estado().isPresent());
    }

    @PutMapping("/poemas/{id}")
    public PoemaDetalhado renomear(@AuthenticationPrincipal Jwt token, @PathVariable UUID id,
            @Valid @RequestBody DadosDoTitulo dados) {
        Poema poema = poemaDoUsuario(token, id);
        poema.setTitulo(dados.titulo().trim());
        return detalhar(poema, motor.estado().isPresent());
    }

    /** Salva o texto do poema e devolve o relatório estrutural atualizado. */
    @PutMapping("/poemas/{id}/conteudo")
    public PoemaDetalhado salvarConteudo(@AuthenticationPrincipal Jwt token, @PathVariable UUID id,
            @Valid @RequestBody DadosDoConteudo dados) {
        Poema poema = poemaDoUsuario(token, id);
        boolean motorDisponivel = analise.salvarConteudo(poema, dados.conteudo());
        return detalhar(poema, motorDisponivel);
    }

    @DeleteMapping("/poemas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt token, @PathVariable UUID id) {
        Poema poema = poemaDoUsuario(token, id);
        poema.getAto().getPoemas().remove(poema);
    }

    private Poema poemaDoUsuario(Jwt token, UUID id) {
        return poemas.findByIdAndAtoObraUsuarioId(id, ServicoDeToken.idDoUsuario(token))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Poema"));
    }

    private PoemaDetalhado detalhar(Poema poema, boolean motorDisponivel) {
        // Numera os versos na ordem do poema e atribui as letras do esquema de rimas:
        // versos com a mesma terminação recebem a mesma letra; quem não rima fica sem letra.
        Map<UUID, Integer> linhas = new HashMap<>();
        Map<String, Integer> ocorrencias = new HashMap<>();
        int linha = 0;
        for (Estrofe estrofe : poema.getEstrofes()) {
            for (Verso verso : estrofe.getVersos()) {
                linhas.put(verso.getId(), ++linha);
                if (verso.getAnaliseRima() != null) {
                    ocorrencias.merge(verso.getAnaliseRima().getFonemaTonico(), 1, Integer::sum);
                }
            }
        }
        Map<String, String> letras = new HashMap<>();
        List<EstrofeAnalisada> estrofes = new ArrayList<>();
        for (Estrofe estrofe : poema.getEstrofes()) {
            List<VersoAnalisado> versos = new ArrayList<>();
            StringBuilder esquema = new StringBuilder();
            for (Verso verso : estrofe.getVersos()) {
                AnaliseMetrica m = verso.getAnaliseMetrica();
                AnaliseRima r = verso.getAnaliseRima();
                String letra = null;
                if (r != null && !r.getFonemaTonico().isBlank() && ocorrencias.get(r.getFonemaTonico()) > 1) {
                    letra = letras.computeIfAbsent(r.getFonemaTonico(), t -> letraDoEsquema(letras.size()));
                }
                esquema.append(letra == null ? "-" : letra);
                versos.add(new VersoAnalisado(verso.getId(), linhas.get(verso.getId()), verso.getTextoOriginal(),
                        verso.getTextoLimpo(),
                        m == null ? null : new MetricaDoVerso(m.getSeparacaoGramatical(), m.getSeparacaoPoetica(),
                                m.getTotalSilabas(), m.getClassificacao()),
                        r == null ? null : new RimaDoVerso(r.getPalavraFinal(), r.getFonemaTonico(),
                                r.getClasseGramatical()),
                        letra, verso.getAvisos()));
            }
            estrofes.add(new EstrofeAnalisada(estrofe.getOrdem(), esquema.toString(), versos));
        }
        List<ParDeRima> rimas = paresDeRima.findByPoemaId(poema.getId()).stream()
                .map(par -> new ParDeRima(linhas.get(par.getVersoA().getId()), linhas.get(par.getVersoB().getId()),
                        par.getVersoA().getAnaliseRima().getPalavraFinal(),
                        par.getVersoB().getAnaliseRima().getPalavraFinal(), par.getClassificacaoRiqueza()))
                .sorted((a, b) -> a.linhaA() != b.linhaA() ? Integer.compare(a.linhaA(), b.linhaA())
                        : Integer.compare(a.linhaB(), b.linhaB()))
                .toList();
        return new PoemaDetalhado(poema.getId(), poema.getTitulo(), poema.getAto().getId(),
                poema.getAto().getObra().getId(), poema.getConteudoBruto(), motorDisponivel, estrofes, rimas);
    }

    /** A, B, ..., Z, AA, AB, ... */
    static String letraDoEsquema(int indice) {
        String letra = String.valueOf((char) ('A' + indice % 26));
        return indice < 26 ? letra : letraDoEsquema(indice / 26 - 1) + letra;
    }
}
