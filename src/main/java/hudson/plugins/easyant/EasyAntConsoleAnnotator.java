package hudson.plugins.easyant;

import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Filter {@link OutputStream} that places an annotation that marks EasyAnt
 * target
 * 
 */
public class EasyAntConsoleAnnotator extends LineTransformationOutputStream {
    private final OutputStream out;
    private final Charset charset;

    private boolean seenEmptyLine;

    public EasyAntConsoleAnnotator(OutputStream out, Charset charset) {
        this.out = out;
        this.charset = charset;
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {
        String line = charset.decode(ByteBuffer.wrap(b, 0, len)).toString();

        // trim off CR/LF from the end
        line = trimEOL(line);

        if (seenEmptyLine && endsWith(line, ':') && line.indexOf(' ') < 0)
            // put the annotation
            new EasyAntTargetNote().encodeTo(out);

        if (line.startsWith("BUILD SUCCESSFUL")
                || line.startsWith("BUILD FAILED"))
            new EasyAntOutcomeNote().encodeTo(out);

        seenEmptyLine = line.length() == 0;
        out.write(b, 0, len);
    }

    private boolean endsWith(String line, char c) {
        int len = line.length();
        return len > 0 && line.charAt(len - 1) == c;
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }

}
