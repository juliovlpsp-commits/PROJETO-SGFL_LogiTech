package com.logitech.sgfl.service;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mini servidor HTTP para os testes: responde qualquer caminho com o
 * status/conteúdo informado, evitando depender de Nominatim ou OSRM
 * reais na esteira de CI. Guarda o último caminho chamado para os
 * testes validarem o formato da URL do provedor.
 */
final class StubHttp implements AutoCloseable {

    private final HttpServer servidor;
    private final AtomicReference<String> ultimoCaminho = new AtomicReference<>();

    StubHttp(int status, String corpo) throws IOException {
        this.servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        responder(status, corpo);
        this.servidor.start();
    }

    void responder(int status, String corpo) {
        servidor.createContext("/", exchange -> {
            ultimoCaminho.set(exchange.getRequestURI().toString());
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    String ultimoCaminho() {
        return ultimoCaminho.get();
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
