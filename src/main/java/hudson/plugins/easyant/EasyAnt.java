package hudson.plugins.easyant;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A builder for EasyAnt scripts
 * 
 * @author Jean Louis Boudart
 */
public class EasyAnt extends Builder {

    private final String easyAntName;

    private final String targets;

    private final String buildModule;

    private final String buildFile;

    /**
     * EASYANT_OPTS if not null.
     */
    private final String easyAntOpts;

    /**
     * Optional properties to be passed to EasyAnt. Follows {@link Properties}
     * syntax.
     */
    private final String properties;

    @DataBoundConstructor
    public EasyAnt(String easyAntName, String targets, String buildModule,
            String buildFile, String easyAntOpts, String properties) {
        this.easyAntName = easyAntName;
        this.targets = targets;
        this.buildModule = Util.fixEmptyAndTrim(buildModule);
        this.buildFile = Util.fixEmptyAndTrim(buildFile);
        this.easyAntOpts = Util.fixEmptyAndTrim(easyAntOpts);
        this.properties = Util.fixEmptyAndTrim(properties);
    }

    public String getEasyAntName() {
        return easyAntName;
    }

    public String getTargets() {
        return targets;
    }

    public String getBuildFile() {
        return buildFile;
    }

    public String getEasyAntOpts() {
        return easyAntOpts;
    }

    public String getProperties() {
        return properties;
    }

    public String getBuildModule() {
        return buildModule;
    }

    public EasyAntInstallation getEasyAnt() {
        for (EasyAntInstallation i : getDescriptor().getInstallations()) {
            if (easyAntName != null && i.getName().equals(easyAntName))
                return i;
        }
        return null;

    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder();

        EnvVars env = build.getEnvironment(listener);
        // env.overrideAll(build.getBuildVariables());

        EasyAntInstallation ai = getEasyAnt();
        if (ai == null) {
            args.add(launcher.isUnix() ? EasyAntInstallation.UNIX_EASYANT_COMMAND
                    : EasyAntInstallation.WINDOWS_EASYANT_COMMAND);

        } else {
            ai = ai.forNode(Computer.currentComputer().getNode(), listener);
            ai = ai.forEnvironment(env);
            String exe;
            exe = ai.getExecutable(launcher);
            if (exe == null) {
                listener.error("Can't retrieve the EasyAnt executable.");
                return false;
            }
            args.add(exe);
        }

        VariableResolver<String> vr = new VariableResolver.ByMap<String>(env);
        String buildModule = env.expand(this.buildModule);
        String buildFile = env.expand(this.buildFile);
        String targets = env.expand(this.targets);

        if (buildModule != null) {
            FilePath buildModulePath = buildFilePath(build, listener,
                    buildModule);
            if (buildModulePath == null) {
                return false;
            }
            args.add("-buildModule", buildModulePath.getName());
        }

        if (buildFile != null) {
            FilePath buildFilePath = buildFilePath(build, listener, buildFile);
            if (buildFilePath == null) {
                return false;
            }
            args.add("-buildFile", buildFilePath.getName());
        }

        Set<String> sensitiveVars = build.getSensitiveBuildVariables();

        args.addKeyValuePairs("-D", build.getBuildVariables(), sensitiveVars);

        args.addKeyValuePairsFromPropertyString("-D", properties, vr,
                sensitiveVars);

        args.addTokenized(targets.replaceAll("[\t\r\n]+", " "));

        if (ai != null) {
            env.put("EASYANT_HOME", ai.getHome());
        }
        if (easyAntOpts != null)
            env.put("EASYANT_OPTS", env.expand(easyAntOpts));

        if (!launcher.isUnix()) {
            args = args.toWindowsCommand();
            // For some reason, ant on windows rejects empty parameters but unix
            // does not.
            // Add quotes for any empty parameter values:
            List<String> newArgs = new ArrayList<String>(args.toList());
            newArgs.set(newArgs.size() - 1, newArgs.get(newArgs.size() - 1)
                    .replaceAll("(?<= )(-D[^\" ]+)= ", "$1=\"\" "));
            args = new ArgumentListBuilder(newArgs.toArray(new String[newArgs
                    .size()]));
        }

        FilePath rootLauncher = null;
        if (buildFile != null && buildFile.trim().length() != 0) {
            String rootBuildScriptReal = Util.replaceMacro(buildFile, env);
            rootLauncher = new FilePath(build.getModuleRoot(), new File(
                    rootBuildScriptReal).getParent());
        } else {
            rootLauncher = build.getModuleRoot();
        }

        try {
            int r = launcher.launch().cmds(args).envs(env).stdout(listener)
                    .pwd(rootLauncher).join();
            return r == 0;
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("command execution failed"));
            return false;
        }
    }

    private FilePath buildFilePath(AbstractBuild<?, ?> build,
            BuildListener listener, String file) throws IOException,
            InterruptedException {
        FilePath filepath = build.getModuleRoot().child(file);
        if (filepath.exists()) {
            return filepath;
        } else {
            // because of the poor choice of getModuleRoot() with
            // CVS/Subversion, people often get confused
            // with where the build file path is relative to. Now it's too late
            // to change this behavior
            // due to compatibility issue, but at least we can make this less
            // painful by looking for errors
            // and diagnosing it nicely. See HUDSON-1782

            // first check if this appears to be a valid relative path from
            // workspace root
            FilePath buildFilePath2 = build.getWorkspace().child(file);
            if (buildFilePath2.exists()) {
                // This must be what the user meant. Let it continue.
                return buildFilePath2;
            } else {
                // neither file exists. So this now really does look like an
                // error.

                listener.fatalError("Unable to find build module or build script at "
                        + filepath);
                return null;
            }
        }

    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Builder> {

        @CopyOnWrite
        private volatile EasyAntInstallation[] installations = new EasyAntInstallation[0];

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        protected DescriptorImpl(Class<? extends EasyAnt> clazz) {
            super(clazz);
        }

        /**
         * Obtains the {@link EasyantInstallation.DescriptorImpl} instance.
         */
        public EasyAntInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(
                    EasyAntInstallation.DescriptorImpl.class);
        }

        @Override
        public String getHelpFile() {
            return "/plugin/easyant/help.html";
        }

        public String getDisplayName() {
            return Messages.EasyAnt_DisplayName();
        }

        public EasyAntInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(EasyAntInstallation... installations) {
            this.installations = installations;
            save();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json)
                throws FormException {
            installations = req.bindJSONToList(EasyAntInstallation.class,
                    json.get("inst")).toArray(new EasyAntInstallation[0]);
            save();
            return true;
        }

        @Override
        public EasyAnt newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return (EasyAnt) req.bindJSON(clazz, formData);
        }

    }

}
