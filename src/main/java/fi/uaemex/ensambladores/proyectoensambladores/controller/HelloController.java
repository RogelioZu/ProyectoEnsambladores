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
    @FXML
    private TextArea panelSegmentos;
    @FXML
    private TextArea panelTablaSimbolos;

    // Definición de elementos del lenguaje ensamblador
    private static final Set<String> PALABRAS_RESERVADAS = new HashSet<>(Arrays.asList(
            "flat", "stdcall", "ExitProcess", "Proto", "dwExitCode",
            "main", "proc", "invoke", "endp", "end", "segment", "ends"
    ));

    private static final Set<String> DIRECTIVAS = new HashSet<>(Arrays.asList(
            ".data", ".stack", ".code", ".model"
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
            "real4", "real8", "real10", "dup"
    ));

    private static final Set<String> INSTRUCCIONES = new HashSet<>(Arrays.asList(
            "mov", "add", "sub", "mul", "div", "inc", "dec",
            "and", "or", "xor", "not", "shl", "shr",
            "jmp", "je", "jne", "jg", "jl", "jge", "jle",
            "call", "ret", "push", "pop",
            "cmp", "test", "lea", "int"
    ));

    // Clase para almacenar información de símbolos
    private static class Simbolo {
        String nombre;
        String tipo;
        String valor;
        int tamanio;

        public Simbolo(String nombre, String tipo, String valor, int tamanio) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.valor = valor;
            this.tamanio = tamanio;
        }
    }

    @FXML
    private void abrirArchivo(ActionEvent event) {
        FileChooser selectorArchivo = new FileChooser();
        selectorArchivo.setTitle("Abrir Archivo de Texto");

        FileChooser.ExtensionFilter filtroTxt = new FileChooser.ExtensionFilter("Archivos de Texto (*.txt)", "*.txt","*.asm");
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
        panelSegmentos.clear();
        panelTablaSimbolos.clear();

        if (text == null || text.trim().isEmpty()) {
            System.out.println("El área de texto está vacía");
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

        // Tabla de símbolos
        Map<String, Simbolo> tablaSimbolos = new LinkedHashMap<>();

        // Análisis de segmentos
        String segmentoActual = "";
        List<String> lineasSegmentoData = new ArrayList<>();
        List<String> lineasSegmentoPila = new ArrayList<>();
        List<String> lineasSegmentoCodigo = new ArrayList<>();
        Map<Integer, String> analisisLineas = new LinkedHashMap<>();

        int numeroLinea = 1;
        for (String linea : lineas) {
            String lineaTrim = linea.trim();

            // Detectar segmento actual
            if (lineaTrim.toLowerCase().contains(".data")) {
                segmentoActual = "data";
                directivasEncontradas.add(".data");
            } else if (lineaTrim.toLowerCase().contains(".stack")) {
                segmentoActual = "stack";
                directivasEncontradas.add(".stack");
            } else if (lineaTrim.toLowerCase().contains(".code")) {
                segmentoActual = "code";
                directivasEncontradas.add(".code");
            }

            if (lineaTrim.startsWith(";")) {
                numeroLinea++;
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

            // Análisis de declaraciones de variables en segmento .data
            boolean esCorrecta = true;
            StringBuilder errores = new StringBuilder();

            if (segmentoActual.equals("data") && !lineaTrim.startsWith(".") && !lineaTrim.isEmpty()) {
                String resultadoAnalisis = analizarDeclaracionVariable(lineaTrim, tablaSimbolos);
                if (resultadoAnalisis != null) {
                    esCorrecta = false;
                    errores.append(resultadoAnalisis);
                }
                lineasSegmentoData.add(lineaTrim);
            } else if (segmentoActual.equals("stack") && !lineaTrim.isEmpty()) {
                lineasSegmentoPila.add(lineaTrim);
            } else if (segmentoActual.equals("code") && !lineaTrim.isEmpty()) {
                lineasSegmentoCodigo.add(lineaTrim);

                // Validar instrucciones en código
                if (!lineaTrim.startsWith(".") && !lineaTrim.contains(":")) {
                    String errorInstruccion = validarInstruccion(lineaTrim);
                    if (errorInstruccion != null) {
                        esCorrecta = false;
                        errores.append(errorInstruccion);
                    }
                }
            }

            // Guardar análisis de la línea
            if (!lineaTrim.isEmpty()) {
                if (esCorrecta) {
                    analisisLineas.put(numeroLinea, "✓ CORRECTA");
                } else {
                    analisisLineas.put(numeroLinea, "✗ INCORRECTA - " + errores.toString());
                }
            }

            String[] palabras = linea.split("\\s+|,");
            for (String palabra : palabras) {
                palabra = palabra.trim();
                if (palabra.isEmpty()) continue;

                // Limpiar caracteres especiales al final
                String palabraLimpia = palabra.replaceAll("[,;:()]", "");
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

            numeroLinea++;
        }

        // PANEL CENTRAL - Mantener funcionalidad original (tokens)
        panelCentral.setText(textoResultado.toString());

        // PANEL DERECHO - Mantener funcionalidad original (reporte de elementos)
        StringBuilder reporte = new StringBuilder();

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

        // NUEVOS PANELES INFERIORES
        // Panel Segmentos - Análisis de líneas y segmentos
        generarAnalisisSegmentos(lineasSegmentoData, lineasSegmentoPila, lineasSegmentoCodigo, analisisLineas);

        // Panel Tabla de Símbolos
        generarTablaSimbolos(tablaSimbolos);
    }

    private String analizarDeclaracionVariable(String linea, Map<String, Simbolo> tablaSimbolos) {
        String[] partes = linea.split("\\s+");

        if (partes.length < 2) {
            return "Declaración incompleta";
        }

        String nombreVariable = partes[0].trim();
        String tipoDato = partes[1].toLowerCase().trim();

        // Validar nombre de variable
        if (!esIdentificadorValido(nombreVariable)) {
            return "Nombre de variable inválido";
        }

        // Validar tipo de dato
        if (!TIPOS_DATO.contains(tipoDato)) {
            return "Tipo de dato no reconocido: " + tipoDato;
        }

        // Determinar tamaño según tipo de dato
        int tamanio = obtenerTamanioPorTipo(tipoDato);
        String valor = "";

        // Extraer valor si existe
        if (partes.length > 2) {
            valor = partes[2];

            // Manejar DUP
            if (linea.toLowerCase().contains("dup")) {
                int cantidad = 0;
                String valorDup = "";

                // Extraer cantidad del DUP
                for (String parte : partes) {
                    if (esConstanteNumerica(parte)) {
                        cantidad = Integer.parseInt(parte);
                    }
                    if (parte.contains("(") && parte.contains(")")) {
                        valorDup = parte;
                    }
                }

                if (cantidad > 0) {
                    valor = cantidad + " dup " + valorDup;
                    tamanio = tamanio * cantidad;
                }
            } else if (!esConstanteNumerica(valor) && !valor.contains("\"") && !valor.contains("'") && !valor.contains("h") && !valor.contains("b")) {
                return "Valor inicial inválido";
            }
        }

        // Agregar a tabla de símbolos
        tablaSimbolos.put(nombreVariable, new Simbolo(nombreVariable, tipoDato, valor, tamanio));

        return null; // Sin errores
    }

    private String validarInstruccion(String linea) {
        String[] partes = linea.trim().split("\\s+", 2);

        if (partes.length == 0) return null;

        String instruccion = partes[0].toLowerCase();

        // Verificar si es una instrucción válida
        if (!INSTRUCCIONES.contains(instruccion) && !PALABRAS_RESERVADAS.contains(instruccion)) {
            return "Instrucción no reconocida: " + instruccion;
        }

        // Validar que tenga operandos si los requiere
        if (partes.length < 2 && requiereOperandos(instruccion)) {
            return "Instrucción requiere operandos";
        }

        return null;
    }

    private boolean requiereOperandos(String instruccion) {
        Set<String> instruccionesSinOperandos = new HashSet<>(Arrays.asList(
                "ret", "nop", "hlt", "clc", "stc", "cli", "sti", "endp", "end"
        ));
        return !instruccionesSinOperandos.contains(instruccion);
    }

    private int obtenerTamanioPorTipo(String tipo) {
        switch (tipo.toLowerCase()) {
            case "db":
            case "byte":
                return 1;
            case "dw":
            case "word":
                return 2;
            case "dd":
            case "dword":
            case "real4":
                return 4;
            case "dq":
            case "qword":
            case "real8":
                return 8;
            case "dt":
            case "tbyte":
            case "real10":
                return 10;
            default:
                return 0;
        }
    }

    private void generarAnalisisSegmentos(List<String> data, List<String> pila, List<String> codigo, Map<Integer, String> analisisLineas) {
        StringBuilder analisis = new StringBuilder();


        analisis.append("            ANÁLISIS DE LÍNEAS Y SEGMENTOS\n");


        // Análisis de líneas
        if (!analisisLineas.isEmpty()) {
            analisis.append("📋 ANÁLISIS DE LÍNEAS:\n");
            analisis.append("─────────────────────────────────────────────────────────\n");
            for (Map.Entry<Integer, String> entry : analisisLineas.entrySet()) {
                analisis.append(String.format("  Línea %d: %s\n", entry.getKey(), entry.getValue()));
            }
            analisis.append("\n");
        }

        // Segmento de datos
        if (!data.isEmpty()) {
            analisis.append("📊 SEGMENTO DE DATOS (.data)\n");
            analisis.append("─────────────────────────────────────────────────────────\n");
            int totalBytes = 0;
            for (String linea : data) {
                if (!linea.startsWith(".") && !linea.trim().isEmpty()) {
                    analisis.append("  • ").append(linea).append("\n");
                    // Calcular tamaño aproximado
                    if (linea.contains("dw")) totalBytes += 2;
                    else if (linea.contains("db") || linea.contains("byte")) totalBytes += 1;
                    else if (linea.contains("dd") || linea.contains("dword")) totalBytes += 4;
                }
            }
            analisis.append("\n  Total estimado: ").append(totalBytes).append(" bytes\n\n");
        }

        // Segmento de pila
        if (!pila.isEmpty()) {
            analisis.append("📚 SEGMENTO DE PILA (.stack)\n");
            analisis.append("─────────────────────────────────────────────────────────\n");
            for (String linea : pila) {
                if (!linea.startsWith(".") && !linea.trim().isEmpty()) {
                    analisis.append("  • ").append(linea).append("\n");
                }
            }
            analisis.append("\n");
        }

        // Segmento de código
        if (!codigo.isEmpty()) {
            analisis.append("⚙️ SEGMENTO DE CÓDIGO (.code)\n");
            analisis.append("─────────────────────────────────────────────────────────\n");
            analisis.append("  Total de líneas de código: ").append(codigo.size()).append("\n\n");
        }

        panelSegmentos.setText(analisis.toString());
    }

    private void generarTablaSimbolos(Map<String, Simbolo> tablaSimbolos) {
        StringBuilder tabla = new StringBuilder();


        tabla.append("                        TABLA DE SÍMBOLOS\n");


        if (tablaSimbolos.isEmpty()) {
            tabla.append("  No se encontraron símbolos en el segmento .data\n");
        } else {
            tabla.append(String.format("%-18s %-12s %-25s %-12s\n",
                    "SÍMBOLO", "TIPO", "VALOR", "TAMAÑO"));
            tabla.append("───────────────────────────────────────────────────────────────────\n");

            for (Simbolo simbolo : tablaSimbolos.values()) {
                String valorMostrar = simbolo.valor.isEmpty() ? "(sin inicializar)" : simbolo.valor;
                if (valorMostrar.length() > 23) {
                    valorMostrar = valorMostrar.substring(0, 20) + "...";
                }

                tabla.append(String.format("%-18s %-12s %-25s %-12s\n",
                        simbolo.nombre,
                        simbolo.tipo.toUpperCase(),
                        valorMostrar,
                        simbolo.tamanio + " bytes"));
            }

            tabla.append("\n");
            int totalBytes = tablaSimbolos.values().stream()
                    .mapToInt(s -> s.tamanio)
                    .sum();
            tabla.append("═══════════════════════════════════════════════════════════════════\n");
            tabla.append("Total de memoria utilizada: ").append(totalBytes).append(" bytes\n");
        }

        panelTablaSimbolos.setText(tabla.toString());
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
        panelSegmentos.setText("");
        panelTablaSimbolos.setText("");
    }
}