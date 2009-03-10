#!/bin/bash

java  -classpath $CLASSPATH:./bin \
      -Xmx1000m -Xms1000m         \
      joshua.decoder.JoshuaDecoder example/example.config.bloomfilterlm example/example.test.in example/example.nbest.bloomfilterlm.out
