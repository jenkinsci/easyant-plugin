package hudson.plugins.easyant;

import hudson.Extension;
import hudson.tasks.Messages;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;

import org.kohsuke.stapler.DataBoundConstructor;

public class EasyAntInstaller extends DownloadFromUrlInstaller {
    @DataBoundConstructor
    public EasyAntInstaller(String id) {
        super(id);
    }

    @Extension
    public static final class DescriptorImpl extends
            DownloadFromUrlInstaller.DescriptorImpl<EasyAntInstaller> {
        public String getDisplayName() {
            return Messages.InstallFromApache();
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == EasyAntInstallation.class;
        }
    }
}
