# Sockets Multithread x Java NIO com ISO 8583 (Spring Boot)

## Executar
Terminal 1 (servidores: multithread na 8080, NIO na 8081):
    mvn spring-boot:run

Terminal 2 (cliente de testes):
    mvn spring-boot:run -Dspring-boot.run.arguments=--app.modo=cliente

Obs.: o teste de concorrência consome o saldo dos cartões 400000000098/99;
para repetir, reinicie o servidor.
