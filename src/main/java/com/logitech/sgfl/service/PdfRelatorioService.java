package com.logitech.sgfl.service;

import com.logitech.sgfl.me.Entrega;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PdfRelatorioService {
    /**
     * PDF simples e sem dependência externa, suficiente para relatórios operacionais.
     * O documento usa Helvetica e uma página A4 com as principais informações das entregas.
     */
    public byte[] gerarEntregas(List<Entrega> entregas) {
        StringBuilder stream = new StringBuilder();
        stream.append("BT /F1 16 Tf 40 800 Td (SGFL - Relatorio de Entregas) Tj ET\n");
        int y = 775;
        stream.append("BT /F1 9 Tf 40 ").append(y).append(" Td (ID | Rastreio | Status | Destino | Motorista | Veiculo) Tj ET\n");
        y -= 16;
        for (Entrega e : entregas) {
            if (y < 45) break;
            String line = escape(String.format("%s | %s | %s | %s | %s | %s",
                    value(e.getId()), value(e.getCodigoRastreio()), value(e.getStatus()),
                    value(e.getEnderecoDestino()),
                    value(e.getMotorista() == null ? null : e.getMotorista().getNome()),
                    value(e.getVeiculo() == null ? null : e.getVeiculo().getPlaca())));
            stream.append("BT /F1 8 Tf 40 ").append(y).append(" Td (").append(line).append(") Tj ET\n");
            y -= 13;
        }

        byte[] content = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

        long[] offsets = new long[6];
        offsets[1] = out.size(); write(out, "1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
        offsets[2] = out.size(); write(out, "2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n");
        offsets[3] = out.size(); write(out, "3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>endobj\n");
        offsets[4] = out.size(); write(out, "4 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n");
        offsets[5] = out.size(); write(out, "5 0 obj<< /Length " + content.length + " >>stream\n");
        out.writeBytes(content);
        write(out, "endstream endobj\n");

        long xref = out.size();
        write(out, "xref\n0 6\n0000000000 65535 f \n");
        for (int i = 1; i <= 5; i++) {
            write(out, String.format("%010d 00000 n \n", offsets[i]));
        }
        write(out, "trailer<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private void write(ByteArrayOutputStream out, String value) {
        out.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    private String value(Object value) {
        return value == null ? "-" : String.valueOf(value).replace('|', '/');
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
