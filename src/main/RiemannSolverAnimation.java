package main;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import util.ScreenshotUtility;
import util.FrameRateCalculator;

public class RiemannSolverAnimation extends Application {

    final static double WIDTH = 800;
    final static double HEIGHT = 800;
    final static double SIDE_OFFSET = 50;
    final static double BOTTOM_OFFSET = 50;
    final static double TOP_OFFSET = 10;
    final static double STOP_TIME = 5.0;
    static double[] dx_dt = {-0.25, -0.5, 1.0, 1.5};
    String titleStr = "*** Pause/play:SPACE; Reverse:R; Change wave speeds:DOUBLE_CLICK (Programmed by Sourabh Bhat) ***";

    boolean playing = false;

    public static void main(String[] args) {
        Arrays.sort(dx_dt);
        System.out.println(Arrays.toString(dx_dt));
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Group root = new Group();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
        startStageTitleAnimation(primaryStage);
        //ScreenshotUtility.screenshotThread(scene, 6).start();
        FrameRateCalculator frc = new FrameRateCalculator();
        new Thread(() -> {
            while (primaryStage.isShowing()) {
                LockSupport.parkNanos(1_000_000_000);
                System.out.printf("FPS = %.2f\n", frc.getFramesPerSecond());
            }
        }).start();

        scene.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                setNew_dx_dt();
            }
        });

        SimpleDoubleProperty timeProperty = new SimpleDoubleProperty(0);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Node axes1 = createAxis(1, "x", "t");
                Node waves = createWaves(timeProperty.doubleValue());
                Node axes2 = createAxis(2, "x", "U");
                Node distribution = createDistribution(timeProperty.doubleValue());
                root.getChildren().clear();
                root.getChildren().addAll(waves, axes1, distribution, axes2);
            }
        };
        timer.start();

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(timeProperty, 0)),
                new KeyFrame(Duration.seconds(5), new KeyValue(timeProperty, STOP_TIME))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        //timeline.play();

        scene.setOnKeyTyped(e -> {
            if (e.getCharacter().equals(" ")) {
                if (playing) {
                    timeline.pause();
                } else {
                    timeline.play();
                }
                playing = !playing;
            }

            if (e.getCharacter().equalsIgnoreCase("r")) {
                timeline.setRate(timeline.getRate() * -1);
            }
        });
    }

    private Group createWaves(double time) {
        double timeRatio = time / STOP_TIME;
        double xZero = WIDTH / 2;
        double yZero = HEIGHT / 2.0 - 2.0 * BOTTOM_OFFSET;
        double maxHeight = HEIGHT / 2.0 - 2.0 * BOTTOM_OFFSET - 2.0 * TOP_OFFSET - 50;
        double yTime = yZero - timeRatio * maxHeight;

        Polygon leftState = new Polygon(
                SIDE_OFFSET, yZero,
                xZero, yZero,
                xZero + (yZero - yTime) * dx_dt[0], yTime,
                SIDE_OFFSET, yTime
        );
        leftState.setFill(getColor(0.0));
        leftState.setOnMousePressed(e -> showInfo(e, "UL"));

        Polygon rightState = new Polygon(
                xZero + WIDTH / 2 - SIDE_OFFSET, yZero,
                xZero, yZero,
                xZero + (yZero - yTime) * dx_dt[dx_dt.length - 1], yTime,
                xZero + WIDTH / 2 - SIDE_OFFSET, yTime
        );
        rightState.setFill(getColor(1.0));
        rightState.setOnMousePressed(e -> showInfo(e, "UR"));

        Group wavesGroup = new Group(leftState, rightState);

        // Intermediate waves
        for (int i = 0; i < dx_dt.length - 1; i++) {
            Polygon state = new Polygon(
                    xZero, yZero,
                    xZero + (yZero - yTime) * dx_dt[i], yTime,
                    xZero + (yZero - yTime) * dx_dt[i + 1], yTime
            );
            double colorRatio = (i + 1.0) / (dx_dt.length);
            state.setFill(getColor(colorRatio));
            final String infoString = "U*" + (i + 1);
            state.setOnMousePressed(e -> showInfo(e, infoString));
            wavesGroup.getChildren().add(state);
        }

        return wavesGroup;
    }

    private Group createDistribution(double time) {
        double timeRatio = time / STOP_TIME;
        double xZero = WIDTH / 2;
        double yZero = HEIGHT - BOTTOM_OFFSET;
        double topLimit = HEIGHT / 2.0 - 50;
        double maxHeight = HEIGHT / 2.0 - 2.0 * BOTTOM_OFFSET - 2.0 * TOP_OFFSET - 50;
        double yTime = yZero - timeRatio * maxHeight;
        double du = (yZero - topLimit - 50) / (dx_dt.length + 1);

        Polygon leftState = new Polygon(
                SIDE_OFFSET, yZero,
                xZero + (yZero - yTime) * dx_dt[0], yZero,
                xZero + (yZero - yTime) * dx_dt[0], topLimit + 50.0,
                SIDE_OFFSET, topLimit + 50.0
        );
        leftState.setFill(getColor(0));
        leftState.setOnMousePressed(e -> showInfo(e, "UL"));

        Polygon rightState = new Polygon(
                xZero + WIDTH / 2 - SIDE_OFFSET, yZero,
                xZero + (yZero - yTime) * dx_dt[dx_dt.length - 1], yZero,
                xZero + (yZero - yTime) * dx_dt[dx_dt.length - 1], yZero - du,
                xZero + WIDTH / 2 - SIDE_OFFSET, yZero - du
        );
        rightState.setFill(getColor(1.0));
        rightState.setOnMousePressed(e -> showInfo(e, "UR"));

        Group distGroup = new Group(leftState, rightState);

        // Intermediate states
        for (int i = 0; i < dx_dt.length - 1; i++) {
            Polygon state = new Polygon(
                    xZero + (yZero - yTime) * dx_dt[i], yZero,
                    xZero + (yZero - yTime) * dx_dt[i], yZero - (dx_dt.length - i) * du,
                    xZero + (yZero - yTime) * dx_dt[i + 1], yZero - (dx_dt.length - i) * du,
                    xZero + (yZero - yTime) * dx_dt[i + 1], yZero
            );
            double colorRatio = (i + 1.0) / (dx_dt.length);
            state.setFill(getColor(colorRatio));
            final String infoString = "U*" + (i + 1);
            state.setOnMousePressed(e -> showInfo(e, infoString));
            distGroup.getChildren().add(state);
        }

        return distGroup;
    }

    private Color getColor(double colorRatio) {
        double redL = 1.0;
        double greenL = 0.0;
        double blueL = 0.0;
        double redR = 0.0;
        double greenR = 0.0;
        double blueR = 1.0;

        return new Color(redL + (redR - redL) * colorRatio,
                greenL + (greenR - greenL) * colorRatio,
                blueL + (blueR - blueL) * colorRatio, 1.0);
    }

    private Group createAxis(int id, String xLabel, String yLabel) {
        double xZero = WIDTH / 2;
        double yZero = id == 1 ? HEIGHT / 2.0 - 2.0 * BOTTOM_OFFSET : HEIGHT - BOTTOM_OFFSET;
        double topLimit = id == 1 ? TOP_OFFSET : HEIGHT / 2.0 - 50;
        Path axisX = createArrow(new Point2D(SIDE_OFFSET, yZero), new Point2D(xZero * 2 - SIDE_OFFSET, yZero), 20);
        axisX.setStrokeWidth(2.0);
        Path axisY = createArrow(new Point2D(xZero, yZero), new Point2D(xZero, topLimit), 20);
        axisY.setStrokeWidth(2.0);

        Font labelFont = Font.font("serif", FontPosture.ITALIC, 20);
        Text xLabelText = new Text(WIDTH - 80, yZero + 25, xLabel);
        xLabelText.setFont(labelFont);

        Text yLabelText = new Text(xZero + 15, topLimit + 25, yLabel);
        yLabelText.setFont(labelFont);

        Group axes = new Group(axisX, axisY, xLabelText, yLabelText);
        return axes;
    }

    private Path createArrow(Point2D tail, Point2D head, double headSize) {
        final double arrowHeadAngle = 20;
        double arrowLength = tail.distance(head);
        double angleDegrees = Math.toDegrees(Math.atan2(head.getY() - tail.getY(), head.getX() - tail.getX()));

        MoveTo startPoint = new MoveTo(0, 0);
        LineTo arrowLine = new LineTo(arrowLength, 0);
        MoveTo arrowHeadStart = new MoveTo(arrowLength - headSize, Math.tan(Math.toRadians(arrowHeadAngle)) * headSize);
        LineTo headLine1 = new LineTo(arrowLength, 0);
        LineTo headLine2 = new LineTo(arrowLength - headSize, -Math.tan(Math.toRadians(arrowHeadAngle)) * headSize);

        Path arrow = new Path(startPoint, arrowLine, arrowHeadStart, headLine1, headLine2);
        arrow.getTransforms().add(Transform.translate(tail.getX(), tail.getY()));
        arrow.getTransforms().add(Transform.rotate(angleDegrees, 0, 0));

        arrow.setStrokeLineCap(StrokeLineCap.BUTT);

        return arrow;
    }

    private double[] parseAndScaleToArray(String dx_dt_list, double[] defaultArray) {
        if (dx_dt_list.length() < 2) {
            return defaultArray;
        }
        String csv = dx_dt_list.trim().substring(1, dx_dt_list.length() - 1);
        List<Double> list;
        double max;
        try {
            list = Arrays.stream(csv.split(","))
                    .map(s -> Double.parseDouble(s.trim()))
                    .collect(Collectors.toList());
            max = list.stream()
                    .mapToDouble(d -> Math.abs(d))
                    .max()
                    .getAsDouble();
        } catch (Exception ex) {
            return defaultArray;
        }
        double[] double_list = new double[list.size()];
        for (int i = 0; i < double_list.length; i++) {
            double_list[i] = list.get(i) / max * 1.5;
        }
        System.out.println(Arrays.toString(double_list));

        return double_list;
    }

    private void setNew_dx_dt() {
        // Creating a dummy Stage to stay always on top and act as a owner for the dialog
        // so that the dialog can be non-modal and still remain on top of animating window
        Stage s = new Stage(StageStyle.TRANSPARENT);
        s.setScene(new Scene(new Group(), 1, 1, Color.TRANSPARENT));
        s.setAlwaysOnTop(true);
        s.setResizable(false);
        s.show();
        // End of creation of dummy Stage

        String dx_dt_list = Arrays.toString(dx_dt);
        TextInputDialog get_dx_dt = new TextInputDialog(dx_dt_list);
        get_dx_dt.initModality(Modality.WINDOW_MODAL);
        get_dx_dt.initOwner(s);
        TextField inputField = get_dx_dt.getEditor();
        inputField.setPrefColumnCount(20);
        inputField.setFont(Font.font(null, FontWeight.BOLD, FontPosture.REGULAR, 20));
        inputField.textProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    set_dx_dt(newValue);
                });
        get_dx_dt.setTitle("slope: dx/dt");
        get_dx_dt.setHeaderText("Enter list of wave speeds dx/dt");

        get_dx_dt.showAndWait();
        s.close();
    }

    private void set_dx_dt(String dx_dt_list) {
        double[] dx_dt_local = parseAndScaleToArray(dx_dt_list, dx_dt);
        Arrays.sort(dx_dt_local);
        RiemannSolverAnimation.dx_dt = dx_dt_local;
    }

    private void startStageTitleAnimation(Stage primaryStage) {
        new Thread(() -> {
            while (primaryStage.isShowing()) {
                titleStr = titleStr.substring(1) + titleStr.charAt(0);
                setTitle(primaryStage, titleStr);
                LockSupport.parkNanos(500_000_000);
            }
        }).start();
    }

    private void setTitle(Stage stage, String title) {
        Platform.runLater(() -> stage.setTitle(title));
    }

    private void showInfo(MouseEvent e, String infoText) {
        Tooltip info = new Tooltip();
        info.setText(infoText);
        Node node = (Node) e.getSource();
        info.show(node, e.getScreenX() + 5, e.getScreenY() + 5);
        new Thread(() -> {
            LockSupport.parkNanos(1_000_000_000);
            Platform.runLater(() -> info.hide());
        }).start();
    }
}
