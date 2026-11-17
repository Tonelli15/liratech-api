package br.edu.ifsul.liratech;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ifsul.liratech.analise.ClienteDoMotor;
import br.edu.ifsul.liratech.analise.ClienteDoMotor.Analise;
import br.edu.ifsul.liratech.analise.ClienteDoMotor.Estado;
import br.edu.ifsul.liratech.analise.ClienteDoMotor.Metrica;
import br.edu.ifsul.liratech.analise.ClienteDoMotor.MotorIndisponivelException;
import br.edu.ifsul.liratech.analise.ClienteDoMotor.Rima;

/**
 * Percorre a API de ponta a ponta com um banco em memória. O motor de PLN é
 * substituído por um dublê, para que o teste não dependa do serviço Python.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FluxoCompletoTest {

    @Autowired
    MockMvc api;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate banco;

    @MockitoBean
    ClienteDoMotor motor;

    @BeforeEach
    void motorNoAr() {
        when(motor.estado()).thenReturn(Optional.of(new Estado("ok", "1.0.0", "pt_core_news_sm")));
        // Dublê: a terminação são as duas últimas letras; três palavras contam como verbo.
        when(motor.analisar(anyString())).thenAnswer(chamada -> {
            String verso = chamada.getArgument(0, String.class);
            String palavra = verso.substring(verso.lastIndexOf(' ') + 1).toLowerCase();
            String terminacao = palavra.substring(Math.max(0, palavra.length() - 2));
            String classe = List.of("amar", "cantar", "sonhar").contains(palavra) ? "verbo" : "substantivo";
            return Optional.of(new Analise(verso.toLowerCase(),
                    new Metrica("x", "x", 10, "decassílabo"), new Rima(palavra, terminacao, classe),
                    List.of(), "1.0.0", "pt_core_news_sm"));
        });
    }

    @Test
    void semTokenARespostaE401() throws Exception {
        api.perform(get("/obras")).andExpect(status().isUnauthorized());
        api.perform(get("/rimas").param("palavra", "amor")).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroLoginEValidacao() throws Exception {
        api.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("nome", "Ana", "email", "ana@exemplo.com", "senha", "curta"))))
                .andExpect(status().isBadRequest());
        cadastrar("ana@exemplo.com");
        api.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("nome", "Ana", "email", "ana@exemplo.com", "senha", "senha-segura"))))
                .andExpect(status().isConflict());
        api.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("email", "ana@exemplo.com", "senha", "senha-errada"))))
                .andExpect(status().isUnauthorized());
        api.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("email", "ana@exemplo.com", "senha", "senha-segura"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void analisaOPoemaEReaproveitaOsVersosInalterados() throws Exception {
        String token = cadastrar("poeta@exemplo.com");
        String idPoema = criarPoema(token);

        // Duas estrofes; "amar" e "cantar" são verbos, "mar" e "luar" não: há rimas pobres e ricas.
        String texto = "quero amar\nolho o mar\n\nvou cantar\nsob o luar";
        api.perform(autenticado(put("/poemas/" + idPoema + "/conteudo"), token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("conteudo", texto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motorDisponivel").value(true))
                .andExpect(jsonPath("$.estrofes", hasSize(2)))
                .andExpect(jsonPath("$.estrofes[0].esquemaRimas").value("AA"))
                .andExpect(jsonPath("$.estrofes[1].esquemaRimas").value("AA"))
                .andExpect(jsonPath("$.estrofes[1].versos[1].linha").value(4))
                .andExpect(jsonPath("$.estrofes[0].versos[0].metrica.totalSilabas").value(10))
                .andExpect(jsonPath("$.rimas", hasSize(6)))
                .andExpect(jsonPath("$.rimas[0].riqueza").value("rica"))    // amar (verbo) x mar
                .andExpect(jsonPath("$.rimas[1].riqueza").value("pobre"));  // amar x cantar (verbos)
        verify(motor, times(4)).analisar(anyString());

        // Altera só o segundo verso: o motor deve ser chamado uma única vez.
        clearInvocations(motor);
        api.perform(autenticado(put("/poemas/" + idPoema + "/conteudo"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("conteudo", texto.replace("olho o mar", "vejo a flor")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estrofes[0].esquemaRimas").value("A-"))
                .andExpect(jsonPath("$.rimas", hasSize(3)));
        verify(motor, times(1)).analisar(anyString());

        // Dicionário de rimas: devolve as palavras finais com a mesma terminação.
        api.perform(autenticado(get("/rimas"), token).param("palavra", "sonhar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terminacao").value("ar"))
                .andExpect(jsonPath("$.rimas", hasSize(3)));

        // O poema de um usuário não é visível para outro.
        String outro = cadastrar("outro@exemplo.com");
        api.perform(autenticado(get("/poemas/" + idPoema), outro)).andExpect(status().isNotFound());

        // Excluir o poema remove versos, análises e pares de rima.
        api.perform(autenticado(delete("/poemas/" + idPoema), token)).andExpect(status().isNoContent());
        api.perform(autenticado(get("/poemas/" + idPoema), token)).andExpect(status().isNotFound());
    }

    @Test
    void comOMotorForaDoArOPoemaESalvoSemAnalise() throws Exception {
        String token = cadastrar("offline@exemplo.com");
        String idPoema = criarPoema(token);
        when(motor.estado()).thenReturn(Optional.empty());
        when(motor.analisar(anyString())).thenThrow(new MotorIndisponivelException(null));

        api.perform(autenticado(put("/poemas/" + idPoema + "/conteudo"), token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("conteudo", "um verso qualquer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motorDisponivel").value(false))
                .andExpect(jsonPath("$.conteudoBruto").value("um verso qualquer"))
                .andExpect(jsonPath("$.estrofes[0].versos[0].metrica").doesNotExist())
                .andExpect(jsonPath("$.estrofes[0].versos[0].avisos", hasSize(1)));
    }

    @Test
    void gerenciaAContaEAExclusaoApagaTudoEmCascata() throws Exception {
        String token = cadastrar("conta@exemplo.com");
        cadastrar("ocupado@exemplo.com");

        // Alterar nome e e-mail; e-mail de outra conta é recusado.
        api.perform(autenticado(put("/conta"), token).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("nome", "Novo Nome", "email", "novo@exemplo.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Nome"))
                .andExpect(jsonPath("$.email").value("novo@exemplo.com"));
        api.perform(autenticado(put("/conta"), token).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("nome", "Novo Nome", "email", "ocupado@exemplo.com"))))
                .andExpect(status().isConflict());

        // Trocar a senha exige a senha atual.
        api.perform(autenticado(put("/conta/senha"), token).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("senhaAtual", "senha-errada", "novaSenha", "outra-senha-segura"))))
                .andExpect(status().isForbidden());
        api.perform(autenticado(put("/conta/senha"), token).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("senhaAtual", "senha-segura", "novaSenha", "outra-senha-segura"))))
                .andExpect(status().isNoContent());
        api.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("email", "novo@exemplo.com", "senha", "outra-senha-segura"))))
                .andExpect(status().isOk());

        // Cria uma obra com um poema analisado: 2 versos que rimam.
        Map<String, Integer> antes = contagens();
        String idPoema = criarPoema(token);
        api.perform(autenticado(put("/poemas/" + idPoema + "/conteudo"), token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("conteudo", "quero amar\nvou cantar"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rimas", hasSize(1)));

        // Excluir a conta pede a senha; depois disso nada da conta resta no banco.
        api.perform(autenticado(delete("/conta"), token).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("senha", "senha-errada"))))
                .andExpect(status().isForbidden());
        api.perform(autenticado(delete("/conta"), token).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("senha", "outra-senha-segura"))))
                .andExpect(status().isNoContent());
        assertEquals(antes, contagens());
        api.perform(autenticado(get("/obras"), token)).andExpect(status().isUnauthorized()); // token da conta excluída
        api.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("email", "novo@exemplo.com", "senha", "outra-senha-segura"))))
                .andExpect(status().isUnauthorized());
    }

    // --- Apoio -------------------------------------------------------------------
    /** Quantidade de linhas de cada tabela da hierarquia. */
    private Map<String, Integer> contagens() {
        Map<String, Integer> linhas = new LinkedHashMap<>();
        for (String tabela : List.of("obra", "ato", "poema", "estrofe", "verso", "analise_metrica", "analise_rima",
                "rima_verso")) {
            linhas.put(tabela, banco.queryForObject("select count(*) from " + tabela, Integer.class));
        }
        return linhas;
    }

    private String cadastrar(String email) throws Exception {
        String resposta = api.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("nome", "Pessoa", "email", email, "senha", "senha-segura"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get("token").asText();
    }

    private String criarPoema(String token) throws Exception {
        String obra = api.perform(autenticado(post("/obras"), token).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(Map.of("titulo", "Minha obra", "sinopse", "Teste"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String ato = api.perform(autenticado(post("/obras/" + json.readTree(obra).get("id").asText() + "/atos"), token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("titulo", "Ato I"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String poema = api.perform(autenticado(post("/atos/" + json.readTree(ato).get("id").asText() + "/poemas"), token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo(Map.of("titulo", "Poema"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        api.perform(autenticado(get("/obras"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].poemas").value(1));
        return json.readTree(poema).get("id").asText();
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder requisicao, String token) {
        return requisicao.header("Authorization", "Bearer " + token);
    }

    private String corpo(Map<String, String> dados) throws Exception {
        return json.writeValueAsString(dados);
    }
}
