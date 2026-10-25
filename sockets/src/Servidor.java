import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {
    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(1212,50)) {
            System.out.println("Servidor ");
        
            while (true) {
                try (Socket conexao = serverSocket.accept()) {
                    System.out.println("Conexão recebida de: " + conexao.getInetAddress().getHostAddress());
        
                DataInputStream entrada = new DataInputStream(conexao.getInputStream());
                DataOutputStream saida = new DataOutputStream(conexao.getOutputStream());
        
                System.out.println("Conexão recebida de: " + conexao.getInetAddress().getHostAddress());
        
                    String mensagem = entrada.readUTF();
                    saida.writeUTF("Servidor recebeu: " + mensagem);
                }
            }
        };
    }
}
