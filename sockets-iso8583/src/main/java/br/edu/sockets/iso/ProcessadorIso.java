package br.edu.sockets.iso;

import br.edu.sockets.model.Transacao;
import br.edu.sockets.service.AutorizacaoService;
import org.springframework.stereotype.Component;

/**
 * Camada compartilhada pelos DOIS servidores: recebe os bytes da requisição
 * e devolve os bytes da resposta. Assim a regra de negócio fica em um só lugar
 * e os servidores cuidam apenas de rede (sockets).
 */
@Component
public class ProcessadorIso {

    private final AutorizacaoService autorizacaoService;

    // Injeção de dependência pelo construtor (o Spring fornece o service)
    public ProcessadorIso(AutorizacaoService autorizacaoService) {
        this.autorizacaoService = autorizacaoService;
    }

    public byte[] processar(byte[] requisicao) {
        try {
            Iso8583Message req = Iso8583Message.parseRequisicao(requisicao);
            Transacao t = autorizacaoService.autorizar(req);
            return Iso8583Message.respostaPara(req, t.getCodigoResposta(), t.getNsu()).toBytes();
        } catch (IllegalArgumentException e) {
            // Mensagem malformada: responde 05 com NSU zerado
            return Iso8583Message.respostaInvalida().toBytes();
        }
    }
}
