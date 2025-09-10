module fi.uaemex.ensambladores.proyectoensambladores {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    opens fi.uaemex.ensambladores.proyectoensambladores to javafx.fxml;
    exports fi.uaemex.ensambladores.proyectoensambladores;
}