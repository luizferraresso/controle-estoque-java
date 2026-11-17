package br.edu.estoque.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public final class CsvExporter {
    private CsvExporter() { }

    public static void exportar(Path arquivo, List<String> cabecalho, List<List<String>> linhas) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            out.write('\uFEFF'); // BOM facilita a abertura de acentos no Excel.
            escrever(out, cabecalho);
            for (List<String> linha : linhas) escrever(out, linha);
        }
    }
    private static void escrever(BufferedWriter out, List<String> linha) throws IOException {
        for (int i = 0; i < linha.size(); i++) {
            if (i > 0) out.write(';');
            String valor = linha.get(i) == null ? "" : linha.get(i);
            String inicio = valor.stripLeading();
            if (!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0)) >= 0) valor = "'" + valor;
            out.write('"'); out.write(valor.replace("\"", "\"\"")); out.write('"');
        }
        out.write("\r\n");
    }
}
