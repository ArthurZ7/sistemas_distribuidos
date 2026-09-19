package barbeiro_dorminhoco;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BarbeiroDorminhocoLocks {

    public static void main(String[] args) {
        Barbearia barbearia = new Barbearia(3);

        Thread barbeiro = new Thread(new Barbeiro(barbearia));
        barbeiro.start();

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

// O estado compartilhado protegido por Lock
class Barbearia {
    private final int totalCadeiras;
    private int clientesEsperando = 0;

    // O Lock substitui a palavra "synchronized"
    private final Lock lock = new ReentrantLock();
    
    // Condições separadas (Múltiplas "salas de espera")
    private final Condition condBarbeiroDormindo = lock.newCondition();
    private final Condition condClienteEsperandoVez = lock.newCondition();
    private final Condition condAguardandoSentar = lock.newCondition();

    // Estado lógico
    private boolean barbeiroPronto = false;
    private boolean clienteSentou = false;

    public Barbearia(int totalCadeiras) {
        this.totalCadeiras = totalCadeiras;
    }

    public boolean entrar(int idCliente) throws InterruptedException {
        // Bloqueia o acesso às variáveis (equivalente a entrar no bloco synchronized)
        lock.lock(); 
        try {
            if (clientesEsperando == totalCadeiras) {
                System.out.println("Cliente " + idCliente + ": Barbearia lotada. Foi embora.");
                return false;
            }

            clientesEsperando++;
            System.out.println("Cliente " + idCliente + ": Sentou na sala de espera. (Espera: " + clientesEsperando + ")");
            
            // Acorda EXCLUSIVAMENTE o barbeiro (se ele estiver na condBarbeiroDormindo)
            condBarbeiroDormindo.signal();

            // Aguarda a vez
            while (!barbeiroPronto) {
                condClienteEsperandoVez.await(); // O cliente dorme nesta condição específica
            }

            // Chegou a vez
            clientesEsperando--;
            barbeiroPronto = false;
            clienteSentou = true;
            System.out.println("Cliente " + idCliente + ": Sentou na cadeira do barbeiro para o corte.");
            
            // Avisa EXCLUSIVAMENTE o barbeiro que ele já sentou na cadeira
            condAguardandoSentar.signal();

            return true;

        } finally {
            // O unlock DEVE estar no finally para garantir que a trava seja liberada
            // mesmo que ocorra alguma exceção no meio do código.
            lock.unlock(); 
        }
    }

    public void chamarProximo() throws InterruptedException {
        lock.lock();
        try {
            while (clientesEsperando == 0) {
                System.out.println("Barbeiro: Zzzzz... (Dormindo na cadeira)");
                condBarbeiroDormindo.await(); // Barbeiro dorme nesta condição
            }

            barbeiroPronto = true;
            
            // Chama EXCLUSIVAMENTE UM cliente da fila (signal ao invés de signalAll)
            condClienteEsperandoVez.signal();

            // Aguarda o cliente se levantar da espera e sentar na cadeira de corte
            while (!clienteSentou) {
                condAguardandoSentar.await();
            }

            // Reseta para o próximo ciclo
            clienteSentou = false;

        } finally {
            lock.unlock();
        }
    }
}

class Barbeiro implements Runnable {
    private final Barbearia barbearia;

    public Barbeiro(Barbearia barbearia) {
        this.barbearia = barbearia;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Sincroniza a chamada
                barbearia.chamarProximo();
                
                // Corta o cabelo fora da área bloqueada (sem segurar o lock)
                System.out.println("Barbeiro: Cortando o cabelo...");
                Thread.sleep(3000); 
                System.out.println("Barbeiro: Terminou o corte!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

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
            barbearia.entrar(id);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}