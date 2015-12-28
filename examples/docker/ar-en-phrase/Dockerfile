FROM joshua

ENV language_pack=ar-en-phrase

RUN mkdir /opt/$language_pack
WORKDIR /opt/$language_pack

RUN curl http://cs.jhu.edu/~post/language-packs/language-pack-ar-en-phrase-2015-03-18.tgz \
    | tar xz --strip-components=1

ENTRYPOINT ["./run-joshua.sh"]
