Documenting Joshua				{#documentation}
------------------

Developers can contribute to this manual to keep the descriptions of the code
up-to-date and provide usage instructions and examples. Source code
documentation is automatically extracted from the source code, including those
JavaDoc tags contained in comments. These manual pages are written using the
[Markdown](http://www.stack.nl/~dimitri/doxygen/markdown.html#markdown_extra)
syntax. 

## Requirements

* Doxygen version
  [1.8.0 or higher](http://www.stack.nl/~dimitri/doxygen/download.html)

## Instructions

Edit the JavaDoc tagging as needed in the source code.

Edit the source in the `$JOSHUA/doc/*.md` manual pages or create new ones
in the directory. (Note the use of
[reference links](http://www.stack.nl/~dimitri/doxygen/markdown.html#md_reflinks)
in the manual sources.) 

Regenerate the updated documentation by running the following ant target:

    cd $JOSHUA
    ant documentation

Preview the regenerated manual by opening `$JOSHUA/doc/html/index.html` in a web
browser.
