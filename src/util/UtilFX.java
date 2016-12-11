package util;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.concurrent.locks.LockSupport;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;

public class UtilFX {

    private UtilFX() {
    }

    public static void showInfoTip(MouseEvent e, String infoText, double fontSize, double hideAfterMouseTravelDistance, double timeoutSecs) {
        Tooltip info = new Tooltip();
        info.setText(infoText);
        if (fontSize > 0) {
            info.setFont(Font.font(fontSize));
        }
        Node node = (Node) e.getSource();
        info.show(node, e.getScreenX() + 5, e.getScreenY() + 5);
        new Thread(() -> {
            LockSupport.parkNanos(Math.round(timeoutSecs * 1_000_000_000L));
            Platform.runLater(() -> info.hide());
        }).start();
        new Thread(() -> {
            Point2D infoXY = new Point2D(info.getX(), info.getY());
            Point m = MouseInfo.getPointerInfo().getLocation();
            Point2D mouseXY = new Point2D(m.getX(), m.getY());
            while (mouseXY.distance(infoXY) < hideAfterMouseTravelDistance && info.isShowing()) {
                LockSupport.parkNanos(100_000_000);
                m = MouseInfo.getPointerInfo().getLocation();
                mouseXY = new Point2D(m.getX(), m.getY());
            }
            if (info.isShowing()) {
                Platform.runLater(() -> info.hide());
            }
        }).start();
    }

    private static final double DEFAULT_ACTIVE_DISTANCE = 50;
    private static final double DEFAULT_TIMEOUT = 2;

    public static void showInfoTip(MouseEvent e, String infoText, double fontSize) {
        showInfoTip(e, infoText, fontSize, DEFAULT_ACTIVE_DISTANCE, DEFAULT_TIMEOUT);
    }

    public static void showInfoTip(MouseEvent e, String infoText) {
        showInfoTip(e, infoText, -1);
    }
}
