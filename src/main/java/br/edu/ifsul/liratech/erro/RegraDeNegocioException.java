package br.edu.ifsul.liratech.erro;

import org.springframework.http.HttpStatus;

/** Requisição bem formada, mas que viola uma regra do sistema. */
public class RegraDeNegocioException extends RuntimeException {

    private final HttpStatus status;

    public RegraDeNegocioException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
