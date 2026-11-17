package br.edu.ifsul.liratech.seguranca;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

/** Emite os tokens JWT e lê, de um token já validado, quem é o usuário. */
@Service
public class ServicoDeToken {

    private final JwtEncoder emissor;
    private final Duration validade;

    public ServicoDeToken(JwtEncoder emissor, @Value("${liratech.jwt.validade-horas}") long horas) {
        this.emissor = emissor;
        this.validade = Duration.ofHours(horas);
    }

    /** O "assunto" (subject) do token é o identificador do usuário. */
    public String emitir(UUID idUsuario) {
        Instant agora = Instant.now();
        JwtClaimsSet dados = JwtClaimsSet.builder()
                .issuer("liratech")
                .subject(idUsuario.toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return emissor.encode(JwtEncoderParameters.from(cabecalho, dados)).getTokenValue();
    }

    public static UUID idDoUsuario(Jwt token) {
        return UUID.fromString(token.getSubject());
    }
}
