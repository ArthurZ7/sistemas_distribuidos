package barbeiro_dorminhoco;

// O MONITOR: Encapsula as variáveis e a sincronização
class Barbearia {
    private final int totalCadeiras;
    private int clientesEsperando = 0;
    
    // Variáveis de Condição
    private boolean barbeiroPronto = false;
    private boolean clientePronto = false;

    public Barbearia(int totalCadeiras) {
        this.totalCadeiras = totalCadeiras;
    }

    // Método sincronizado chamado pelo Cliente
    public synchronized boolean entrar(int idCliente) throws InterruptedException {
        // Se a barbearia estiver lotada, o cliente vai embora
        if (clientesEsperando == totalCadeiras) {
            System.out.println("Cliente " + idCliente + ": Barbearia lotada. Foi embora.");
            return false; 
        }

        clientesEsperando++;
        System.out.println("Cliente " + idCliente + ": Sentou na sala de espera. (Espera: " + clientesEsperando + ")");
        
        // Acorda o barbeiro (caso ele esteja dormindo)
        notifyAll();

        // O cliente fica bloqueado até o barbeiro avisar que está pronto
        // Usamos 'while' para evitar "acordares falsos" (spurious wakeups)
        while (!barbeiroPronto) {
            wait(); // Libera o "lock" da barbearia e dorme
        }

        // É a vez deste cliente!
        clientesEsperando--;
        barbeiroPronto = false; // Consome a vez do barbeiro
        clientePronto = true;   // Avisa o barbeiro que sentou na cadeira de corte
        
        System.out.println("Cliente " + idCliente + ": Sentou na cadeira do barbeiro para o corte.");
        notifyAll(); // Acorda o barbeiro que estava esperando o cliente sentar

        return true;
    }

    // Método sincronizado chamado pelo Barbeiro
    public synchronized void chamarProximo() throws InterruptedException {
        // Se não há clientes, o barbeiro dorme
        while (clientesEsperando == 0) {
            System.out.println("Barbeiro: Zzzzz... (Dormindo na cadeira)");
            wait(); // Libera o "lock" da barbearia e dorme
        }

        // Há clientes. O barbeiro se declara pronto para o corte.
        barbeiroPronto = true;
        notifyAll(); // Acorda os clientes na sala de espera

        // Aguarda até que o cliente escolhido efetivamente sente na cadeira de corte
        while (!clientePronto) {
            wait();
        }

        // Cliente sentou. O barbeiro reseta o estado para o próximo e vai cortar o cabelo.
        clientePronto = false;
    }
}

// Thread do Barbeiro
class Barbeiro implements Runnable {
    private final Barbearia barbearia;

    public Barbeiro(Barbearia barbearia) {
        this.barbearia = barbearia;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Entra no monitor para chamar o próximo cliente ou dormir
                barbearia.chamarProximo();
                
                // CORTA O CABELO (Fora do monitor)
                // É crucial que o 'sleep' do corte aconteça FORA do método 'synchronized'
                // Se ficasse lá dentro, nenhum cliente poderia entrar na barbearia enquanto ocorre o corte.
                System.out.println("Barbeiro: Cortando o cabelo do cliente...");
                Thread.sleep(3000); 
                System.out.println("Barbeiro: Terminou o corte!");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

// Thread do Cliente
class Cliente implements Runnable {
    private final Barbearia barbearia;
    private final int id;

    public Cliente(Barbearia barbearia, int id) {
        this.barbearia = barbearia;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            System.out.println("Cliente " + id + ": Chegou na porta.");
            // O cliente tenta entrar. Todo o processo de espera ocorre dentro do monitor.
            barbearia.entrar(id);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class BarbeiroDorminhocoMonitor {

    public static void main(String[] args) {
        Barbearia barbearia = new Barbearia(3); // 3 cadeiras de espera

        // Inicia a thread do Barbeiro
        Thread barbeiro = new Thread(new Barbeiro(barbearia));
        barbeiro.start();

        // Simula a chegada de clientes
        for (int i = 1; i <= 10; i++) {
            Thread cliente = new Thread(new Cliente(barbearia, i));
            cliente.start();
            try {
                Thread.sleep((long) (Math.random() * 2000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}