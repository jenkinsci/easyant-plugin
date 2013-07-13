package hudson.plugins.easyant;

import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotationDescriptor;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;

/**
 * Annotates the BUILD SUCCESSFUL/FAILED line of the EasyAnt execution.
 * 
 */
public class EasyAntOutcomeNote extends ConsoleNote {
    public EasyAntOutcomeNote() {
    }

    @Override
    public ConsoleAnnotator annotate(Object context, MarkupText text,
            int charPos) {
        if (text.getText().contains("FAIL"))
            text.addMarkup(0, text.length(),
                    "<span class=easyant-outcome-failure>", "</span>");
        if (text.getText().contains("SUCCESS"))
            text.addMarkup(0, text.length(),
                    "<span class=easyant-outcome-success>", "</span>");
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends
            ConsoleAnnotationDescriptor {
        public String getDisplayName() {
            return "EasyAnt build outcome";
        }
    }
}
