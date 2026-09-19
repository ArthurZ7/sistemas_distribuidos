import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JantarDosFilosofosMonitor {

    // Enum para representar os estados
    enum Estado {
        PENSANDO, FAMINTO, COMENDO
    }

    // Classe Monitor que gerencia o estado da mesa
    static class MonitorMesa {
        private final int NUM_FILOSOFOS = 5;
        private Estado[] estado = new Estado[NUM_FILOSOFOS];
        
        // Trava global para a mesa
        private Lock lock = new ReentrantLock();
        // Uma condição de espera para cada filósofo
        private Condition[] condicoes = new Condition[NUM_FILOSOFOS];

        public MonitorMesa() {
            for (int i = 0; i < NUM_FILOSOFOS; i++) {
                estado[i] = Estado.PENSANDO;
                condicoes[i] = lock.newCondition();
            }
        }

        // Método auxiliar para calcular vizinhos
        private int esquerda(int i) { return (i + 4) % NUM_FILOSOFOS; }
        private int direita(int i) { return (i + 1) % NUM_FILOSOFOS; }

        // Verifica se o filósofo i pode comer
        private void testar(int i) {
            if (estado[i] == Estado.FAMINTO &&
                estado[esquerda(i)] != Estado.COMENDO &&
                estado[direita(i)] != Estado.COMENDO) {
                
                estado[i] = Estado.COMENDO;
                // Acorda o filósofo caso ele estivesse esperando (await)
                condicoes[i].signal();
            }
        }

        public void pegarGarfos(int i) throws InterruptedException {
            lock.lock(); // Bloqueia a mesa para checar estados
            try {
                estado[i] = Estado.FAMINTO;
                testar(i); // Tenta comer
                
                // Se o teste falhou (vizinho estava comendo), ele dorme
                while (estado[i] != Estado.COMENDO) {
                    condicoes[i].await(); 
                }
            } finally {
                lock.unlock(); // Libera a mesa
            }
        }

        public void largarGarfos(int i) {
            lock.lock();
            try {
                estado[i] = Estado.PENSANDO;
                // Ao terminar, testa se os vizinhos podem comer agora
                testar(esquerda(i));
                testar(direita(i));
            } finally {
                lock.unlock();
            }
        }
    }

    // Classe Thread do Filósofo
    static class Filosofo extends Thread {
        private int id;
        private MonitorMesa monitor;

        public Filosofo(int id, MonitorMesa monitor) {
            this.id = id;
            this.monitor = monitor;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    System.out.println("Filósofo " + id + " está PENSANDO.");
                    Thread.sleep((long) (Math.random() * 1000));

                    System.out.println("Filósofo " + id + " está com fome...");
                    monitor.pegarGarfos(id);

                    System.out.println("Filósofo " + id + " está COMENDO.");
                    Thread.sleep((long) (Math.random() * 1000));

                    monitor.largarGarfos(id);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        MonitorMesa monitor = new MonitorMesa();
        Filosofo[] filosofos = new Filosofo[5];

        for (int i = 0; i < 5; i++) {
            filosofos[i] = new Filosofo(i, monitor);
            filosofos[i].start();
        }
    }
}