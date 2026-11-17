package br.edu.ifsul.liratech.usuario;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifsul.liratech.erro.RecursoNaoEncontradoException;
import br.edu.ifsul.liratech.erro.RegraDeNegocioException;
import br.edu.ifsul.liratech.seguranca.ServicoDeToken;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Caso de uso "Gerenciar Conta": consultar e alterar os dados, trocar a senha e excluir a conta. */
@RestController
@RequestMapping("/conta")
@Transactional
public class ContaController {

    public record DadosDaConta(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 254) String email) {
    }

    public record TrocaDeSenha(
            @NotBlank String senhaAtual,
            @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter de 8 a 72 caracteres") String novaSenha) {
    }

    /** A exclusão pede a senha de novo, para evitar que um token esquecido apague a conta. */
    public record ConfirmacaoDeSenha(@NotBlank String senha) {
    }

    public record Conta(String nome, String email, Instant dataCadastro) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public ContaController(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @GetMapping
    public Conta consultar(@AuthenticationPrincipal Jwt token) {
        return conta(usuario(token));
    }

    @PutMapping
    public Conta alterar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody DadosDaConta dados) {
        Usuario usuario = usuario(token);
        String email = dados.email().trim().toLowerCase();
        if (!email.equals(usuario.getEmail()) && usuarios.existsByEmail(email)) {
            throw new RegraDeNegocioException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
        }
        usuario.alterarDados(dados.nome().trim(), email);
        return conta(usuario);
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@AuthenticationPrincipal Jwt token, @Valid @RequestBody TrocaDeSenha dados) {
        Usuario usuario = usuario(token);
        conferirSenha(usuario, dados.senhaAtual());
        usuario.alterarSenhaHash(codificador.encode(dados.novaSenha()));
    }

    /**
     * Exclui o usuário com um único comando DELETE. As obras, os atos, os poemas,
     * as estrofes, os versos, as análises e os pares de rima são removidos pelo
     * próprio PostgreSQL, por meio das chaves estrangeiras com ON DELETE CASCADE.
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt token, @Valid @RequestBody ConfirmacaoDeSenha dados) {
        Usuario usuario = usuario(token);
        conferirSenha(usuario, dados.senha());
        usuarios.excluirPorId(usuario.getId());
    }

    private Usuario usuario(Jwt token) {
        UUID id = ServicoDeToken.idDoUsuario(token);
        return usuarios.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário"));
    }

    private void conferirSenha(Usuario usuario, String senha) {
        if (!codificador.matches(senha, usuario.getSenhaHash())) {
            // 403, e não 401: o token é válido; o que está errado é a senha digitada.
            throw new RegraDeNegocioException(HttpStatus.FORBIDDEN, "Senha incorreta.");
        }
    }

    private Conta conta(Usuario usuario) {
        return new Conta(usuario.getNome(), usuario.getEmail(), usuario.getDataCadastro());
    }
}
