package barbeiro_dorminhoco;

import java.util.concurrent.Semaphore;

public class BarbeiroDorminhocoSemaforo {

    // Número total de cadeiras na sala de espera
    private static final int CADEIRAS_ESPERA = 3;
    
    // Contagem de clientes esperando
    private static int clientesEsperando = 0;

    // Semáforos
    // Controla o acesso à variável clientesEsperando (Exclusão Mútua)
    private static final Semaphore mutex = new Semaphore(1);
    
    // Conta quantos clientes estão aguardando para acordar o barbeiro
    private static final Semaphore clientes = new Semaphore(0);
    
    // Sinaliza se o barbeiro está pronto para cortar o cabelo
    private static final Semaphore barbeiro = new Semaphore(0);

    // Thread do Barbeiro
    static class Barbeiro implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    System.out.println("Barbeiro: Zzzzz... (Dormindo ou aguardando clientes)");
                    
                    // Barbeiro dorme se não houver clientes (clientes == 0)
                    // Se houver, ele adquire um cliente e acorda
                    clientes.acquire();
                    
                    // Entra na região crítica para atualizar o número de clientes esperando
                    mutex.acquire();
                    clientesEsperando--;
                    
                    // Sinaliza para UM cliente que o barbeiro está pronto
                    barbeiro.release();
                    mutex.release(); // Sai da região crítica
                    
                    // Corta o cabelo (fora da região crítica para não travar a barbearia)
                    System.out.println("Barbeiro: Cortando o cabelo de um cliente.");
                    Thread.sleep(3000); // Simula o tempo do corte
                    System.out.println("Barbeiro: Terminou o corte.");
                    
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Thread do Cliente
    static class Cliente implements Runnable {
        private int id;

        public Cliente(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                System.out.println("Cliente " + id + ": Chegou na barbearia.");
                
                // Entra na região crítica para verificar as cadeiras
                mutex.acquire();
                
                if (clientesEsperando < CADEIRAS_ESPERA) {
                    clientesEsperando++;
                    System.out.println("Cliente " + id + ": Sentou na sala de espera. (Clientes esperando: " + clientesEsperando + ")");
                    
                    // Sinaliza ao barbeiro que há um cliente (acorda o barbeiro se estiver dormindo)
                    clientes.release();
                    mutex.release(); // Sai da região crítica
                    
                    // Fica bloqueado até o barbeiro sinalizar que é a vez dele
                    barbeiro.acquire();
                    System.out.println("Cliente " + id + ": Sentou na cadeira do barbeiro e está cortando o cabelo.");
                } else {
                    // Sala de espera cheia
                    mutex.release(); // Libera a verificação para os próximos
                    System.out.println("Cliente " + id + ": Barbearia lotada. Foi embora.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        public static void main(String[] args) {
        // Inicia a thread do Barbeiro
        Thread threadBarbeiro = new Thread(new Barbeiro());
        threadBarbeiro.start();

        // Simula a chegada de clientes em tempos aleatórios
        for (int i = 1; i <= 10; i++) {
            Thread threadCliente = new Thread(new Cliente(i));
            threadCliente.start();
            try {
                // Intervalo aleatório entre a chegada de cada cliente
                Thread.sleep((long) (Math.random() * 2000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        }

    }
}