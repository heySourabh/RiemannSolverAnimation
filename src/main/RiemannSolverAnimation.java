package main;

import java.util.Arrays;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.util.Duration;
import screenshots.ScreenshotUtility;

public class RiemannSolverAnimation extends Application {

    final double WIDTH = 800;
    final double HEIGHT = 300;
    final double SIDE_OFFSET = 50;
    final double BOTTOM_OFFSET = 50;
    final double TOP_OFFSET = 10;
    final double STOP_TIME = 5.0;
    final static double[] dx_dt = {-1.5, -1, -0.5, 0.5, 1.5};

    boolean playing = true;

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
        ScreenshotUtility.screenshotThread(scene, 10).start();

        SimpleDoubleProperty timeProperty = new SimpleDoubleProperty(0);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Node axes = createAxis();
                Node waves = createWaves(timeProperty.doubleValue());
                root.getChildren().clear();
                root.getChildren().addAll(waves, axes);
            }
        };
        timer.start();

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(timeProperty, 0)),
                new KeyFrame(Duration.seconds(5), new KeyValue(timeProperty, STOP_TIME))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

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
        double yZero = HEIGHT - BOTTOM_OFFSET;
        double maxHeight = HEIGHT - BOTTOM_OFFSET - TOP_OFFSET - 50;
        double yTime = yZero - timeRatio * maxHeight;

        double redL = 1.0;
        double greenL = 0.0;
        double blueL = 0.0;
        double redR = 0.0;
        double greenR = 0.0;
        double blueR = 1.0;

        Polygon leftState = new Polygon(
                SIDE_OFFSET, yZero,
                xZero, yZero,
                xZero + (yZero - yTime) * dx_dt[0], yTime,
                SIDE_OFFSET, yTime
        );
        leftState.setFill(new Color(redL, greenL, blueL, 1.0));

        Polygon rightState = new Polygon(
                xZero + WIDTH / 2 - SIDE_OFFSET, yZero,
                xZero, yZero,
                xZero + (yZero - yTime) * dx_dt[dx_dt.length - 1], yTime,
                xZero + WIDTH / 2 - SIDE_OFFSET, yTime
        );
        rightState.setFill(new Color(redR, greenR, blueR, 1.0));

        Group wavesGroup = new Group(leftState, rightState);

        // Intermediate waves
        for (int i = 0; i < dx_dt.length - 1; i++) {
            Polygon state = new Polygon(
                    xZero, yZero,
                    xZero + (yZero - yTime) * dx_dt[i], yTime,
                    xZero + (yZero - yTime) * dx_dt[i + 1], yTime
            );
            double colorRatio = (i + 1.0) / (dx_dt.length);
            state.setFill(new Color(redL + (redR - redL) * colorRatio,
                    greenL + (greenR - greenL) * colorRatio,
                    blueL + (blueR - blueL) * colorRatio, 1.0));
            wavesGroup.getChildren().add(state);
        }

        return wavesGroup;
    }

    private Group createAxis() {
        double xZero = WIDTH / 2;
        double yZero = HEIGHT - BOTTOM_OFFSET;
        Path axisX = createArrow(new Point2D(SIDE_OFFSET, yZero), new Point2D(xZero * 2 - SIDE_OFFSET, yZero), 20);
        axisX.setStrokeWidth(2.0);
        Path axisY = createArrow(new Point2D(xZero, yZero), new Point2D(xZero, TOP_OFFSET), 20);
        axisY.setStrokeWidth(2.0);

        Group axes = new Group(axisX, axisY);
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
}
