#!/bin/bash


cat test.in | \
java  -classpath ${CLASSPATH}:${JOSHUA}/bin \
      -Djava.library.path=${JOSHUA}/lib \
      -Dfile.encoding=utf8 \
      -Djava.util.logging.config.file=${JOSHUA}/logging.properties \
      -Xmx512m -Xms512m \
      joshua.decoder.JoshuaDecoder -c config.default \
      > nbest.default.out
