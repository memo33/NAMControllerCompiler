package view;

import static controller.NAMControllerCompilerMain.LOGGER;

import java.util.logging.Level;

public class DefaultView implements View {

    @Override
    public void publishException(String message, Throwable e) {
        LOGGER.log(Level.SEVERE, message, e);
    }

    @Override
    public void publishIssue(String formatMessage, Object... args) {
        LOGGER.log(Level.SEVERE, formatMessage, args);
    }

    @Override
    public void publishInfoMessage(String formatMessage, Object... args) {
        LOGGER.log(Level.INFO, formatMessage, args);
    }

    @Override
    public boolean publishConfirmOption(String formatMessage, Object... args) {
        return true; // always accept if no user input available
    }

    @Override
    public void initProgress(String message, int min, int max) {
        // not implemented
    }

    @Override
    public void publishProgressIncrement(int increment, String note) {
        // not implemented
    }

    @Override
    public void dispose() {
        // nothing
    }
}
