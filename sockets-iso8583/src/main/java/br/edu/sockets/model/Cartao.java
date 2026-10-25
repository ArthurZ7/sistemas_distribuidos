package br.edu.sockets.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Cartão (classe "Cartao" do diagrama). Um cartão possui várias transações
 * (relação 1..* / composição). O saldo é guardado em CENTAVOS (long) para
 * evitar erros de arredondamento de double.
 */
public class Cartao {
    private final String numero;       // 12 dígitos, igual ao bit 62 da mensagem
    private final String nomeCliente;
    private long saldo;                // em centavos
    private final List<Transacao> transacoes = new ArrayList<>();

    public Cartao(String numero, String nomeCliente, long saldo) {
        this.numero = numero;
        this.nomeCliente = nomeCliente;
        this.saldo = saldo;
    }

    public String getNumero() { return numero; }
    public String getNomeCliente() { return nomeCliente; }
    public long getSaldo() { return saldo; }
    public void setSaldo(long saldo) { this.saldo = saldo; }
    public List<Transacao> getTransacoes() { return transacoes; }
}
