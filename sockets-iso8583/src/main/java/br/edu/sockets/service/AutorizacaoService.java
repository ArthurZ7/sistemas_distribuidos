package br.edu.sockets.service;

import br.edu.sockets.iso.Iso8583Message;
import br.edu.sockets.model.Cartao;
import br.edu.sockets.model.Transacao;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Regras de negócio. É um SINGLETON do Spring, logo as DUAS implementações de servidor
 * (multithread e NIO) compartilham os mesmos cartões e o mesmo contador de NSU.
 * Por isso a região crítica precisa ser protegida com synchronized.
 */
@Service
public class AutorizacaoService {

    private static final String NSU_ZERADO = "000000000000";

    // "Banco de dados" em memória: número do cartão -> cartão
    private final Map<String, Cartao> cartoes = new HashMap<>();

    // Contador do NSU. Começa de modo que o 1º NSU seja 000900002363 (como no exemplo do enunciado)
    private long ultimoNsu = 900002362L;

    /** Cria alguns cartões com saldo pré-definido (valores em centavos). */
    @PostConstruct
    void carregarCartoes() {
        cartoes.put("401231021845", new Cartao("401231021845", "Joao Silva", 100000));  // R$ 1.000,00
        cartoes.put("401231021846", new Cartao("401231021846", "Maria Souza", 5000));   // R$ 50,00
        cartoes.put("401231021847", new Cartao("401231021847", "Carlos Lima", 1000));   // R$ 10,00
        // Cartões usados no teste de concorrência do cliente (um para cada servidor)
        cartoes.put("400000000098", new Cartao("400000000098", "Teste Concorrencia MT", 5000));
        cartoes.put("400000000099", new Cartao("400000000099", "Teste Concorrencia NIO", 5000));
    }

    /**
     * REGIÃO CRÍTICA (synchronized):
     * "verificar saldo -> debitar -> gerar NSU" precisa ser ATÔMICO. Sem isso, duas threads
     * poderiam ler o mesmo saldo e aprovar duas compras que juntas excedem o saldo
     * (condição de corrida), ou gerar o mesmo NSU duas vezes.
     * O synchronized no método usa o lock do próprio objeto (singleton), então vale
     * para todas as threads de ambos os servidores.
     */
    public synchronized Transacao autorizar(Iso8583Message req) {
        Transacao t = new Transacao(req.getValor(), req.getData(), req.getHora(),
                req.getRede(), req.getFormaPagamento());

        Cartao cartao = cartoes.get(req.getCartao());

        if (cartao == null) {
            // Cartão inexistente -> 05 e NSU zerado
            t.setCodigoResposta("05");
            t.setNsu(NSU_ZERADO);
        } else if (cartao.getSaldo() < req.getValor()) {
            // Saldo insuficiente -> 51 e NSU zerado
            t.setCodigoResposta("51");
            t.setNsu(NSU_ZERADO);
            cartao.getTransacoes().add(t);
        } else {
            // Aprovada -> debita saldo, gera NSU válido, código 00
            cartao.setSaldo(cartao.getSaldo() - req.getValor());
            t.setCodigoResposta("00");
            t.setNsu(gerarNsu());
            cartao.getTransacoes().add(t);
        }
        return t;
    }

    /** Gera NSU de 12 dígitos. Só é chamado dentro de autorizar() (já sincronizado). */
    private String gerarNsu() {
        ultimoNsu++;
        return String.format("%012d", ultimoNsu);
    }
}
