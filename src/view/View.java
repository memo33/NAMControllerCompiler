package view;

/**
 * If a GUI exists, all the publish..-methods must be called from the EDT.
 */
public interface View {

    public void publishException(String message, Throwable e);

    public void publishIssue(String formatMessage, Object... args);

    public boolean publishConfirmOption(String formatMessage, Object... args);

    public void initProgress(String message, int min, int max);

    public void publishProgressIncrement(int increment, String message);

    public void dispose();

    public void publishInfoMessage(String formatMessage, Object... args);
}
