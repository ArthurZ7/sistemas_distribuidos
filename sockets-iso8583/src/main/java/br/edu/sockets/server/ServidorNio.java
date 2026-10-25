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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/**
 * Servidor 2: Java NIO (I/O NÃO bloqueante).
 * UMA única thread com um Selector atende TODAS as conexões: o Selector avisa
 * quais canais estão prontos para aceitar / ler / escrever.
 */
@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "servidor")
public class ServidorNio {

    private static final Logger log = LoggerFactory.getLogger(ServidorNio.class);

    /** Estado de cada conexão, guardado como "attachment" da SelectionKey. */
    private static class Conexao {
        // Acumula bytes recebidos: o TCP é um fluxo, uma leitura pode trazer
        // meia mensagem ou várias mensagens juntas.
        final ByteBuffer entrada = ByteBuffer.allocate(1024);
        // Respostas aguardando envio (a escrita também pode ser parcial)
        final Queue<ByteBuffer> saida = new ArrayDeque<>();
    }

    private final ProcessadorIso processador;
    private final int porta;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean ativo;

    public ServidorNio(ProcessadorIso processador, @Value("${servidor.nio.porta}") int porta) {
        this.processador = processador;
        this.porta = porta;
    }

    @PostConstruct
    public void iniciar() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(porta));
        serverChannel.configureBlocking(false);                    // obrigatório para usar Selector
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);  // interesse: novas conexões
        ativo = true;
        new Thread(this::loopSelector, "nio-selector").start();
        log.info("[NIO] Servidor escutando na porta {}", porta);
    }

    /** Event loop: espera eventos e despacha cada um para o tratador adequado. */
    private void loopSelector() {
        while (ativo) {
            try {
                selector.select();                                 // bloqueia até haver algum evento
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();                                   // precisa remover manualmente
                    try {
                        if (key.isValid() && key.isAcceptable()) aceitar();
                        if (key.isValid() && key.isReadable())   ler(key);
                        if (key.isValid() && key.isWritable())   escrever(key);
                    } catch (IOException | CancelledKeyException e) {
                        fechar(key);
                    }
                }
            } catch (IOException e) {
                if (ativo) log.error("[NIO] Erro no selector", e);
            }
        }
    }

    /** Nova conexão: registra o canal no Selector com interesse em LEITURA. */
    private void aceitar() throws IOException {
        SocketChannel canal = serverChannel.accept();
        if (canal == null) return;
        canal.configureBlocking(false);
        canal.register(selector, SelectionKey.OP_READ, new Conexao());
        log.info("[NIO] Cliente conectado: {}", canal.getRemoteAddress());
    }

    /** Dados disponíveis: lê, extrai mensagens completas (45 bytes) e enfileira as respostas. */
    private void ler(SelectionKey key) throws IOException {
        SocketChannel canal = (SocketChannel) key.channel();
        Conexao c = (Conexao) key.attachment();

        int lidos = canal.read(c.entrada);
        if (lidos == -1) {                                         // cliente fechou a conexão
            fechar(key);
            return;
        }

        c.entrada.flip();                                          // modo leitura do buffer
        while (c.entrada.remaining() >= Iso8583Message.TAMANHO_REQUISICAO) {
            byte[] requisicao = new byte[Iso8583Message.TAMANHO_REQUISICAO];
            c.entrada.get(requisicao);
            byte[] resposta = processador.processar(requisicao);
            c.saida.add(ByteBuffer.wrap(resposta));
            log.info("[NIO] resposta {}", new String(resposta));
        }
        c.entrada.compact();                                       // guarda sobras (mensagem incompleta)

        if (!c.saida.isEmpty()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE); // quero escrever
        }
    }

    /** Canal pronto para escrita: envia o que estiver na fila. */
    private void escrever(SelectionKey key) throws IOException {
        SocketChannel canal = (SocketChannel) key.channel();
        Conexao c = (Conexao) key.attachment();

        while (!c.saida.isEmpty()) {
            ByteBuffer buf = c.saida.peek();
            canal.write(buf);
            if (buf.hasRemaining()) return;                        // buffer de rede cheio: tenta depois
            c.saida.poll();
        }
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE); // nada mais a escrever
    }

    private void fechar(SelectionKey key) {
        try {
            key.cancel();
            key.channel().close();
        } catch (IOException ignored) { }
    }

    @PreDestroy
    public void parar() throws IOException {
        ativo = false;
        selector.wakeup();                                         // acorda o select() para sair do laço
        serverChannel.close();
        selector.close();
    }
}
