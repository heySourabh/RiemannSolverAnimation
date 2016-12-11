package util;

import java.util.Arrays;
import javafx.animation.AnimationTimer;

public class FrameRateCalculator {

    private final static int DEFAULT_SAMPLE_SIZE = 30;
    private int arrayPointer = 0;
    private long prevTime = 0;
    private final long[] frame_dt_nanos;

    public FrameRateCalculator() {
        this(DEFAULT_SAMPLE_SIZE);
    }

    public FrameRateCalculator(int sampleSize) {
        if (sampleSize < 5) {
            sampleSize = DEFAULT_SAMPLE_SIZE;
        }
        frame_dt_nanos = new long[sampleSize];
        animationThread().start();
    }

    private AnimationTimer animationThread() {
        AnimationTimer animTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (prevTime == 0) {
                    prevTime = now;
                    return;
                }
                arrayPointer = arrayPointer % frame_dt_nanos.length;
                frame_dt_nanos[arrayPointer] = now - prevTime;
                prevTime = now;
                arrayPointer++;
            }
        };
        return animTimer;
    }

    public double getFramesPerSecond() {
        double avg_dt_nanos = Arrays.stream(frame_dt_nanos).average().orElse(-1);
        double avg_dt_secs = avg_dt_nanos * 1e-9;
        return 1.0 / avg_dt_secs;
    }
}
