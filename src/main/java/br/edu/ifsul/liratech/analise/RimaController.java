package br.edu.ifsul.liratech.analise;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifsul.liratech.analise.ClienteDoMotor.MotorIndisponivelException;
import br.edu.ifsul.liratech.erro.RegraDeNegocioException;
import br.edu.ifsul.liratech.poema.AnaliseRimaRepository;

/** Dicionário de rimas e estado do motor de PLN. */
@RestController
public class RimaController {

    public record Sugestao(String palavra, String classeGramatical) {
    }

    public record Dicionario(String palavra, String terminacao, String classeGramatical, List<Sugestao> rimas) {
    }

    public record EstadoDoMotor(boolean disponivel, String versaoMotor, String modeloSpacy) {
    }

    private final ClienteDoMotor motor;
    private final AnaliseRimaRepository analises;

    public RimaController(ClienteDoMotor motor, AnaliseRimaRepository analises) {
        this.motor = motor;
        this.analises = analises;
    }

    /**
     * O motor extrai a terminação sonora da palavra consultada; o banco devolve
     * as palavras finais de versos já analisados que têm a mesma terminação.
     */
    @GetMapping("/rimas")
    public Dicionario consultar(@RequestParam String palavra) {
        String termo = palavra.strip();
        if (termo.isEmpty() || termo.length() > 60 || termo.contains(" ")) {
            throw new RegraDeNegocioException(HttpStatus.BAD_REQUEST, "Informe uma única palavra.");
        }
        ClienteDoMotor.Rima rima;
        try {
            rima = motor.analisar(termo).map(ClienteDoMotor.Analise::analiseRima).orElse(null);
        } catch (MotorIndisponivelException e) {
            throw new RegraDeNegocioException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
        if (rima == null) {
            throw new RegraDeNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não foi possível identificar a terminação desta palavra.");
        }
        List<Sugestao> rimas = analises.palavrasQueRimam(rima.fonemaTonico(), rima.palavraFinal()).stream()
                .map(p -> new Sugestao(p.getPalavra(), p.getClasseGramatical())).toList();
        return new Dicionario(rima.palavraFinal(), rima.fonemaTonico(), rima.classeGramatical(), rimas);
    }

    @GetMapping("/motor")
    public EstadoDoMotor estado() {
        return motor.estado().map(e -> new EstadoDoMotor(true, e.versaoMotor(), e.modeloSpacy()))
                .orElse(new EstadoDoMotor(false, null, null));
    }
}
