package com.logitech.sgfl.service;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mini servidor HTTP para os testes: responde qualquer caminho com o
 * status/conteúdo informado, evitando depender de Nominatim ou OSRM
 * reais na esteira de CI. Guarda o último caminho, método e corpo
 * chamados para os testes validarem o formato das requisições.
 */
final class StubHttp implements AutoCloseable {

    private final HttpServer servidor;
    private final AtomicReference<String> ultimoCaminho = new AtomicReference<>();
    private final AtomicReference<String> ultimoMetodo = new AtomicReference<>();
    private final AtomicReference<String> ultimoCorpo = new AtomicReference<>();
    private final AtomicInteger quantidadeRequisicoes = new AtomicInteger();
    private final AtomicReference<Resposta> resposta = new AtomicReference<>();

    private record Resposta(int status, String corpo) {}

    StubHttp(int status, String corpo) throws IOException {
        this.servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.resposta.set(new Resposta(status, corpo));
        servidor.createContext("/", exchange -> {
            ultimoCaminho.set(exchange.getRequestURI().toString());
            ultimoMetodo.set(exchange.getRequestMethod());
            quantidadeRequisicoes.incrementAndGet();
            try (var corpoRequisicao = exchange.getRequestBody()) {
                ultimoCorpo.set(new String(corpoRequisicao.readAllBytes(), StandardCharsets.UTF_8));
            }
            Resposta atual = this.resposta.get();
            byte[] bytes = atual.corpo().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(atual.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        this.servidor.start();
    }

    void responder(int status, String corpo) {
        this.resposta.set(new Resposta(status, corpo));
    }

    String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    String ultimoCaminho() {
        return ultimoCaminho.get();
    }

    String ultimoMetodo() {
        return ultimoMetodo.get();
    }

    String ultimoCorpo() {
        return ultimoCorpo.get();
    }

    int quantidadeRequisicoes() {
        return quantidadeRequisicoes.get();
    }

    @Override
    public void close() {
        this.servidor.stop(0);
    }
}
