# EXAMPLE: tree_visualizer.cmd tree.fr tree.ref tree.en
set JAVA_HOME="C:\Program Files (x86)\Java\jre6"
set PATH="%JAVA_HOME%/bin";%PATH%

java -Xmx1g -jar tree_visualizer.jar %*
