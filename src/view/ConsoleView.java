package view;

public class ConsoleView extends DefaultView {

    private int min;
    private int max;
    private int progress;
    private int scaledProgress = 0;
    private final int WIDTH = 80;

    @Override
    public void initProgress(String message, int min, int max) {
        super.initProgress(message, min, max);
        System.out.println("The Controller Compiler is running right now, please wait.");
        this.min = min;
        this.max = max -1; // TODO fix -1 offset
        this.progress = min;
    }

    @Override
    public void publishProgressIncrement(int increment, String note) {
        super.publishProgressIncrement(increment, note);
        progress += increment;
        assert progress <= max;
        if (progress <= max) {
            int newScaledProgress = scale(progress);
            for (int i = scaledProgress; i < newScaledProgress; i++) {
                System.out.print(".");
            }
            scaledProgress = newScaledProgress;
            if (progress == max) {
                System.out.println();
                System.out.println("The compilation process finished.");
            }
        }
    }

    private int scale(int prog) {
        return (int) ((progress - min) * WIDTH / (max - min));
    }
}
