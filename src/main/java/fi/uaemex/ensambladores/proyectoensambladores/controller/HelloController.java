package fi.uaemex.ensambladores.proyectoensambladores.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class HelloController {

    @FXML private TextArea panelIzquierdo;
    @FXML private TextArea panelCentral;
    @FXML private TextArea panelDerecho;
    @FXML private TextArea panelSegmentos;
    @FXML private TextArea panelTablaSimbolos;

    // --- DEFINICIONES ---
    private static final Set<String> PALABRAS_RESERVADAS = new HashSet<>(Arrays.asList(
            "flat", "stdcall", "exitprocess", "proto", "dwexitcode",
            "main", "proc", "invoke", "endp", "end", "segment", "ends", "equ", "macro"
    ));

    private static final Set<String> DIRECTIVAS = new HashSet<>(Arrays.asList(
            ".data", ".stack", ".code", ".model", ".386", ".const"
    ));

    private static final Set<String> REGISTROS = new HashSet<>(Arrays.asList(
            "eax", "ebx", "ecx", "edx", "esi", "edi", "ebp", "esp",
            "ax", "bx", "cx", "dx", "si", "di", "bp", "sp",
            "ah", "al", "bh", "bl", "ch", "cl", "dh", "dl",
            "cs", "ds", "es", "fs", "gs", "ss"
    ));

    private static final Set<String> TIPOS_DATO = new HashSet<>(Arrays.asList(
            "db", "dw", "dd", "dq", "dt", "byte", "word", "dword", "qword", "tbyte", "real4", "dup"
    ));

    // Incluimos MOVZX y las instrucciones de tu imagen
    private static final Set<String> INSTRUCCIONES = new HashSet<>(Arrays.asList(
            "mov", "add", "sub", "mul", "div", "inc", "dec",
            "and", "or", "xor", "not", "shl", "shr",
            "jmp", "je", "jne", "jg", "jl", "jge", "jle", "jz", "jnz",
            "call", "ret", "push", "pop",
            "cmp", "test", "lea", "int", "nop", "loop", "movzx"
    ));

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

    // --- MÉTODOS FXML ---

    @FXML
    private void abrirArchivo(ActionEvent event) {
        FileChooser selectorArchivo = new FileChooser();
        selectorArchivo.setTitle("Abrir Archivo");
        selectorArchivo.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM y TXT", "*.asm", "*.txt"));
        Stage stage = (Stage) panelIzquierdo.getScene().getWindow();
        File archivo = selectorArchivo.showOpenDialog(stage);
        if (archivo != null) {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(archivo.toURI()));
                panelIzquierdo.setText(new String(bytes, StandardCharsets.UTF_8));
            } catch (IOException e) { panelIzquierdo.setText("Error: " + e.getMessage()); }
        }
    }

    @FXML
    private void AnalizarTexto() {
        String text = panelIzquierdo.getText();
        LimpiarPaneles();

        if (text == null || text.trim().isEmpty()) {
            panelCentral.setText("Código vacío");
            return;
        }

        String[] lineasRaw = text.split("\\r?\\n");

        // 1. CONSOLIDACIÓN DE SEGMENTOS (FASE 4 - Requisito 1)
        List<String> bloqueData = new ArrayList<>();
        List<String> bloqueStack = new ArrayList<>();
        List<String> bloqueCode = new ArrayList<>();

        String segmentoActual = "ninguno";

        // Tokenización global para reportes
        StringBuilder tokensSalida = new StringBuilder();
        Set<String> dirs = new LinkedHashSet<>(), res = new LinkedHashSet<>(), regs = new LinkedHashSet<>();
        Set<String> tips = new LinkedHashSet<>(), insts = new LinkedHashSet<>(), nums = new LinkedHashSet<>(), etiqs = new LinkedHashSet<>();

        int nLinea = 0;
        for (String linea : lineasRaw) {
            nLinea++;
            String limpia = limpiarComentarios(linea).trim();
            if (limpia.isEmpty()) continue;

            String lower = limpia.toLowerCase();

            // Detección de cambio de segmento
            if (lower.contains(".data")) { segmentoActual = "data"; dirs.add(".data"); }
            else if (lower.contains(".stack")) { segmentoActual = "stack"; dirs.add(".stack"); }
            else if (lower.contains(".code")) { segmentoActual = "code"; dirs.add(".code"); }

            // Agrupar líneas según segmento actual
            if (!limpia.startsWith(".")) {
                switch (segmentoActual) {
                    case "data": bloqueData.add(limpia); break;
                    case "stack": bloqueStack.add(limpia); break;
                    case "code": bloqueCode.add(limpia); break;
                }
            }

            // Recolectar tokens para panel central/derecho
            procesarTokens(limpia, tokensSalida, dirs, res, regs, tips, insts, nums, etiqs);
        }

        // 2. ANÁLISIS DE DATOS (Construir Tabla de Símbolos Primero)
        Map<String, Simbolo> tablaSimbolos = new LinkedHashMap<>();
        Map<String, String> erroresData = new LinkedHashMap<>(); // K=Contenido, V=Error

        for (String linea : bloqueData) {
            String err = analizarDeclaracionVariable(linea, tablaSimbolos);
            if (err != null) erroresData.put(linea, err);
        }

        // 3. ANÁLISIS DE CÓDIGO (Validación Estricta usando Tabla de Símbolos)
        Map<String, String> erroresCode = new LinkedHashMap<>(); // K=Instrucción, V=Error

        for (String linea : bloqueCode) {
            // Separar etiqueta de instrucción si existe
            String instruccion = linea;
            if (linea.contains(":")) {
                String[] parts = linea.split(":", 2);
                instruccion = (parts.length > 1) ? parts[1].trim() : "";
            }

            if (!instruccion.isEmpty()) {
                // AQUÍ SE APLICA LA VALIDACIÓN FASE 4
                String err = validarInstruccionFase4(instruccion, tablaSimbolos);
                if (err != null) erroresCode.put(linea, err);
            }
        }

        // --- SALIDAS A PANELES ---
        panelCentral.setText(tokensSalida.toString());
        generarReporte(dirs, res, regs, insts, tips, nums, etiqs);
        generarTablaSimbolos(tablaSimbolos);

        // Reporte de Análisis Consolidado (Fase 4)
        generarReporteSegmentos(bloqueData, bloqueStack, bloqueCode, erroresData, erroresCode);
    }

    // --- LÓGICA DE VALIDACIÓN FASE 4 (LO IMPORTANTE) ---

    private String validarInstruccionFase4(String linea, Map<String, Simbolo> tablaSimbolos) {
        // Dividir mnemónico y operandos
        String[] partes = linea.trim().split("\\s+", 2);
        String mnemonico = partes[0].toLowerCase();

        if (!INSTRUCCIONES.contains(mnemonico) && !PALABRAS_RESERVADAS.contains(mnemonico)) {
            return "Instrucción desconocida";
        }

        // Si no tiene operandos (NOP)
        if (partes.length < 2) {
            if (requiereOperandos(mnemonico)) return "Faltan operandos";
            return null; // Es correcto para NOP, RET, etc.
        }

        // Analizar operandos: "ax, bx" -> ["ax", "bx"]
        String[] ops = partes[1].split(",");
        for (int i=0; i<ops.length; i++) ops[i] = ops[i].trim();

        String op1 = ops[0];
        String op2 = (ops.length > 1) ? ops[1] : null;

        boolean op1EsReg = esRegistro(op1);
        boolean op1EsMem = esMemoria(op1, tablaSimbolos);
        boolean op1EsConst = esConstanteNumerica(op1);

        boolean op2EsReg = (op2 != null) && esRegistro(op2);
        boolean op2EsMem = (op2 != null) && esMemoria(op2, tablaSimbolos);
        boolean op2EsConst = (op2 != null) && esConstanteNumerica(op2);

        // --- REGLAS ESPECÍFICAS DE TU IMAGEN ---

        switch (mnemonico) {
            case "mov":
                if (op2 == null) return "MOV requiere 2 operandos";
                if (op1EsConst) return "Destino no puede ser constante";
                if (op1EsMem && op2EsMem) return "No se permite Memoria a Memoria";
                // Correcto: Reg-Reg, Reg-Mem, Mem-Reg, Reg-Const, Mem-Const
                break;

            case "movzx": // Mover con extensión de ceros
                if (op2 == null) return "MOVZX requiere 2 operandos";
                if (!op1EsReg) return "Destino MOVZX debe ser registro";
                if (op1EsConst) return "Destino inválido";
                // Típicamente el destino debe ser mayor que el origen (ej: movzx ax, bl)
                break;

            case "add":
            case "sub":
            case "and":
            case "or":
            case "xor":
                if (op2 == null) return mnemonico.toUpperCase() + " requiere 2 operandos";
                if (op1EsConst) return "Destino no puede ser constante";
                if (op1EsMem && op2EsMem) return "No se permite Memoria a Memoria";
                break;

            case "shl": // Desplazamiento
            case "shr":
                if (op2 == null) return mnemonico.toUpperCase() + " requiere 2 operandos";
                if (op1EsConst) return "Destino no puede ser constante";
                // En 8086 el segundo operando debe ser 1 o CL, en modernos acepta inmediatos
                break;

            case "pop":
                if (ops.length > 1) return "POP solo acepta 1 operando";
                if (op1EsConst) return "No se puede hacer POP a una constante";
                // POP CS es ilegal, pero POP DS es valido.
                break;

            case "jz":
            case "jnz":
            case "jmp":
            case "je":
                // Saltos toman 1 operando (etiqueta)
                if (ops.length > 1) return "Saltos solo aceptan 1 destino";
                // Aquí podríamos validar si la etiqueta existe, pero requeriría recolectarlas antes.
                break;

            case "nop":
                if (partes.length > 1) return "NOP no lleva operandos";
                break;
        }

        return null; // Sin errores detectados
    }

    // --- MÉTODOS AUXILIARES DE VALIDACIÓN ---

    private boolean esRegistro(String s) { return REGISTROS.contains(s.toLowerCase()); }

    private boolean esMemoria(String s, Map<String, Simbolo> tabla) {
        // Es memoria si está en la tabla de variables o tiene corchetes [eax]
        String sClean = s.replaceAll("[\\[\\]]", "");
        return tabla.containsKey(sClean) || s.contains("[");
    }

    private String analizarDeclaracionVariable(String linea, Map<String, Simbolo> tabla) {
        String[] partes = linea.trim().split("\\s+", 3);
        if (partes.length < 2) return "Declaración incompleta";

        String nombre = partes[0].trim();
        String tipo = partes[1].toLowerCase();

        if (!nombre.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) return "Nombre inválido";
        if (!TIPOS_DATO.contains(tipo)) return "Tipo desconocido";

        int tam = obtenerTamanio(tipo);
        String val = (partes.length > 2) ? partes[2] : "?";

        // Calcular tamaño real si hay DUP o String
        if (val.toLowerCase().contains("dup")) {
            try { tam *= Integer.parseInt(val.split("(?i)dup")[0].trim()); } catch(Exception e){}
        } else if (val.startsWith("'") || val.startsWith("\"")) {
            tam = Math.max(0, val.length()-2);
        }

        tabla.put(nombre, new Simbolo(nombre, tipo, val, tam));
        return null;
    }

    private void generarReporteSegmentos(List<String> data, List<String> stack, List<String> code,
                                         Map<String, String> errData, Map<String, String> errCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("ESTADO DEL ANÁLISIS (POR SEGMENTOS)\n\n");

        // Reporte DATA
        sb.append("📊 SEGMENTO DE DATOS (.data)\n");
        sb.append("----------------------------\n");
        if (data.isEmpty()) sb.append("  (Vacío)\n");
        else {
            for (String l : data) {
                if (errData.containsKey(l)) sb.append("  [X] ").append(l).append(" -> ERROR: ").append(errData.get(l)).append("\n");
                else sb.append("  [OK] ").append(l).append("\n");
            }
        }
        sb.append("\n");

        // Reporte STACK
        sb.append("📚 SEGMENTO DE PILA (.stack)\n");
        sb.append("----------------------------\n");
        sb.append("  Líneas detectadas: ").append(stack.size()).append("\n\n");

        // Reporte CODE
        sb.append("⚙️ SEGMENTO DE CÓDIGO (.code)\n");
        sb.append("----------------------------\n");
        if (code.isEmpty()) sb.append("  (Vacío)\n");
        else {
            for (String l : code) {
                if (errCode.containsKey(l)) sb.append("  [X] ").append(l).append(" -> ERROR: ").append(errCode.get(l)).append("\n");
                else sb.append("  [OK] ").append(l).append("\n");
            }
        }

        panelSegmentos.setText(sb.toString());
    }

    // --- UTILIDADES RESTAURADAS Y TABLA CENTRADA ---

    private void generarTablaSimbolos(Map<String, Simbolo> tabla) {
        StringBuilder sb = new StringBuilder();
        int wNom=20, wTip=12, wTam=12, wVal=20;

        sb.append("TABLA DE SÍMBOLOS\n\n");
        sb.append(centrar("NOMBRE", wNom)).append(centrar("TIPO", wTip))
                .append(centrar("TAMAÑO", wTam)).append(centrar("VALOR", wVal)).append("\n");
        sb.append("-".repeat(wNom+wTip+wTam+wVal)).append("\n");

        for (Simbolo s : tabla.values()) {
            sb.append(centrar(s.nombre, wNom)).append(centrar(s.tipo, wTip))
                    .append(centrar(s.tamanio+" B", wTam)).append(centrar(s.valor, wVal)).append("\n");
        }
        panelTablaSimbolos.setText(sb.toString());
    }

    private String centrar(String s, int w) {
        if (s==null) s="-";
        if (s.length()>w-2) s=s.substring(0, w-3)+"..";
        int pad = w - s.length();
        return " ".repeat(pad/2) + s + " ".repeat(pad - pad/2);
    }

    private void procesarTokens(String linea, StringBuilder sb, Set<String> d, Set<String> r, Set<String> rg,
                                Set<String> t, Set<String> i, Set<String> n, Set<String> e) {
        String[] toks = linea.replace(",", " ").split("\\s+");
        for (String tok : toks) {
            if(tok.isEmpty()) continue;
            String clean = tok.replaceAll("[\\[\\]]", "");
            String low = clean.toLowerCase();
            sb.append(clean).append("\n");

            if (low.startsWith(".")) d.add(low);
            else if (INSTRUCCIONES.contains(low)) i.add(low);
            else if (REGISTROS.contains(low)) rg.add(low);
            else if (TIPOS_DATO.contains(low)) t.add(low);
            else if (esConstanteNumerica(low)) n.add(low);
            else if (linea.contains(":") && !INSTRUCCIONES.contains(low)) e.add(clean); // Etiqueta simple
            else r.add(clean); // Palabras grales
        }
    }

    private void generarReporte(Set<String> d, Set<String> res, Set<String> reg, Set<String> in,
                                Set<String> tip, Set<String> num, Set<String> et) {
        StringBuilder sb = new StringBuilder();
        appendSec(sb, "DIRECTIVAS", d); appendSec(sb, "REGISTROS", reg);
        appendSec(sb, "INSTRUCCIONES", in); appendSec(sb, "TIPOS", tip);
        appendSec(sb, "CONSTANTES", num); appendSec(sb, "ETIQUETAS", et);
        panelDerecho.setText(sb.toString());
    }

    private void appendSec(StringBuilder sb, String t, Set<String> s) {
        if(!s.isEmpty()) { sb.append(t).append(":\n"); s.forEach(x->sb.append(" - ").append(x).append("\n")); sb.append("\n"); }
    }

    @FXML
    private void LimpiarPaneles() {
        panelCentral.clear(); panelDerecho.clear(); panelSegmentos.clear(); panelTablaSimbolos.clear();
    }

    private String limpiarComentarios(String l) { return l.contains(";") ? l.substring(0, l.indexOf(";")) : l; }
    private boolean esConstanteNumerica(String s) { return s.matches("(?i)(\\d+|[0-9a-f]+h|0x[0-9a-f]+|[01]+b)"); }
    private boolean requiereOperandos(String i) { return !Arrays.asList("nop","ret","cli","sti").contains(i); }
    private int obtenerTamanio(String t) {
        if(t.contains("byte")||t.equals("db")) return 1;
        if(t.contains("word")||t.equals("dw")) return 2;
        if(t.contains("dword")||t.equals("dd")) return 4;
        return 0;
    }
}