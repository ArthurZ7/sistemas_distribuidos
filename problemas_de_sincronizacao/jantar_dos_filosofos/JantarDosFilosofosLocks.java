import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Filosofo extends Thread {
    private final int id;
    private final Lock garfoEsquerdo;
    private final Lock garfoDireito;

    public Filosofo(int id, Lock garfoEsquerdo, Lock garfoDireito) {
        this.id = id;
        this.garfoEsquerdo = garfoEsquerdo;
        this.garfoDireito = garfoDireito;
    }

    private void pensar() throws InterruptedException {
        System.out.println("Filósofo " + id + " está PENSANDO.");
        Thread.sleep((long) (Math.random() * 1000));
    }

    private void comer() throws InterruptedException {
        System.out.println("Filósofo " + id + " está COMENDO.");
        Thread.sleep((long) (Math.random() * 1000));
    }

    @Override
    public void run() {
        try {
            while (true) {
                pensar();

                // Loop de tentativa de pegar os garfos
                while (true) {
                    boolean pegouEsquerda = garfoEsquerdo.tryLock();
                    boolean pegouDireita = false;

                    if (pegouEsquerda) {
                        // Conseguiu o da esquerda, tenta o da direita
                        pegouDireita = garfoDireito.tryLock();

                        if (pegouDireita) {
                            // Sucesso! Conseguiu os dois garfos. Sai do loop de tentativas.
                            break; 
                        } else {
                            // O garfo da direita estava ocupado. 
                            // Solta o da esquerda para não travar os vizinhos (evita Deadlock).
                            garfoEsquerdo.unlock();
                        }
                    }

                    // Espera um tempo aleatório antes de tentar novamente (evita Livelock)
                    Thread.sleep((long) (Math.random() * 50));
                }

                // Se chegou aqui, é porque o break foi acionado (tem os dois garfos)
                comer();

                // Devolve os garfos (a ordem do unlock não importa muito aqui)
                garfoDireito.unlock();
                garfoEsquerdo.unlock();
            }
        } catch (InterruptedException e) {
            System.out.println("Filósofo " + id + " foi interrompido.");
            Thread.currentThread().interrupt();
        }
    }
}

public class JantarDosFilosofosLocks {
    public static void main(String[] args) {
        int NUM_FILOSOFOS = 5;
        Lock[] garfos = new ReentrantLock[NUM_FILOSOFOS];
        Filosofo[] filosofos = new Filosofo[NUM_FILOSOFOS];

        // 1. Inicializa os Locks (garfos)
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            garfos[i] = new ReentrantLock();
        }

        // 2. Inicializa e inicia as threads
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            Lock garfoEsquerdo = garfos[i];
            Lock garfoDireito = garfos[(i + 1) % NUM_FILOSOFOS];

            filosofos[i] = new Filosofo(i, garfoEsquerdo, garfoDireito);
            filosofos[i].start();
        }
    }
}