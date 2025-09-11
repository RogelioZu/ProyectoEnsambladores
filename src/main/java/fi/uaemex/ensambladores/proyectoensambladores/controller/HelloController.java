package fi.uaemex.ensambladores.proyectoensambladores.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class HelloController {


    @FXML
    private TextArea panelIzquierdo;
    @FXML
    private TextArea panelDerecho;


    @FXML
    private void AnalizarTexto() {


        String text = panelIzquierdo.getText();
        panelDerecho.clear();

        if(text == null || text.trim().isEmpty()){
            System.out.println("El area de texto esta vacia");
            return;
        }
        
        String[] lineas = text.split("\\r?\\n"); 
        StringBuilder textoResultado = new StringBuilder();

        for (String linea : lineas) {
            if (linea.trim().startsWith(";")) {
                continue;
            }
            String[] palabras = linea.split("\\s+");
            for (String palabra : palabras) {
                textoResultado.append(palabra).append("\n");
            }
        }

        panelDerecho.setText(textoResultado.toString());
    }
}
