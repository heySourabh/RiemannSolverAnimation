package screenshots;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javafx.animation.AnimationTimer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotResult;
import javafx.scene.image.WritableImage;
import javafx.util.Callback;
import javax.imageio.ImageIO;

public class ScreenshotUtility {

    private static long screenshotCallCount = 0;
    private static int frameNumber = 0;

    public static AnimationTimer screenshotThread(Scene scene, final int interval) {
        AnimationTimer captureAnimation = new AnimationTimer() {
            @Override
            public void handle(long now) {
                screenshotCallCount++;
                if (screenshotCallCount % interval == 0) {
                    System.out.println("Capturing frame: " + frameNumber);

                    captureScene(scene, frameNumber);
                    frameNumber++;
                }
            }
        };
        return captureAnimation;
    }

    private static void captureScene(Scene scene, int id) {
        scene.snapshot(processSnapshotResult(id), null);
    }

    private static Callback<SnapshotResult, Void> processSnapshotResult(int id) {
        return (SnapshotResult snapshotResult) -> {
            WritableImage sceneImg = snapshotResult.getImage();
            new Thread(() -> {
                BufferedImage img = SwingFXUtils.fromFXImage(sceneImg, null);
                try {
                    ImageIO.write(img, "png", new File(String.format("capture%05d.png", id)));
                } catch (IOException ex) {
                    System.out.println("Unable to save image!");
                }
            }).start();

            return null;
        };
    }
}
