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

public class HelloController {


    @FXML
    private TextArea panelIzquierdo;
    @FXML
    private TextArea panelCentral;
    @FXML
    private TextArea panelDerecho;


    @FXML
    private void abrirArchivo (ActionEvent event) {
        FileChooser selectorArchivo = new FileChooser();
        selectorArchivo.setTitle("Abrir Archivo de Texto");

        // Filtrar para que solo se muestren archivos .txt
        FileChooser.ExtensionFilter filtroTxt = new FileChooser.ExtensionFilter("Archivos de Texto (*.txt)", "*.txt");
        selectorArchivo.getExtensionFilters().add(filtroTxt);

        // Obtener el Stage (la ventana principal) para mostrar el diálogo
        Stage stage = (Stage) panelIzquierdo.getScene().getWindow();
        File archivo = selectorArchivo.showOpenDialog(stage);

        // Si el usuario seleccionó un archivo, lo leemos y lo mostramos
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

        panelCentral.setText(textoResultado.toString());
    }
    @FXML
    private void Limpiar(){
        panelIzquierdo.setText("");
        panelCentral.setText("");
    }

}
