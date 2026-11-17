package br.edu.ifsul.liratech.seguranca;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import br.edu.ifsul.liratech.usuario.UsuarioRepository;

/**
 * Regras de acesso da API (RNF04): tudo exige um token JWT válido, exceto o
 * cadastro e o login. Sem token, ou com token expirado, a resposta é 401 e
 * nenhuma rota de negócio (nem o motor de PLN) chega a ser acionada.
 */
@Configuration
public class ConfiguracaoDeSeguranca {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                // A API não usa cookies nem sessão: cada requisição traz o seu token.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/auth/cadastro", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                // Valida o cabeçalho "Authorization: Bearer <token>" em cada requisição.
                .oauth2ResourceServer(servidor -> servidor.jwt(Customizer.withDefaults()))
                .build();
    }

    /** Chave simétrica (HMAC-SHA256) usada para assinar e para conferir os tokens. */
    @Bean
    SecretKey chaveJwt(@Value("${liratech.jwt.segredo}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("liratech.jwt.segredo precisa de pelo menos 32 caracteres.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder emissorDeTokens(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    /**
     * Além da assinatura e da validade, confere se a conta do token ainda existe:
     * o token de uma conta excluída passa a receber 401, como um token expirado.
     */
    @Bean
    JwtDecoder validadorDeTokens(SecretKey chaveJwt, UsuarioRepository usuarios) {
        NimbusJwtDecoder validador = NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> contaExiste = token -> {
            try {
                if (usuarios.existsById(UUID.fromString(token.getSubject()))) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                // assunto ausente ou fora do formato UUID: cai na falha abaixo
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "A conta deste token não existe mais.", null));
        };
        validador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), contaExiste));
        return validador;
    }

    /** As senhas são guardadas apenas como hash BCrypt, nunca em texto puro. */
    @Bean
    PasswordEncoder codificadorDeSenhas() {
        return new BCryptPasswordEncoder();
    }

    /** Autoriza o navegador a chamar a API a partir do endereço do frontend. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${liratech.frontend.origem}") String origem) {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(List.of(origem.split(",")));
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);
        return fonte;
    }
}
