Creating a release	 {#mainpage}
==================

Define `$JOSHUA` and `$JOSHUA_VERISON` (the release version), then
call

    ant release
    
This should be done in a freshly checked-out copy, since it will wipe
out all non-tracked files, download the web site, and do some other
things. You should also make sure that you tag the release in the
source code.

    git tag -a $JOSHUA_RELEASE
    git push --tags

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
