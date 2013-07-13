package hudson.plugins.easyant;

import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotationDescriptor;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;

import java.util.regex.Pattern;

/**
 * Marks the log line "TARGET:" that EasyAnt uses to mark the beginning of the
 * new target.
 * 
 */
public final class EasyAntTargetNote extends ConsoleNote {
    public EasyAntTargetNote() {
    }

    @Override
    public ConsoleAnnotator annotate(Object context, MarkupText text,
            int charPos) {
        // still under development. too early to put into production
        if (!ENABLED)
            return null;

        MarkupText.SubText t = text.findToken(Pattern.compile(".*(?=:)"));
        if (t != null)
            t.addMarkup(0, t.length(), "<b class=easyant-target>", "</b>");
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends
            ConsoleAnnotationDescriptor {
        public String getDisplayName() {
            return "EasyAnt targets";
        }
    }

    public static boolean ENABLED = !Boolean.getBoolean(EasyAntTargetNote.class
            .getName() + ".disabled");
}
