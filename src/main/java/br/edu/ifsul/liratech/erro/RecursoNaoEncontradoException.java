package br.edu.ifsul.liratech.erro;

/** O recurso não existe ou pertence a outro usuário (a resposta é a mesma: 404). */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso) {
        super(recurso + " não encontrado(a).");
    }
}
