package jenkins.plugins.publish_over_cifs;

import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public class JenkinsHelper {
    private JenkinsHelper() {}

    public static <T extends Descriptor> T getDescriptor(Class<T> descriptorType) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            return jenkins.getDescriptorByType(descriptorType);
        } else {
            return null;
        }
    }
}
