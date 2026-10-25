import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class Cliente {
    public static void main(String[] args) throws Exception {
        try (Socket conexao = new Socket("localhost", 12222)) {
            DataInputStream entrada = new DataInputStream(conexao.getInputStream());
            DataOutputStream saida = new DataOutputStream(conexao.getOutputStream());
            saida.writeUTF("Olá Servidor");
            String resposta = entrada.readUTF();
            saida.writeUTF("Servidor recebeu: " + resposta);
        }
    }
}
