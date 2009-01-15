#!/bin/bash

java  -classpath $CLASSPATH:./bin \
      -Djava.library.path=./lib   \
      -Xmx1000m -Xms1000m         \
      joshua.decoder.JoshuaDecoder example/example.config.srilm example/example.test.in example/example.nbest.srilm.out
