package br.edu.ifsul.liratech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Ponto de entrada da API REST do LiraTech. */
@SpringBootApplication
public class LiratechApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiratechApiApplication.class, args);
    }
}
