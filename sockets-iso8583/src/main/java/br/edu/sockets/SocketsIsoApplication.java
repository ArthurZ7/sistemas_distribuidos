package br.edu.sockets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada. O Spring Boot varre este pacote e cria os beans.
 * Quais beans existem depende da propriedade app.modo:
 *  - app.modo=servidor -> ServidorSocketMultithread e ServidorNio são iniciados
 *  - app.modo=cliente  -> ClienteTestes é executado
 */
@SpringBootApplication
public class SocketsIsoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SocketsIsoApplication.class, args);
    }
}
