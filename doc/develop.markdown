Information for Developers				{#develop}
==========================

[TOC]

## Source code

The Joshua source code repository is located at 
[http://github.com/joshua-decoder/joshua](http://github.com/joshua-decoder/joshua).
Active development is being done in the `devel` branch.

## Discussion list

[https://groups.google.com/forum/#!forum/joshua_developers](https://groups.google.com/forum/#!forum/joshua_developers)

## GitHub issues

[https://github.com/joshua-decoder/joshua/issues](https://github.com/joshua-decoder/joshua/issues)

## Documentation

Documenting Joshua is of great importance. The manual is automatically
generated using Doxygen. Find out more on the [documentation](@ref documentation)
page.

## Style guide

### Naming conventions

TBD

### Tools

If you are developing Joshua using Eclipse, please import the GoogleStyle 
formatter configuration `eclipse-java-google-style.xml`, which can be 
downloaded from 
[http://code.google.com/p/google-styleguide/source/browse/trunk](http://code.google.com/p/google-styleguide/source/browse/trunk).

In Eclipse preferences, go to *Java* -> *Code Style* -> *Formatter*. Then click 
on *Import...* and choose `eclipse-java-google-style.xml`.

### Format

* 100 characters maximum line width
* Indent with 2 spaces

  In emacs: 

    (setq tab-width 2)
    (setq-default indent-tabs-mode nil)

* Use spaces only for tabbing
* Open brace on same line
* Keep `else if` on one line
* TBD

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
    git checkout -b devel origin/devel
    git submodule update --init
    cd thrax
    ant
    cd $JOSHUA
    ant release

