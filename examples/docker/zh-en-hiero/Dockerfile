FROM joshua

ENV language_pack=zh-en-hiero

RUN mkdir /opt/$language_pack
WORKDIR /opt/$language_pack

RUN curl http://cs.jhu.edu/~post/language-packs/zh-en-hiero-2016-01-13.tgz \
    | tar xz --strip-components=1

ENTRYPOINT ["./run-joshua.sh"]
