#!/bin/bash

java  -classpath ${CLASSPATH}:${JOSHUA}/bin \
      -Djava.library.path=${JOSHUA}/lib \
      -Dfile.encoding=utf8 \
      -Djava.util.logging.config.file=${JOSHUA}/logging.properties \
      -Xmx55g -Xms55g \
      joshua.decoder.JoshuaDecoder $CONF <$INPUT 2>$LOG
