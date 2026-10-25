package br.edu.sockets.iso;

import java.nio.charset.StandardCharsets;

/**
 * Representa uma mensagem ISO 8583 no layout do enunciado (campos de tamanho FIXO, em ASCII).
 *
 * REQUISIÇÃO 0200 (45 bytes)             RESPOSTA 0210 (46 bytes)
 *  MTI .............. 4  (0-3)           MTI .............. 4  (0-3)
 *  bit 4  valor ..... 12 (4-15)           bit 4  valor ..... 12 (4-15)
 *  bit 12 hora ...... 6  (16-21)          bit 12 hora ...... 6  (16-21)
 *  bit 13 data ...... 4  (22-25)          bit 13 data ...... 4  (22-25)
 *  bit 33 rede ...... 6  (26-31)          bit 33 rede ...... 6  (26-31)
 *  bit 62 cartão .... 12 (32-43)          bit 39 resposta .. 2  (32-33)
 *  bit 62 pagamento . 1  (44)             bit 127 NSU ...... 12 (34-45)
 *
 * Como os tamanhos são fixos, o "enquadramento" (framing) no TCP é simples:
 * basta ler exatamente 45 bytes por requisição.
 */
public class Iso8583Message {

    public static final int TAMANHO_REQUISICAO = 45;
    public static final int TAMANHO_RESPOSTA = 46;
    public static final String MTI_REQUISICAO = "0200";
    public static final String MTI_RESPOSTA = "0210";

    private String mti;
    private long valor;             // centavos
    private String hora = "000000";
    private String data = "0000";
    private String rede = "000000";
    private String cartao;          // só existe na requisição
    private int formaPagamento;     // só existe na requisição
    private String codigoResposta;  // só existe na resposta
    private String nsu;             // só existe na resposta

    /** Converte os 45 bytes recebidos em objeto, validando cada campo. */
    public static Iso8583Message parseRequisicao(byte[] bytes) {
        if (bytes == null || bytes.length != TAMANHO_REQUISICAO) {
            throw new IllegalArgumentException("Tamanho inválido da requisição");
        }
        String s = new String(bytes, StandardCharsets.US_ASCII);

        Iso8583Message m = new Iso8583Message();
        m.mti = s.substring(0, 4);
        if (!MTI_REQUISICAO.equals(m.mti)) {
            throw new IllegalArgumentException("MTI não suportado: " + m.mti);
        }
        String valor = s.substring(4, 16);
        m.hora = s.substring(16, 22);
        m.data = s.substring(22, 26);
        m.rede = s.substring(26, 32);
        m.cartao = s.substring(32, 44);
        String forma = s.substring(44, 45);

        // Todos os campos numéricos precisam conter apenas dígitos
        for (String campo : new String[]{valor, m.hora, m.data, m.rede, m.cartao, forma}) {
            if (!campo.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("Campo não numérico: " + campo);
            }
        }
        m.valor = Long.parseLong(valor);
        m.formaPagamento = Integer.parseInt(forma);
        if (m.formaPagamento != 1 && m.formaPagamento != 2) {
            throw new IllegalArgumentException("Forma de pagamento inválida");
        }
        return m;
    }

    /** Monta uma resposta 0210 copiando valor/hora/data/rede da requisição. */
    public static Iso8583Message respostaPara(Iso8583Message req, String codigo, String nsu) {
        Iso8583Message r = new Iso8583Message();
        r.mti = MTI_RESPOSTA;
        r.valor = req.valor;
        r.hora = req.hora;
        r.data = req.data;
        r.rede = req.rede;
        r.codigoResposta = codigo;
        r.nsu = nsu;
        return r;
    }

    /** Resposta usada quando a mensagem é inválida: código 05 e campos zerados. */
    public static Iso8583Message respostaInvalida() {
        Iso8583Message r = new Iso8583Message();
        r.mti = MTI_RESPOSTA;
        r.codigoResposta = "05";
        r.nsu = "000000000000";
        return r;
    }

    /** Serializa a resposta em 46 bytes ASCII. */
    public byte[] toBytes() {
        String s = mti
                + String.format("%012d", valor)
                + hora + data + rede
                + codigoResposta
                + nsu;
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    public long getValor() { return valor; }
    public String getHora() { return hora; }
    public String getData() { return data; }
    public String getRede() { return rede; }
    public String getCartao() { return cartao; }
    public int getFormaPagamento() { return formaPagamento; }
}
