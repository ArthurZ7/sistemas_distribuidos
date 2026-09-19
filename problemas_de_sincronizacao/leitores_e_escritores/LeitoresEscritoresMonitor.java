package leitores_e_escritores;

public class LeitoresEscritoresMonitor {

    public static void main(String[] args) {
        BancoDeDados banco = new BancoDeDados();

        // Inicia threads de Leitores
        for (int i = 1; i <= 5; i++) {
            new Thread(new Leitor(banco, i)).start();
        }

        // Inicia threads de Escritores
        for (int i = 1; i <= 2; i++) {
            new Thread(new Escritor(banco, i)).start();
        }
    }
}

// O MONITOR: Encapsula as regras de acesso e o estado
class BancoDeDados {
    private int leitoresAtivos = 0;
    private boolean escritorAtivo = false;
    
    // Variável crucial para evitar a inanição (starvation) dos escritores
    private int escritoresEsperando = 0; 

    // --- Controles do Leitor ---
    
    public synchronized void iniciarLeitura(int id) throws InterruptedException {
        // Um leitor deve esperar se houver alguém escrevendo 
        // OU se houver um escritor na fila de espera (isso resolve o Starvation).
        while (escritorAtivo || escritoresEsperando > 0) {
            wait();
        }
        
        leitoresAtivos++;
        System.out.println("Leitor " + id + " entrou. (Leitores lendo: " + leitoresAtivos + ")");
    }

    public synchronized void finalizarLeitura(int id) {
        leitoresAtivos--;
        System.out.println("Leitor " + id + " saiu.");
        
        // Se este foi o último leitor a sair, acorda quem estiver esperando (os escritores)
        if (leitoresAtivos == 0) {
            notifyAll(); 
        }
    }

    // --- Controles do Escritor ---

    public synchronized void iniciarEscrita(int id) throws InterruptedException {
        escritoresEsperando++; // Entrou na fila de espera
        
        // Um escritor deve esperar se houver qualquer leitor lendo 
        // OU se já houver outro escritor escrevendo.
        while (leitoresAtivos > 0 || escritorAtivo) {
            wait();
        }
        
        escritoresEsperando--; // Saiu da fila de espera e assumiu o controle
        escritorAtivo = true;
        System.out.println("Escritor " + id + " entrou e BLOQUEOU o banco para escrita.");
    }

    public synchronized void finalizarEscrita(int id) {
        escritorAtivo = false;
        System.out.println("Escritor " + id + " saiu e LIBEROU o banco.");
        
        // Acorda todos (tanto leitores quanto outros escritores que estavam esperando)
        notifyAll(); 
    }
}

// Thread do Leitor
class Leitor implements Runnable {
    private final BancoDeDados banco;
    private final int id;

    public Leitor(BancoDeDados banco, int id) {
        this.banco = banco;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            while (true) {
                banco.iniciarLeitura(id);
                
                // LENDO (Fora do bloco sincronizado)
                Thread.sleep((long) (Math.random() * 2000)); 
                
                banco.finalizarLeitura(id);
                
                // Vai fazer outra coisa antes de ler de novo
                Thread.sleep((long) (Math.random() * 3000));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

// Thread do Escritor
class Escritor implements Runnable {
    private final BancoDeDados banco;
    private final int id;

    public Escritor(BancoDeDados banco, int id) {
        this.banco = banco;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            while (true) {
                banco.iniciarEscrita(id);
                
                // ESCREVENDO (Fora do bloco sincronizado, mas protegido pelas regras lógicas)
                Thread.sleep((long) (Math.random() * 3000)); 
                
                banco.finalizarEscrita(id);
                
                // Vai fazer outra coisa antes de escrever de novo
                Thread.sleep((long) (Math.random() * 5000));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}