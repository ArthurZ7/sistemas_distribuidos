package br.edu.sockets.server;

import br.edu.sockets.iso.Iso8583Message;
import br.edu.sockets.iso.ProcessadorIso;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor 1: ServerSocket + UMA THREAD POR CONEXÃO (I/O bloqueante).
 * Só é criado quando app.modo=servidor.
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "servidor")
public class ServidorSocketMultithread {

    private static final Logger log = LoggerFactory.getLogger(ServidorSocketMultithread.class);

    private final ProcessadorIso processador;
    private final int porta;
    private ServerSocket serverSocket;
    private volatile boolean ativo;

    public ServidorSocketMultithread(ProcessadorIso processador,
                                     @Value("${servidor.multithread.porta}") int porta) {
        this.processador = processador;
        this.porta = porta;
    }

    /** Executado pelo Spring após criar o bean: abre a porta e inicia a thread de aceitação. */
    @PostConstruct
    public void iniciar() throws IOException {
        serverSocket = new ServerSocket(porta);
        ativo = true;
        // Thread separada para não bloquear a inicialização do Spring (accept() é bloqueante)
        new Thread(this::aceitarConexoes, "mt-accept").start();
        log.info("[MULTITHREAD] Servidor escutando na porta {}", porta);
    }

    /** Laço principal: aceita conexões e cria uma nova thread para cada cliente. */
    private void aceitarConexoes() {
        while (ativo) {
            try {
                Socket cliente = serverSocket.accept();           // bloqueia até alguém conectar
                new Thread(() -> atender(cliente), "mt-cliente-" + cliente.getPort()).start();
            } catch (IOException e) {
                if (ativo) log.error("[MULTITHREAD] Erro no accept", e);
            }
        }
    }

    /** Atende UM cliente (roda na thread dedicada a ele). */
    private void atender(Socket socket) {
        // try-with-resources fecha o socket ao terminar
        try (socket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             OutputStream out = socket.getOutputStream()) {

            byte[] requisicao = new byte[Iso8583Message.TAMANHO_REQUISICAO];
            while (true) {
                in.readFully(requisicao);                          // lê exatamente 45 bytes
                byte[] resposta = processador.processar(requisicao);
                out.write(resposta);
                out.flush();
                log.info("[MULTITHREAD] {} -> resposta {}", Thread.currentThread().getName(),
                        new String(resposta));
            }
        } catch (EOFException e) {
            // Cliente encerrou a conexão normalmente
        } catch (IOException e) {
            log.warn("[MULTITHREAD] Conexão encerrada com erro: {}", e.getMessage());
        }
    }

    /** Chamado pelo Spring ao encerrar a aplicação. */
    @PreDestroy
    public void parar() throws IOException {
        ativo = false;
        if (serverSocket != null) serverSocket.close();
    }
}
