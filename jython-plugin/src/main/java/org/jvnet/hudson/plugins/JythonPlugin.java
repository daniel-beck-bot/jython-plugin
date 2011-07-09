package org.jvnet.hudson.plugins;

import hudson.FilePath;
import hudson.Plugin;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.logging.Logger;

/**
 * The Jython plug-in entry point. Copies the Jython runtime to the tools
 * directory on the master node.
 *
 * @author Jack Leow
 */
public final class JythonPlugin extends Plugin {
    public static final URL INSTALLER_URL =
        JythonPlugin.class.getResource("jython-installer-2.5.2.JENKINS.zip");
    public static final FilePath JYTHON_HOME =
        Hudson.getInstance().getRootPath().child("tools/jython");
    public static final String SITE_PACKAGES_PATH = "Lib/site-packages";
    
    private static final String EASY_INSTALL_FILENAME = "easy-install.pth";
    private static final Logger LOG =
        Logger.getLogger(JythonPlugin.class.toString());
    
    static void syncSitePackages(
            FilePath targetJythonHome, TaskListener listener)
            throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();
        final FilePath srcSitePkgs = JYTHON_HOME.child(SITE_PACKAGES_PATH);
        final FilePath tgtSitePkgs = targetJythonHome.child(SITE_PACKAGES_PATH);
        
        // Copying "easy-install.pth"
        srcSitePkgs.child(EASY_INSTALL_FILENAME).copyTo(tgtSitePkgs);
        // Copying new packages
        for (FilePath pkgSrc : srcSitePkgs.list()) {
            String pkgName = pkgSrc.getName();
            FilePath pkgTgt = tgtSitePkgs.child(pkgName);
            if (!pkgTgt.exists() ||
                    pkgSrc.lastModified() > pkgTgt.lastModified()) {
                if (pkgSrc.isDirectory()) {
                    pkgSrc.copyRecursiveTo(pkgTgt);
                } else {
                    pkgSrc.copyTo(pkgTgt);
                }
                logger.println("Copied " + pkgName);
            }
        }
        // Deleting uninstalled packages
        for (FilePath pkgTgt : tgtSitePkgs.list()) {
            String pkgName = pkgTgt.getName();
            FilePath pkgSrc = srcSitePkgs.child(pkgName);
            if (!pkgSrc.exists()) {
                try {
                    if (pkgTgt.isDirectory()) {
                        pkgTgt.deleteRecursive();
                    } else {
                        pkgTgt.delete();
                    }
                    logger.println("Deleted " + pkgName);
                } catch (Exception e) {
                    e.printStackTrace(
                        listener.error(
                            "error deleting package - continuing with build"
                        )
                    );
                }
            }
        }
    }
    
    @Override
    public void start() throws Exception {
        if (!JYTHON_HOME.child("jython.jar").exists()) {
            JYTHON_HOME.unzipFrom(INSTALLER_URL.openStream());
            JYTHON_HOME.child("jython").chmod(0755);
            JYTHON_HOME.child("tmp").mkdirs();
            LOG.info("Installed Jython runtime");
        }
    }
}