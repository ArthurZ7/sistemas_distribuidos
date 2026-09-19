package leitores_e_escritores;
import java.util.concurrent.Semaphore;


// Risco de Inanição (Starvation), pois ele prioriza os leitores, permitindo que múltiplos leitores acessem o recurso compartilhado ao mesmo tempo
public class LeitoresEscritoresSemaforo {

    // Conta quantos leitores estão lendo o recurso neste momento
    private static int leitoresAtivos = 0;

    // Protege o acesso à variável "leitoresAtivos" para evitar condições de corrida
    private static final Semaphore mutex = new Semaphore(1);

    // Protege o recurso compartilhado (banco de dados, arquivo, etc)
    private static final Semaphore recursoCompartilhado = new Semaphore(1);

    public static void main(String[] args) {
        // Inicia threads de Leitores
        for (int i = 1; i <= 5; i++) {
            new Thread(new Leitor(i)).start();
        }

        // Inicia threads de Escritores
        for (int i = 1; i <= 2; i++) {
            new Thread(new Escritor(i)).start();
        }
    }

    // --- Thread do Leitor ---
    static class Leitor implements Runnable {
        private int id;

        public Leitor(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // 1. Entra na região crítica para alterar o contador
                    mutex.acquire();
                    leitoresAtivos++;
                    
                    // Se for o PRIMEIRO leitor, bloqueia o acesso dos escritores ao recurso
                    if (leitoresAtivos == 1) {
                        recursoCompartilhado.acquire();
                    }
                    mutex.release(); // Libera o contador para outros leitores passarem

                    // 2. LENDO (Fora do mutex, múltiplos leitores podem estar aqui ao mesmo tempo)
                    System.out.println("Leitor " + id + " está LENDO. (Leitores ativos: " + leitoresAtivos + ")");
                    Thread.sleep((long) (Math.random() * 2000)); // Tempo de leitura
                    System.out.println("Leitor " + id + " terminou de ler.");

                    // 3. Entra na região crítica novamente para avisar que está saindo
                    mutex.acquire();
                    leitoresAtivos--;
                    
                    // Se for o ÚLTIMO leitor a sair, libera o recurso para os escritores
                    if (leitoresAtivos == 0) {
                        recursoCompartilhado.release();
                    }
                    mutex.release();

                    // Espera um pouco antes de tentar ler novamente
                    Thread.sleep((long) (Math.random() * 3000));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // --- Thread do Escritor ---
    static class Escritor implements Runnable {
        private int id;

        public Escritor(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    System.out.println("Escritor " + id + " quer escrever e está aguardando.");
                    
                    // 1. Tenta bloquear o recurso.
                    // Ficará bloqueado aqui se houver OUTRO escritor OU QUALQUER leitor lendo
                    recursoCompartilhado.acquire();

                    // 2. ESCREVENDO (Acesso exclusivo total)
                    System.out.println("Escritor " + id + " está ESCREVENDO... -> [DADOS ATUALIZADOS]");
                    Thread.sleep((long) (Math.random() * 3000)); // Tempo de escrita
                    System.out.println("Escritor " + id + " terminou de escrever.");

                    // 3. Libera o recurso para quem estiver esperando (leitores ou outros escritores)
                    recursoCompartilhado.release();

                    // Espera um pouco antes de tentar escrever novamente
                    Thread.sleep((long) (Math.random() * 5000));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}