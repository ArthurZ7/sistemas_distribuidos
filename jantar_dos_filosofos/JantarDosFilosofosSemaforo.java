import java.util.concurrent.Semaphore;

class Filosofo extends Thread {
    private final int id;
    private final Semaphore garfoEsquerdo;
    private final Semaphore garfoDireito;

    public Filosofo(int id, Semaphore garfoEsquerdo, Semaphore garfoDireito) {
        this.id = id;
        this.garfoEsquerdo = garfoEsquerdo;
        this.garfoDireito = garfoDireito;
    }

    private void pensar() throws InterruptedException {
        System.out.println("Filósofo " + id + " está pensando.");
        // Simula o tempo pensando
        Thread.sleep((long) (Math.random() * 1000));
    }

    private void comer() throws InterruptedException {
        System.out.println("Filósofo " + id + " está COMENDO.");
        // Simula o tempo comendo
        Thread.sleep((long) (Math.random() * 1000));
    }

    @Override
    public void run() {
        try {
            while (true) {
                pensar();

                // Estratégia assimétrica para quebrar a espera circular (Deadlock)
                if (id % 2 == 0) {
                    garfoDireito.acquire(); // Pega direita primeiro
                    garfoEsquerdo.acquire(); // Depois esquerda
                } else {
                    garfoEsquerdo.acquire(); // Pega esquerda primeiro
                    garfoDireito.acquire(); // Depois direita
                }

                comer();

                // Libera os garfos para os vizinhos
                garfoEsquerdo.release();
                garfoDireito.release();
            }
        } catch (InterruptedException e) {
            System.out.println("Filósofo " + id + " foi interrompido.");
            Thread.currentThread().interrupt();
        }
    }
}

public class JantarDosFilosofosSemaforo {
    public static void main(String[] args) {
        int NUM_FILOSOFOS = 5;
        Semaphore[] garfos = new Semaphore[NUM_FILOSOFOS];
        Filosofo[] filosofos = new Filosofo[NUM_FILOSOFOS];

        // 1. Inicializa os semáforos (garfos) com 1 permissão cada
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            garfos[i] = new Semaphore(1);
        }

        // 2. Inicializa e inicia as threads dos filósofos
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            Semaphore garfoEsquerdo = garfos[i];
            // O uso do módulo (%) faz a mesa ser circular
            Semaphore garfoDireito = garfos[(i + 1) % NUM_FILOSOFOS];
            
            filosofos[i] = new Filosofo(i, garfoEsquerdo, garfoDireito);
            filosofos[i].start();
        }
    }
}