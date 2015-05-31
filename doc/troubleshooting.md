Troubleshooting		 {#troubleshooting}
================

Make sure that the `JOSHUA` environment variable has been set to the directory
created by the git clone command or from extracting the release tarball.

# Ant build errors

First of all, make sure `ant init` has been run successfully at least once
before running any other Ant tasks.

## Ant version

    download-ivy:
    
    BUILD FAILED
    /home/lorland1/workspace/mt/joshua/build.xml:310: Ivy requires Ant version 1.8.0 or greater. Please upgrade to the latest version.

If an older version of ant is in the system, the developer can manually
download ivy.jar, copy it to `$JOSHUA/lib/`, and delete the `download-ivy` ant
target. Ant 1.8.0 was released in April 2010, so it's not requiring anything
bleeding edge.
