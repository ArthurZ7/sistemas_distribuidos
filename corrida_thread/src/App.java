public class App {
    public static void main(String[] args) throws Exception {
        Corrida corrida1 = new Corrida("Piloto 1");
        Corrida corrida2 = new Corrida("Piloto 2");
        Corrida corrida3 = new Corrida("Piloto 3");
        Thread thread1 = new Thread(corrida1);
        Thread thread2 = new Thread(corrida2);
        Thread thread3 = new Thread(corrida3);
        thread1.start();
        thread2.start();
        thread3.start();
        
    }
}
