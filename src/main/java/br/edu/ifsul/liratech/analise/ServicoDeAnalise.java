package br.edu.ifsul.liratech.analise;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifsul.liratech.analise.ClienteDoMotor.Analise;
import br.edu.ifsul.liratech.analise.ClienteDoMotor.MotorIndisponivelException;
import br.edu.ifsul.liratech.erro.RegraDeNegocioException;
import br.edu.ifsul.liratech.poema.AnaliseMetrica;
import br.edu.ifsul.liratech.poema.AnaliseRima;
import br.edu.ifsul.liratech.poema.Estrofe;
import br.edu.ifsul.liratech.poema.Poema;
import br.edu.ifsul.liratech.poema.PoemaRepository;
import br.edu.ifsul.liratech.poema.RimaVerso;
import br.edu.ifsul.liratech.poema.RimaVersoRepository;
import br.edu.ifsul.liratech.poema.Verso;

/**
 * Orquestra a análise de um poema: divide o texto em estrofes e versos, pede
 * ao motor de PLN somente os versos novos ou alterados (RNF02), persiste o
 * resultado e identifica os pares de versos que rimam, classificando cada
 * par como rima rica ou pobre (RF04).
 */
@Service
public class ServicoDeAnalise {

    public static final int TAMANHO_MAXIMO_DO_VERSO = 500;
    public static final int MAXIMO_DE_VERSOS = 1000;
    static final String AVISO_MOTOR_INDISPONIVEL = "Motor de PLN indisponível: o verso foi salvo sem análise.";
    static final String AVISO_SEM_PALAVRAS = "O verso não contém palavras.";

    private final ClienteDoMotor motor;
    private final PoemaRepository poemas;
    private final RimaVersoRepository rimas;

    public ServicoDeAnalise(ClienteDoMotor motor, PoemaRepository poemas, RimaVersoRepository rimas) {
        this.motor = motor;
        this.poemas = poemas;
        this.rimas = rimas;
    }

    /** Devolve true se o motor respondeu durante o salvamento. */
    @Transactional
    public boolean salvarConteudo(Poema poema, String conteudo) {
        List<List<String>> estrofes = dividir(conteudo);
        validar(estrofes);

        // Versão atual do motor: análises feitas por outra versão não são reaproveitadas.
        Optional<String> versaoAtual = motor.estado().map(ClienteDoMotor.Estado::versaoMotor);
        boolean motorDisponivel = versaoAtual.isPresent();
        Map<String, Deque<Analise>> guardadas = analisesReaproveitaveis(poema, versaoAtual.orElse(null));

        // Remove a estrutura anterior. As rimas saem primeiro, pois apontam para os versos.
        rimas.excluirDoPoema(poema.getId());
        poema.getEstrofes().clear();
        poemas.flush();
        poema.setConteudoBruto(conteudo);

        List<Verso> versos = new ArrayList<>();
        for (int e = 0; e < estrofes.size(); e++) {
            Estrofe estrofe = new Estrofe(poema, e + 1);
            poema.getEstrofes().add(estrofe);
            List<String> linhas = estrofes.get(e);
            for (int v = 0; v < linhas.size(); v++) {
                Verso verso = new Verso(estrofe, linhas.get(v), v + 1);
                estrofe.getVersos().add(verso);
                versos.add(verso);

                Deque<Analise> fila = guardadas.get(verso.getTextoOriginal());
                if (fila != null && !fila.isEmpty()) {
                    aplicar(verso, fila.poll()); // texto inalterado: reaproveita a análise
                } else if (motorDisponivel) {
                    motorDisponivel = analisar(verso);
                } else {
                    verso.setAvisos(List.of(AVISO_MOTOR_INDISPONIVEL));
                }
            }
        }
        poemas.flush(); // grava os versos antes dos pares de rima, que apontam para eles
        rimas.saveAll(identificarRimas(poema, versos));
        return motorDisponivel;
    }

    /** Linhas em branco separam as estrofes; espaços nas pontas das linhas são descartados. */
    static List<List<String>> dividir(String conteudo) {
        List<List<String>> estrofes = new ArrayList<>();
        List<String> atual = new ArrayList<>();
        for (String linha : conteudo.split("\\R", -1)) {
            String verso = linha.strip();
            if (!verso.isEmpty()) {
                atual.add(verso);
            } else if (!atual.isEmpty()) {
                estrofes.add(atual);
                atual = new ArrayList<>();
            }
        }
        if (!atual.isEmpty()) {
            estrofes.add(atual);
        }
        return estrofes;
    }

    private void validar(List<List<String>> estrofes) {
        int total = 0;
        for (List<String> estrofe : estrofes) {
            for (String verso : estrofe) {
                total++;
                if (verso.length() > TAMANHO_MAXIMO_DO_VERSO) {
                    throw new RegraDeNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "O verso " + total + " tem mais de " + TAMANHO_MAXIMO_DO_VERSO + " caracteres.");
                }
            }
        }
        if (total > MAXIMO_DE_VERSOS) {
            throw new RegraDeNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O poema tem mais de " + MAXIMO_DE_VERSOS + " versos.");
        }
    }

    /** Agrupa, pelo texto do verso, as análises que continuam válidas. */
    private Map<String, Deque<Analise>> analisesReaproveitaveis(Poema poema, String versaoAtual) {
        Map<String, Deque<Analise>> guardadas = new HashMap<>();
        for (Estrofe estrofe : poema.getEstrofes()) {
            for (Verso verso : estrofe.getVersos()) {
                boolean analisado = verso.getVersaoMotor() != null;
                boolean mesmaVersao = versaoAtual == null || versaoAtual.equals(verso.getVersaoMotor());
                if (analisado && mesmaVersao) {
                    AnaliseMetrica m = verso.getAnaliseMetrica();
                    AnaliseRima r = verso.getAnaliseRima();
                    Analise analise = new Analise(verso.getTextoLimpo(),
                            m == null ? null : new ClienteDoMotor.Metrica(m.getSeparacaoGramatical(),
                                    m.getSeparacaoPoetica(), m.getTotalSilabas(), m.getClassificacao()),
                            r == null ? null : new ClienteDoMotor.Rima(r.getPalavraFinal(), r.getFonemaTonico(),
                                    r.getClasseGramatical()),
                            verso.getAvisos(), verso.getVersaoMotor(), verso.getModeloSpacy());
                    guardadas.computeIfAbsent(verso.getTextoOriginal(), texto -> new ArrayDeque<>()).add(analise);
                }
            }
        }
        return guardadas;
    }

    /** Copia para o verso os dados de uma análise (vinda do motor ou reaproveitada). */
    private void aplicar(Verso verso, Analise analise) {
        verso.setTextoLimpo(analise.textoLimpo());
        verso.setAvisos(analise.avisos() == null ? List.of() : analise.avisos());
        verso.registrarMotor(analise.versaoMotor(), analise.modeloSpacy());
        if (analise.analiseMetrica() != null) {
            var m = analise.analiseMetrica();
            verso.setAnaliseMetrica(new AnaliseMetrica(verso, m.separacaoGramatical(), m.separacaoPoetica(),
                    m.totalSilabas(), m.classificacao()));
        }
        if (analise.analiseRima() != null) {
            var r = analise.analiseRima();
            verso.setAnaliseRima(new AnaliseRima(verso, r.palavraFinal(), r.fonemaTonico(), r.classeGramatical()));
        }
    }

    /** Chama o motor para um verso; devolve false se o motor deixou de responder. */
    private boolean analisar(Verso verso) {
        Optional<Analise> resposta;
        try {
            resposta = motor.analisar(verso.getTextoOriginal());
        } catch (MotorIndisponivelException e) {
            verso.setAvisos(List.of(AVISO_MOTOR_INDISPONIVEL));
            return false;
        }
        if (resposta.isEmpty()) {
            verso.setAvisos(List.of(AVISO_SEM_PALAVRAS));
            return true;
        }
        aplicar(verso, resposta.get());
        return true;
    }

    /**
     * Dois versos rimam quando têm a mesma terminação sonora normalizada, em
     * qualquer estrofe do poema. A rima é rica quando as palavras finais são de
     * classes gramaticais diferentes e pobre quando são da mesma classe.
     */
    private List<RimaVerso> identificarRimas(Poema poema, List<Verso> versos) {
        Map<String, List<Verso>> porTerminacao = new LinkedHashMap<>();
        for (Verso verso : versos) {
            AnaliseRima rima = verso.getAnaliseRima();
            if (rima != null && !rima.getFonemaTonico().isBlank()) {
                porTerminacao.computeIfAbsent(rima.getFonemaTonico(), t -> new ArrayList<>()).add(verso);
            }
        }
        List<RimaVerso> pares = new ArrayList<>();
        for (List<Verso> grupo : porTerminacao.values()) {
            for (int i = 0; i < grupo.size(); i++) {
                for (int j = i + 1; j < grupo.size(); j++) {
                    Verso a = grupo.get(i);
                    Verso b = grupo.get(j);
                    boolean mesmaClasse = a.getAnaliseRima().getClasseGramatical()
                            .equals(b.getAnaliseRima().getClasseGramatical());
                    pares.add(new RimaVerso(poema, a, b, mesmaClasse ? RimaVerso.POBRE : RimaVerso.RICA));
                }
            }
        }
        return pares;
    }
}
