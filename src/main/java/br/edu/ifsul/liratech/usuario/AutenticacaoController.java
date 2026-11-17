package br.edu.ifsul.liratech.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifsul.liratech.erro.RegraDeNegocioException;
import br.edu.ifsul.liratech.seguranca.ServicoDeToken;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro e login: as duas únicas rotas que não exigem token. */
@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    public record Cadastro(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter de 8 a 72 caracteres") String senha) {
    }

    public record Login(@NotBlank @Email String email, @NotBlank String senha) {
    }

    public record Sessao(String token, String nome, String email) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final ServicoDeToken tokens;

    public AutenticacaoController(UsuarioRepository usuarios, PasswordEncoder codificador, ServicoDeToken tokens) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.tokens = tokens;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public Sessao cadastrar(@Valid @RequestBody Cadastro dados) {
        String email = dados.email().trim().toLowerCase();
        if (usuarios.existsByEmail(email)) {
            throw new RegraDeNegocioException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
        }
        Usuario usuario = usuarios.save(new Usuario(dados.nome().trim(), email, codificador.encode(dados.senha())));
        return sessao(usuario);
    }

    @PostMapping("/login")
    public Sessao entrar(@Valid @RequestBody Login dados) {
        // A mesma mensagem para e-mail inexistente e senha errada: não revela quais contas existem.
        Usuario usuario = usuarios.findByEmail(dados.email().trim().toLowerCase())
                .filter(u -> codificador.matches(dados.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new RegraDeNegocioException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));
        return sessao(usuario);
    }

    private Sessao sessao(Usuario usuario) {
        return new Sessao(tokens.emitir(usuario.getId()), usuario.getNome(), usuario.getEmail());
    }
}
