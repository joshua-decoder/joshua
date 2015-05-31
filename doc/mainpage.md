Installation & Setup	 {#mainpage}
====================

Welcome to the developer documentation for the Joshua hierarchical
statistical machine translation system. Joshua can be used in two
ways: by downloading packaged releases from the
[main web site](http://joshua-decoder.org/) and by installing directly
from the hosted source code Github. The former version is intended for
people who just wish to use Joshua, and the latter for people who wish
to contribute to the codebase. This page is for developers; if you
only wish to use Joshua (including, for example, its prebuilt
[language packs](http://joshua-decoder.org/language-packs/), then you
probably want the [end-user documentation](http://joshua-decoder.org/6.0/).

## Source code

The Joshua source code repository is located at 
[http://github.com/joshua-decoder/joshua](http://github.com/joshua-decoder/joshua).

## Development tools required

* [Java 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html) or greater
* [Apache Ant](http://ant.apache.org/) version 1.8.0 or greater
* [Doxygen](http://www.doxygen.org) version 1.8.0 or greater
* A POSIX environment
* Git
* [Eclipse](https://eclipse.org/downloads/packages/eclipse-ide-java-developers/keplersr2)

## Environment setup

You'll need to set your `$JAVA_HOME` to point to your Java 7+
JDK. Typical values are 

    # OS X
    export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_71.jdk/Contents/Home

    # Redhat / CentOS
    export JAVA_HOME=/usr/java/default
    
## Getting started

You can download Joshua and compile it with the following commands. 

    git clone https://github.com/joshua-decoder/joshua.git
    cd joshua
    export JOSHUA=$(pwd)
    ant
    
The main ant build target target downloads a number of dependencies
using Ivy, along with the Thrax submodule, and then compiles
everything, include support tools like KenLM and GIZA++ (which are
included).

## Using Moses

Moses is used for (1) tuning with kbmira and (2) as an option for
extracting GHKM grammars. If you wish to do either of these, you will
need to download and install Moses 3+. A simplified approach

## Setting up Eclipse

We recommend the use of Eclipse for Joshua development. When setting
up Eclipse, please import the Eclipse settings under `$JOSHUA/.settings`.

## Resources

If you run into troubles, you can post to the
[Joshua Developers' Forum](https://groups.google.com/forum/#!forum/joshua_developers). You
might also be interested in the support forum for
[Joshua users](https://groups.google.com/forum/#!forum/joshua_support). 

If you find bugs in Joshua, the best thing is to fix them yourself and
[submit a pull request](https://help.github.com/articles/creating-a-pull-request/). Alternately,
you can file bug reports, feature requests, and other issues on the
[Joshua issues page](https://github.com/joshua-decoder/joshua/issues).

## Building a new release

Here's an example of building a release versioned "2012-07-18".  It will be placed at
`release/joshua-2012-07-18.tgz`.

    export JOSHUA_VERSION=2012-07-18
    export HADOOP=/path/to/hadoop
    export HADOOP_CONF_DIR=/path/to/hadoop/config
    export HADOOP_VERSION="0.20.203.0"
    export AWS_SDK=/path/to/aws
    export AWS_VERSION="1.1.3"

    git clone https://github.com/joshua-decoder/joshua.git
    cd joshua
    export JOSHUA=`pwd`
    git submodule update --init
    cd thrax
    ant
    cd $JOSHUA
    ant release

## Adding dependencies

Dependencies such as JAR archives are automatically downloaded by the
[Apache Ivy](http://ant.apache.org/ivy/) dependency management tool,
which is designed to interact with the `ant` build tool.

To add a new dependency to the list of automatically downloaded archive
libraries, follow these steps:

1.  Search for the library in 
    [Maven Central Repository](http://search.maven.org/) or 
    [MVN Repository](http://mvnrepository.com/). 
2.  If the desired library is found, both websites provide the line that
    you would add under `<dependencies>` in `ivy.xml`. E.g. for
    **asm-3.1.jar**, the line that would be added is:

        <dependency org="asm" name="asm" rev="3.1"/>

More obscure libraries can be found to be hosted in less common
repositories. Additional repositories can be added to the
`$JOSHUA/ivysettings.xml` file.
