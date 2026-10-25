package br.edu.sockets.model;

/**
 * Transação financeira (classe "Transacao" do diagrama).
 * Cada transação autorizada/recusada de um cartão fica registrada em memória.
 */
public class Transacao {
    private final long valor;            // valor em centavos (bit 4)
    private final String data;           // ddMM (bit 13)
    private final String hora;           // HHmmss (bit 12)
    private final String redeTransmissora; // bit 33
    private final int formaPagamento;    // 1 = débito, 2 = crédito
    private String nsu;                  // bit 127 (zeros quando recusada)
    private String codigoResposta;       // bit 39 (00, 05, 51...)

    public Transacao(long valor, String data, String hora, String redeTransmissora, int formaPagamento) {
        this.valor = valor;
        this.data = data;
        this.hora = hora;
        this.redeTransmissora = redeTransmissora;
        this.formaPagamento = formaPagamento;
    }

    public long getValor() { return valor; }
    public String getData() { return data; }
    public String getHora() { return hora; }
    public String getRedeTransmissora() { return redeTransmissora; }
    public int getFormaPagamento() { return formaPagamento; }
    public String getNsu() { return nsu; }
    public void setNsu(String nsu) { this.nsu = nsu; }
    public String getCodigoResposta() { return codigoResposta; }
    public void setCodigoResposta(String codigoResposta) { this.codigoResposta = codigoResposta; }
}
