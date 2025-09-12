module fi.uaemex.ensambladores.proyectoensambladores {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires java.desktop;

    opens fi.uaemex.ensambladores.proyectoensambladores to javafx.fxml;
    exports fi.uaemex.ensambladores.proyectoensambladores;
    exports fi.uaemex.ensambladores.proyectoensambladores.controller;
    opens fi.uaemex.ensambladores.proyectoensambladores.controller to javafx.fxml;
}