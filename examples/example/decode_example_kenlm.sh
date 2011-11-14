#!/bin/bash

java  -classpath ${CLASSPATH}:${JOSHUA}/bin \
      -Djava.library.path=${JOSHUA}/lib \
      -Dfile.encoding=utf8 \
      -Djava.util.logging.config.file=${JOSHUA}/logging.properties \
      -Xmx1000m -Xms1000m \
      joshua.decoder.JoshuaDecoder example/example.config.kenlm example/example.test.in example/example.nbest.srilm.out
