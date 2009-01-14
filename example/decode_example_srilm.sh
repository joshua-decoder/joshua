cd ..
java  -classpath $CLASS_PATH:$JOSHUA_HOME/bin -Xmx1000m -Xms1000m joshua.decoder.JoshuaDecoder example/example.config.srilm example/example.test.in example/example.nbest.srilm.out
