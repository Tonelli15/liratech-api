package br.edu.ifsul.liratech.analise;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chamadas HTTP ao microsserviço de PLN (rotas /analises e /saude). */
@Component
public class ClienteDoMotor {

    // Espelham o contrato JSON do motor (Quadro "Contrato de dados da resposta do motor de PLN").
    public record Metrica(String separacaoGramatical, String separacaoPoetica, int totalSilabas,
            String classificacao) {
    }

    public record Rima(String palavraFinal, String fonemaTonico, String classeGramatical) {
    }

    public record Analise(String textoLimpo, Metrica analiseMetrica, Rima analiseRima, List<String> avisos,
            String versaoMotor, String modeloSpacy) {
    }

    public record Estado(String status, String versaoMotor, String modeloSpacy) {
    }

    /** O motor não respondeu (fora do ar, lento demais ou com erro interno). */
    public static class MotorIndisponivelException extends RuntimeException {
        public MotorIndisponivelException(Throwable causa) {
            super("O motor de PLN está indisponível.", causa);
        }
    }

    private final RestClient http;

    public ClienteDoMotor(@Value("${liratech.motor.url}") String url,
            @Value("${liratech.motor.tempo-limite-ms}") int tempoLimiteMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(tempoLimiteMs));
        fabrica.setReadTimeout(Duration.ofMillis(tempoLimiteMs));
        this.http = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    /**
     * Analisa um verso. Devolve vazio quando o motor recusa o texto (422: o
     * verso não contém palavras) e lança MotorIndisponivelException nas falhas.
     */
    public Optional<Analise> analisar(String verso) {
        try {
            return Optional.ofNullable(http.post().uri("/analises").body(Map.of("verso", verso)).retrieve()
                    .body(Analise.class));
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new MotorIndisponivelException(e);
        }
    }

    /** Estado do motor, ou vazio se ele não respondeu. */
    public Optional<Estado> estado() {
        try {
            return Optional.ofNullable(http.get().uri("/saude").retrieve().body(Estado.class));
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }
}
