package br.edu.sockets.client;

import br.edu.sockets.iso.Iso8583Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Cliente de testes. Só é criado com app.modo=cliente.
 * CommandLineRunner: o Spring chama run() assim que a aplicação sobe.
 * Executa os mesmos cenários contra os DOIS servidores.
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "cliente")
public class ClienteTestes implements CommandLineRunner {

    private final String host;
    private final int portaMultithread;
    private final int portaNio;

    public ClienteTestes(@Value("${cliente.host}") String host,
                         @Value("${servidor.multithread.porta}") int portaMultithread,
                         @Value("${servidor.nio.porta}") int portaNio) {
        this.host = host;
        this.portaMultithread = portaMultithread;
        this.portaNio = portaNio;
    }

    @Override
    public void run(String... args) throws Exception {
        executarCenarios("MULTITHREAD", portaMultithread, "400000000098");
        executarCenarios("NIO", portaNio, "400000000099");
    }

    private void executarCenarios(String nome, int porta, String cartaoConcorrencia) throws Exception {
        System.out.println("\n===== SERVIDOR " + nome + " (porta " + porta + ") =====");

        // Cenário 1: aprovada (R$ 20,10 num cartão com R$ 1.000,00) -> esperado 00 + NSU válido
        testar(porta, "Aprovada (débito R$ 20,10)", 2010, "401231021845", 1);

        // Cenário 2: aprovada no crédito -> esperado 00
        testar(porta, "Aprovada (crédito R$ 100,00)", 10000, "401231021845", 2);

        // Cenário 3: saldo insuficiente (R$ 50,00 num cartão com R$ 10,00) -> esperado 51 + NSU zerado
        testar(porta, "Saldo insuficiente", 5000, "401231021847", 1);

        // Cenário 4: cartão inexistente -> esperado 05 + NSU zerado
        testar(porta, "Cartão inexistente", 1000, "999999999999", 1);

        // Cenário 5: concorrência. Cartão com R$ 50,00; 10 clientes simultâneos
        // tentam R$ 10,00 cada. Se a região crítica estiver correta, EXATAMENTE 5 são aprovados.
        testarConcorrencia(porta, cartaoConcorrencia);
    }

    /** Envia uma transação e imprime a requisição e a resposta decodificada. */
    private void testar(int porta, String descricao, long valor, String cartao, int forma) throws Exception {
        String resposta = enviar(porta, montarRequisicao(valor, cartao, forma));
        System.out.printf("%-30s -> resposta=%s codigo=%s NSU=%s%n",
                descricao, resposta, codigo(resposta), nsu(resposta));
    }

    private void testarConcorrencia(int porta, String cartao) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Future<String>> futuros = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            // Cada tarefa abre sua própria conexão e envia R$ 10,00
            futuros.add(pool.submit(() -> enviar(porta, montarRequisicao(1000, cartao, 1))));
        }
        int aprovadas = 0, recusadas = 0;
        for (Future<String> f : futuros) {
            if (codigo(f.get()).equals("00")) aprovadas++; else recusadas++;
        }
        pool.shutdown();
        System.out.printf("Concorrência (10 x R$10 em saldo R$50): aprovadas=%d (esperado 5), recusadas=%d%n",
                aprovadas, recusadas);
    }

    /** Monta a requisição 0200 de 45 bytes (mesmo layout do enunciado). */
    private byte[] montarRequisicao(long valorCentavos, String cartao, int forma) {
        LocalDateTime agora = LocalDateTime.now();
        String msg = Iso8583Message.MTI_REQUISICAO                       // MTI
                + String.format("%012d", valorCentavos)                  // bit 4
                + agora.format(DateTimeFormatter.ofPattern("HHmmss"))    // bit 12
                + agora.format(DateTimeFormatter.ofPattern("ddMM"))      // bit 13
                + "040104"                                               // bit 33
                + cartao                                                 // bit 62 (cartão)
                + forma;                                                 // bit 62 (1 débito, 2 crédito)
        return msg.getBytes(StandardCharsets.US_ASCII);
    }

    /** Abre conexão, envia a requisição e lê exatamente 46 bytes de resposta. */
    private String enviar(int porta, byte[] requisicao) throws Exception {
        try (Socket socket = new Socket(host, porta)) {
            OutputStream out = socket.getOutputStream();
            out.write(requisicao);
            out.flush();
            byte[] resposta = new byte[Iso8583Message.TAMANHO_RESPOSTA];
            new DataInputStream(socket.getInputStream()).readFully(resposta);
            return new String(resposta, StandardCharsets.US_ASCII);
        }
    }

    private String codigo(String resposta) { return resposta.substring(32, 34); }  // bit 39
    private String nsu(String resposta) { return resposta.substring(34, 46); }     // bit 127
}
