package model;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Deque;
import java.util.regex.Pattern;

public interface OverrideWriter {

    public void writeLineChecked(String line, OutputStreamWriter writer, Deque<Pattern> patterns) throws IOException;
}
