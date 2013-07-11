package hudson.plugins.easyant;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.remoting.Callable;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * This class represent an easyant installation
 * 
 * @author Jean Louis Boudart
 */
public final class EasyAntInstallation extends ToolInstallation implements
        EnvironmentSpecific<EasyAntInstallation>,
        NodeSpecific<EasyAntInstallation>, Serializable {
    // to remain backward compatible with earlier Hudson that stored this field
    // here.
    @Deprecated
    private transient String easyantHome;

    public static final String UNIX_EASYANT_COMMAND = "easyant";
    public static final String WINDOWS_EASYANT_COMMAND = "easyant.bat";

    @DataBoundConstructor
    public EasyAntInstallation(String name, String home,
            List<? extends ToolProperty<?>> properties) {
        super(name, launderHome(home), properties);
        this.easyantHome = super.getHome();
    }

    /**
     * @deprecated Use {@link #EasyAntInstallation(String, String, List)}
     */
    public EasyAntInstallation(String name, String home) {
        this(name, home, Collections.<ToolProperty<?>> emptyList());
    }

    private static String launderHome(String home) {
        if (home.endsWith("/") || home.endsWith("\\")) {
            // see https://issues.apache.org/bugzilla/show_bug.cgi?id=26947
            // Ant doesn't like the trailing slash, especially on Windows
            return home.substring(0, home.length() - 1);
        } else {
            return home;
        }
    }

    public String getEasyantHome() {
        return easyantHome;
    }

    public String getExecutable(Launcher launcher) throws IOException,
            InterruptedException {
        return launcher.getChannel().call(new Callable<String, IOException>() {
            public String call() throws IOException {
                File exe = getExeFile();
                if (exe.exists()) {
                    return exe.getPath();
                }
                return null;
            }
        });
    }

    private File getExeFile() {
        String execName = (Functions.isWindows()) ? WINDOWS_EASYANT_COMMAND
                : UNIX_EASYANT_COMMAND;
        String antHome = Util.replaceMacro(easyantHome, EnvVars.masterEnvVars);
        return new File(antHome, "bin/" + execName);
    }

    public EasyAntInstallation forEnvironment(EnvVars environment) {
        return new EasyAntInstallation(getName(),
                environment.expand(easyantHome), getProperties().toList());
    }

    public EasyAntInstallation forNode(Node node, TaskListener log)
            throws IOException, InterruptedException {
        return new EasyAntInstallation(getName(), translateFor(node, log),
                getProperties().toList());
    }

    @Extension
    public static class DescriptorImpl extends
            ToolDescriptor<EasyAntInstallation> {

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return Messages.installer_displayName();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new EasyAntInstaller(null));
        }

        @Override
        public EasyAntInstallation[] getInstallations() {
            return Hudson.getInstance()
                    .getDescriptorByType(EasyAnt.DescriptorImpl.class)
                    .getInstallations();
        }

        @Override
        public void setInstallations(EasyAntInstallation... installations) {
            Hudson.getInstance()
                    .getDescriptorByType(EasyAnt.DescriptorImpl.class)
                    .setInstallations(installations);
        }

        /**
         * Checks if the EASYANT_HOME is valid.
         */
        public FormValidation doCheckHome(@QueryParameter File value) {
            // this can be used to check the existence of a file on the server,
            // so needs to be protected
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return FormValidation.ok();
            }

            if (value.getPath().equals("")) {
                return FormValidation.ok();
            }

            if (!value.isDirectory()) {
                return FormValidation.error(Messages
                        .EasyAnt_NotADirectory(value));
            }

            File easyAntJar = new File(value, "lib/easyant-core.jar");
            if (!easyAntJar.exists()) {
                return FormValidation.error(Messages
                        .EasyAnt_NotAntDirectory(value));
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

    }

    public static class ConverterImpl extends ToolConverter {
        public ConverterImpl(XStream2 xstream) {
            super(xstream);
        }

        @Override
        protected String oldHomeField(ToolInstallation obj) {
            return ((EasyAntInstallation) obj).easyantHome;
        }
    }
}
