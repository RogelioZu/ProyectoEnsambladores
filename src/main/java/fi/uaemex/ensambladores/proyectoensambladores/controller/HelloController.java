package fi.uaemex.ensambladores.proyectoensambladores.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class HelloController {

    @FXML
    private TextArea panelIzquierdo;
    @FXML
    private TextArea panelCentral;
    @FXML
    private TextArea panelDerecho;

    // Definición de elementos del lenguaje ensamblador
    private static final Set<String> PALABRAS_RESERVADAS = new HashSet<>(Arrays.asList(
            "flat", "stdcall", "ExitProcess", "Proto", "dwExitCode",
            "main", "proc", "invoke", "endp", "end"
    ));

    private static final Set<String> REGISTROS = new HashSet<>(Arrays.asList(
            // Registros de 32 bits
            "eax", "ebx", "ecx", "edx", "esi", "edi", "ebp", "esp",
            // Registros de 16 bits
            "ax", "bx", "cx", "dx", "si", "di", "bp", "sp",
            // Registros de 8 bits
            "ah", "al", "bh", "bl", "ch", "cl", "dh", "dl",
            // Registros de segmento
            "cs", "ds", "es", "fs", "gs", "ss"
    ));

    private static final Set<String> TIPOS_DATO = new HashSet<>(Arrays.asList(
            "db", "dw", "dd", "dq", "dt",
            "byte", "word", "dword", "qword", "tbyte",
            "real4", "real8", "real10"
    ));

    private static final Set<String> INSTRUCCIONES = new HashSet<>(Arrays.asList(
            "mov", "add", "sub", "mul", "div", "inc", "dec",
            "and", "or", "xor", "not", "shl", "shr",
            "jmp", "je", "jne", "jg", "jl", "jge", "jle",
            "call", "ret", "push", "pop",
            "cmp", "test", "lea", "int"
    ));

    @FXML
    private void abrirArchivo(ActionEvent event) {
        FileChooser selectorArchivo = new FileChooser();
        selectorArchivo.setTitle("Abrir Archivo de Texto");

        FileChooser.ExtensionFilter filtroTxt = new FileChooser.ExtensionFilter("Archivos de Texto (*.txt)", "*.txt");
        selectorArchivo.getExtensionFilters().add(filtroTxt);

        Stage stage = (Stage) panelIzquierdo.getScene().getWindow();
        File archivo = selectorArchivo.showOpenDialog(stage);

        if (archivo != null) {
            try {
                String contenido = new String(Files.readAllBytes(Paths.get(archivo.toURI())));
                panelIzquierdo.setText(contenido);
            } catch (IOException e) {
                System.out.println("Error al leer el archivo: " + e.getMessage());
            }
        }
    }

    @FXML
    private void AnalizarTexto() {
        String text = panelIzquierdo.getText();
        panelCentral.clear();
        panelDerecho.clear();

        if (text == null || text.trim().isEmpty()) {
            System.out.println("El area de texto esta vacia");
            return;
        }

        String[] lineas = text.split("\\r?\\n");
        StringBuilder textoResultado = new StringBuilder();

        // Estructuras para almacenar elementos identificados
        Set<String> directivasEncontradas = new LinkedHashSet<>();
        Set<String> palabrasReservadasEncontradas = new LinkedHashSet<>();
        Set<String> registrosEncontrados = new LinkedHashSet<>();
        Set<String> tiposDatoEncontrados = new LinkedHashSet<>();
        Set<String> instruccionesEncontradas = new LinkedHashSet<>();
        Set<String> constantesNumericas = new LinkedHashSet<>();
        Set<String> etiquetas = new LinkedHashSet<>();

        for (String linea : lineas) {
            if (linea.trim().startsWith(";")) {
                continue;
            }

            // Detectar etiquetas (palabras seguidas de dos puntos)
            if (linea.contains(":") && !linea.trim().startsWith(";")) {
                String[] partes = linea.split(":");
                if (partes.length > 0) {
                    String posibleEtiqueta = partes[0].trim();
                    if (!posibleEtiqueta.isEmpty() && esIdentificadorValido(posibleEtiqueta)) {
                        etiquetas.add(posibleEtiqueta);
                    }
                }
            }

            String[] palabras = linea.split("\\s+|,");
            for (String palabra : palabras) {
                palabra = palabra.trim();
                if (palabra.isEmpty()) continue;

                // Limpiar caracteres especiales al final
                String palabraLimpia = palabra.replaceAll("[,;:]", "");
                String palabraMinuscula = palabraLimpia.toLowerCase();

                textoResultado.append(palabraLimpia).append("\n");

                // Identificar directivas (comienzan con punto)
                if (palabraLimpia.startsWith(".")) {
                    directivasEncontradas.add(palabraLimpia);
                }
                // Identificar palabras reservadas
                else if (PALABRAS_RESERVADAS.contains(palabraMinuscula) ||
                        PALABRAS_RESERVADAS.contains(palabraLimpia)) {
                    palabrasReservadasEncontradas.add(palabraLimpia);
                }
                // Identificar registros
                else if (REGISTROS.contains(palabraMinuscula)) {
                    registrosEncontrados.add(palabraMinuscula);
                }
                // Identificar tipos de dato
                else if (TIPOS_DATO.contains(palabraMinuscula)) {
                    tiposDatoEncontrados.add(palabraMinuscula);
                }
                // Identificar instrucciones
                else if (INSTRUCCIONES.contains(palabraMinuscula)) {
                    instruccionesEncontradas.add(palabraMinuscula);
                }
                // Identificar constantes numéricas
                else if (esConstanteNumerica(palabraLimpia)) {
                    constantesNumericas.add(palabraLimpia);
                }
            }
        }

        panelCentral.setText(textoResultado.toString());

        // Generar reporte en panel derecho
        StringBuilder reporte = new StringBuilder();
        reporte.append("=== ANÁLISIS DEL PROGRAMA ENSAMBLADOR ===\n\n");

        if (!directivasEncontradas.isEmpty()) {
            reporte.append("DIRECTIVAS:\n");
            for (String dir : directivasEncontradas) {
                reporte.append("  - ").append(dir).append("\n");
            }
            reporte.append("\n");
        }

        if (!palabrasReservadasEncontradas.isEmpty()) {
            reporte.append("PALABRAS RESERVADAS:\n");
            for (String pr : palabrasReservadasEncontradas) {
                reporte.append("  - ").append(pr).append("\n");
            }
            reporte.append("\n");
        }

        if (!registrosEncontrados.isEmpty()) {
            reporte.append("REGISTROS:\n");
            for (String reg : registrosEncontrados) {
                reporte.append("  - ").append(reg).append("\n");
            }
            reporte.append("\n");
        }

        if (!instruccionesEncontradas.isEmpty()) {
            reporte.append("INSTRUCCIONES:\n");
            for (String inst : instruccionesEncontradas) {
                reporte.append("  - ").append(inst).append("\n");
            }
            reporte.append("\n");
        }

        if (!tiposDatoEncontrados.isEmpty()) {
            reporte.append("TIPOS DE DATO:\n");
            for (String tipo : tiposDatoEncontrados) {
                reporte.append("  - ").append(tipo).append("\n");
            }
            reporte.append("\n");
        }

        if (!constantesNumericas.isEmpty()) {
            reporte.append("CONSTANTES NUMÉRICAS:\n");
            for (String cons : constantesNumericas) {
                reporte.append("  - ").append(cons).append(" (");
                reporte.append(identificarTipoConstante(cons)).append(")\n");
            }
            reporte.append("\n");
        }

        if (!etiquetas.isEmpty()) {
            reporte.append("ETIQUETAS:\n");
            for (String etiq : etiquetas) {
                reporte.append("  - ").append(etiq).append("\n");
            }
            reporte.append("\n");
        }

        panelDerecho.setText(reporte.toString());
    }

    private boolean esConstanteNumerica(String palabra) {
        if (palabra == null || palabra.isEmpty()) return false;

        // Hexadecimal (termina en h o comienza con 0x)
        if (palabra.matches("(?i)[0-9a-f]+h") || palabra.matches("(?i)0x[0-9a-f]+")) {
            return true;
        }
        // Binario (termina en b)
        if (palabra.matches("[01]+b")) {
            return true;
        }
        // Octal (termina en o o q)
        if (palabra.matches("[0-7]+[oq]")) {
            return true;
        }
        // Decimal simple
        if (palabra.matches("\\d+")) {
            return true;
        }

        return false;
    }

    private String identificarTipoConstante(String constante) {
        if (constante.matches("(?i)[0-9a-f]+h") || constante.matches("(?i)0x[0-9a-f]+")) {
            return "Hexadecimal";
        }
        if (constante.matches("[01]+b")) {
            return "Binario";
        }
        if (constante.matches("[0-7]+[oq]")) {
            return "Octal";
        }
        if (constante.matches("\\d+")) {
            return "Decimal";
        }
        return "Desconocido";
    }

    private boolean esIdentificadorValido(String palabra) {
        return palabra.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    @FXML
    private void Limpiar() {
        panelIzquierdo.setText("");
        panelCentral.setText("");
        panelDerecho.setText("");
    }
}