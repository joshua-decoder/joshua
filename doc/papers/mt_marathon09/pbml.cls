%% $Id: pbml.cls 87 2007-12-14 12:45:41Z zw $
\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{pbml}[2007/12/14 (Xe)LaTeX class for PBML]

% Naming convention:
% The class redefines some standard LaTeX commands in order to comply with
% the graphical design.
% New macros have in most cases names conforming to the pattern \PBMLmacro.
% Internal macros that should not be used directly in the document conform
% to the name pattern \pbml@macro.

% Ignore size options without warning

\DeclareOption{10pt}{}
\DeclareOption{11pt}{}
\DeclareOption{12pt}{}

% Ignore one-/twoside

\DeclareOption{oneside}{}
\DeclareOption{twoside}{}
                                                                                
% Ignore paper sizes
                                                                                
\DeclareOption{a4paper}{}
\DeclareOption{a5paper}{}
\DeclareOption{b5paper}{}
\DeclareOption{letterpaper}{}
\DeclareOption{legalpaper}{}
\DeclareOption{executivepaper}{}
\DeclareOption{landscape}{}

% Switch between dummy logo and real logo

\DeclareOption{nologo}{\def\pbml@inslogo{\pbml@draftlogo}}
\DeclareOption{logo}{\def\pbml@inslogo{\pbml@real@logo}}

% Cropmarks

\DeclareOption{cropmarks}{\def\pbml@use@cropmarks{true}}

% XML with table of contents

\newif\ifpbml@xml
\DeclareOption{xml}{\pbml@xmltrue}

% DejaVu fonts

\newif\ifpbml@fonts \pbml@fontstrue
\DeclareOption{nofonts}{\pbml@fontsfalse}
                                                                                
% Pass all global options to the "article" class
                                                                                
\DeclareOption*{\PassOptionsToClass{\CurrentOption}{article}}
\ExecuteOptions{nologo}
\ProcessOptions
                                                                                
% Load the class with default options
                                                                                
\LoadClass[twoside]{article}

% Paper dimensions -- somewhat smaller than B5

\paperwidth 170mm
\paperheight 240mm

% Sizes (\normalsize and below kept at default)

\def\large{\@setfontsize\large\@xipt{13.5}}
\def\Large{\@setfontsize\Large\@xiipt{14.4}}
\def\LARGE{\@setfontsize\LARGE\@xivpt{18}} % unused
\def\huge{\@setfontsize\huge\@xviipt\@xviipt} % for draft logo
\let\Huge\huge % unused

% Lists

\setlength\partopsep{2\p@ \@plus 1\p@ \@minus 1\p@}
\def\@listi{\leftmargin\leftmargini
            \parsep \z@
            \topsep \z@
            \itemsep \z@}
\let\@listI\@listi
\@listi
\def\@listii {\leftmargin\leftmarginii
              \labelwidth\leftmarginii
              \advance\labelwidth-\labelsep
              \parsep \z@
              \topsep \z@
              \itemsep \z@}
\def\@listiii{\leftmargin\leftmarginiii
              \labelwidth\leftmarginiii
              \advance\labelwidth-\labelsep
              \parsep \z@
              \topsep \z@
              \itemsep \z@}
\def\@listiv {\leftmargin\leftmarginiv
              \labelwidth\leftmarginiv
              \advance\labelwidth-\labelsep}
\def\@listv  {\leftmargin\leftmarginv
              \labelwidth\leftmarginv
              \advance\labelwidth-\labelsep}
\def\@listvi {\leftmargin\leftmarginvi
              \labelwidth\leftmarginvi
              \advance\labelwidth-\labelsep}


% Skips

\bigskipamount\baselineskip
\medskipamount.5\baselineskip
\smallskipamount\medskipamount

\textfloatsep\baselineskip
\intextsep\baselineskip
\floatsep\baselineskip
\@fptop\z@
\@fpsep\baselineskip

% Page layout

\topskip\baselineskip
\textwidth 29cc
\evensidemargin 22mm
\oddsidemargin \paperwidth
\advance\oddsidemargin -\textwidth
\advance\oddsidemargin -\evensidemargin
\advance\oddsidemargin -1in
\advance\evensidemargin -1in
\footskip 24\p@
\headheight 12\p@
\headsep 24\p@
\textheight 42\baselineskip
\topmargin 22mm
\advance\topmargin -1in
\parindent 14\p@
\parskip \z@

% Sectioning, only \section, \subsection and \subsubsection supported

\def\section{\@startsection{section}\@ne\z@{12\p@}{12\p@ minus 6\p@}%
            {\normalfont\large\bfseries}}
\def\pbml@subsection{\@startsection{subsection}\tw@\z@{12\p@}{12\p@ minus 6\p@}%
                    {\normalfont\bfseries}}
\def\pbml@subsubsection{\@startsection{subsubsection}{3}\z@{12\p@}{12\p@ minus 6\p@}%
                       {\normalfont}}
\def\pbml@xsubsection{\@startsection{subsection}\tw@\z@{12\p@}{-.5em}%
                     {\normalfont\bfseries}}
\def\pbml@xsubsubsection{\@startsection{subsubsection}{3}\z@{12\p@}{-.5em}%
                        {\normalfont\bfseries}}

\def\subsection{\@ifstar{\pbml@subsection*}{\pbml@testsec{subsection}}}
\def\subsubsection{\@ifstar{\pbml@subsubsection*}{\pbml@testsec{subsubsection}}}

\def\pbml@testsec#1#2{\ifcat$#2$\@nameuse{pbml@x#1}{}\else\@nameuse{pbml@#1}{#2}\fi}

\setcounter{secnumdepth}{3}
\renewcommand\@seccntformat[1]{\@nameuse{the#1}.\hspace{.5em}}

% Abstract

\renewenvironment{abstract}{\normalfont\normalsize
  \vskip 12\p@ minus 4\p@\par
  \noindent\textbf{Abstract}\par \small}{\par
  \vskip 12\p@ minus 4\p@ \hrule height \p@}

% Captions, always centered

\abovecaptionskip\baselineskip
\belowcaptionskip\baselineskip

\def\@makecaption#1#2{\vskip\abovecaptionskip
  {\centering\normalfont\normalsize\sffamily\itshape #1. #2\par}\vskip\belowcaptionskip}




% Required packages

\RequirePackage{euler} 		% math
\RequirePackage{pbmlfonts}	% PBML fonts
\RequirePackage{graphicx} 	% needed for logo
\RequirePackage{multicol}	% multicolumn text
\RequirePackage{keyval}		% author names and addresses
\RequirePackage{fullname} 	% bibliography
\RequirePackage{pbmlbib}	% bibliography per \include

% Slanted -> italics

\let\sl\it
\let\textsl\textit

% Bibliography

\renewenvironment{thebibliography}[1]{%
 \section*{\bibname}\small
 \list
 {}{\setlength{\labelwidth}{0pt}\setlength{\leftmargin}{\parindent}
 \setlength{\itemindent}{-\parindent}}
 \def\newblock{\hskip .11em plus .33em minus -.07em}
 \sloppy\clubpenalty4000\widowpenalty4000
 \sfcode`\.=1000\relax}%
 {\endlist}

% Hacked references (from fullname.sty)

\let\pbml@fullname@cite\@cite
\def\pbml@cite#1#2{{#1\if@tempswa , #2\fi}\let\@cite\pbml@fullname@cite}
\def\citegood{\def\citename##1{##1, }\let\@cite\pbml@cite\@internalcite}
\def\shortcitegood{\def\citename##1{}\let\@cite\pbml@cite\@internalcite}
\def\namecitegood{\def\citename##1{##1 }\let\@cite\pbml@cite\@internalcite}

% Define logos

\def\pbml@real@logo{\includegraphics{PBMLlogo}}
\def\pbml@draftlogo{\leavevmode\vbox{\hsize 15mm \LARGE \sf \bfseries
     \noindent\hbox to \hsize{P\hss B\hss M\hss L}\par \vskip 2\p@
     \hrule height \p@
     \vrule height 8mm depth 7mm width \p@
     \hfill \tiny logo \hfill
     \vrule width \p@
     \hrule}}

% Cross references, prefix will be defined by \PBMLinclude

\def\pbml@prefix{} % default for single article
\let\pbml@label\label
\let\pbml@pageref\pageref
\let\pbml@ref\ref
\def\pbml@redef#1 {\expandafter
    \def\csname#1\endcsname##1{\csname pbml@#1\endcsname
                              {\pbml@prefix##1}}}
\pbml@redef label
\pbml@redef pageref
\pbml@redef ref

% PBML Info

\define@key{PBMLinfo}{number}{\def\pbml@number{#1}}
\define@key{PBMLinfo}{month}{\expandafter\xdef\expandafter\pbml@month
     \expandafter{\expandafter\uppercase\expandafter{#1}}}
\define@key{PBMLinfo}{year}{\def\pbml@year{#1}}

\def\pbml@year{\number2009}
\def\pbml@month{JANUARY}
\def\pbml@number{91}

\def\PBMLinfo#{\setkeys{PBMLinfo}}

% For hyperref

\newif\ifPBMLhyperref
\def\PBMLhyperref{\ifPBMLhyperref
\usepackage[
pdfdisplaydoctitle,
pdfcreator=XeLaTeX\ with\ Hyperref\ package,
colorlinks,
unicode,
xetex]{hyperref}\fi}

% Title page of an article, start with defining keys

\define@key{PBMLaut}{surname}{\def\pbml@surname{#1}}
\define@key{PBMLaut}{firstname}{\def\pbml@firstname{#1}}
\define@key{PBMLaut}{address}{\def\pbml@aut@address{#1}}

\def\author#1{\def\pbml@surname{}\def\pbml@firstname{}\def\pbml@aut@address{}%
    \setkeys{PBMLaut}{#1}\pbml@compose@autlist}
\def\pbml@compose@autlist{%
    \ifcat$\the\pbml@autlist$\else \pbml@addtoaut{, }\fi
    \expandafter\pbml@addtoaut\expandafter{\pbml@firstname\space}%
    \expandafter\pbml@addtoaut\expandafter{\pbml@surname}}

\def\pbml@addtoaut#1{\pbml@autlist\expandafter{\the\pbml@autlist #1}}
\newtoks\pbml@autlist

\def\title#{\def\pbml@title}
\def\subtitle#{\def\pbml@subtitle}
\def\shorttitle#{\def\pbml@pstitle}
\def\shortauthor#{\def\pbml@psauthor}
\let\shortauthors\shortauthor % alias

% Auxilliary macro for the running head

\def\pbml@def#1 {\expandafter\ifx\csname pbml@ps#1\endcsname\relax
    \expandafter\def\expandafter\pbml@temp\expandafter{\csname pbml@#1\endcsname}%
    \expandafter\let\csname pbml@ps#1\endcsname\pbml@temp
    \fi}

\newdimen\pbml@hwidth
\newbox\pbml@hbox

% Find information in the .aux file and compose the page range

\def\pbml@makerange{%
    \expandafter\ifx\csname\pbml@prefix @@lastpage@@\endcsname\relax
      \expandafter\ifx\csname\pbml@prefix @@firstpage@@\endcsname\relax
        \edef\pbml@pagerange{??--???}%
      \else
        \edef\pbml@pagerange{\@nameuse{\pbml@prefix @@firstpage@@}--???}%
      \fi
    \else
    \expandafter\ifx\csname\pbml@prefix @@firstpage@@\endcsname\relax
      \edef\pbml@pagerange{???--???}% an error has occured
    \else
      \edef\pbml@tempa{\@nameuse{\pbml@prefix @@firstpage@@}}%
      \edef\pbml@tempb{\@nameuse{\pbml@prefix @@lastpage@@}}%
      \ifnum\pbml@tempa=\pbml@tempb
        \edef\pbml@pagerange{\pbml@tempb}%
      \else
        \edef\pbml@pagerange{\pbml@tempa--\pbml@tempb}%
      \fi
    \fi \fi}

% Print the header

\def\pbml@makeheader{\cleardoublepage \normalfont \normalsize
    \pbml@mktitle@warning
    \gdef\thesection{\@arabic\c@section}%
    \setcounter{section}{0}\setcounter{footnote}{0}%
    \setcounter{table}{0}\setcounter{figure}{0}%
    \pbml@makerange
    \thispagestyle{PBMLempty}%
    \setbox\pbml@hbox \hbox{\sf \large \bfseries
      NUMBER \pbml@number \quad \pbml@month \space \pbml@year
      \ifpbml@norange\else\quad \pbml@pagerange\fi}\pbml@hwidth \wd\pbml@hbox
    \vbox to 11mm{\vss \centering \pbml@inslogo \par}\vskip 5\p@
    \centerline{\normalsize \sf \bfseries
      The Prague Bulletin of Mathematical Linguistics}\vskip \p@
    \hrule height \z@ depth .5\p@ \vskip 3\p@
    \centerline{\copy\pbml@hbox}\vskip 16\p@
      \write\@auxout{\string\PBMLbegdef{\pbml@prefix}{\number\c@page}}}

\def\pbml@mktitle@warning{\def\maketitle{\ClassWarning
    {pbml}{\string\maketitle\space is not allowed here.\MessageBreak
           Your command was ignored and procesing continues.\MessageBreak
           This error was found}}}

% Decide whether subtitle is used in the copyright text

\newif\ifPBMLusesubtitle
\PBMLusesubtitletrue
\newif\ifpbml@norange

% Make a title of an article

\renewcommand*\maketitle[1][0]{\pbml@makeheader
    {\pbml@thanks
      \centering \Large \sf {\bfseries \pbml@title\par
      \expandafter\ifx\csname pbml@subtitle\endcsname\relax\else
        \vskip 5\p@
        {\large \pbml@subtitle \par}\fi}\vskip 18\p@
      \the\pbml@autlist\par}\vskip 24\p@
    \hrule height \p@
    \vskip #1\baselineskip
    \expandafter\def\expandafter\pbml@author\expandafter{\the\pbml@autlist}
    \pbml@def title \pbml@def author
    \markright{\pbml@psauthor\hfill\pbml@pstitle\ (\pbml@pagerange)}%
    \ifpbml@xml \pbml@xmlwrite \fi
    \bibliographystyle{fullname}%
    \ifPBMLhyperref
      %\hypersetup{Title=\pbml@title,Author=\the\pbml@autlist}%
      \hypersetup{bookmarks,bookmarksopen}%
    \fi
    \pbml@mkcpr{\textcopyright\ \pbml@year\ PBML. All rights reserved.\\
      Please cite this article as: \the\pbml@autlist, \pbml@title
      \expandafter\ifx\csname pbml@subtitle\endcsname\relax\else
        \ifPBMLusesubtitle
          : \pbml@subtitle
        \fi
      \fi.
      \PBMLusesubtitletrue
      The Prague Bulletin of Mathematical Linguistics No. \pbml@number,
      \pbml@year, \pbml@pagerange.}}

% XML writing
\newwrite\pbml@xmlout

\ifpbml@xml
  \AtBeginDocument{\openout\pbml@xmlout \jobname.xml
    \write\pbml@xmlout{<?xml version="1.0" encoding="UTF-8" standalone="yes"?>}
    \write\pbml@xmlout{<pbml-contents number="\pbml@number" month="\pbml@month" year="\pbml@year">}}
  \AtEndDocument{\immediate\write\pbml@xmlout{</pbml-contents>}\immediate\closeout\pbml@xmlout}
\fi

\def\pbml@xmlwrite{{\let\thepage\relax
  \def\nobreakspace##1{ }%
  \write\pbml@xmlout{<item>}%
  \xdef\pbml@tempa{\the\pbml@autlist}%
  \write\pbml@xmlout{<authors>\pbml@tempa</authors>}%
  \xdef\pbml@tempt{\pbml@title}%
  \write\pbml@xmlout{<title>\pbml@tempt</title>}%
  \expandafter\ifx\csname pbml@subtitle\endcsname\relax\else
    \xdef\pbml@temps{\pbml@subtitle}%
    \write\pbml@xmlout{<subtitle>\pbml@temps</subtitle>}%
  \fi
  \write\pbml@xmlout{<page>\thepage</page>}%
  \write\pbml@xmlout{</item>}}}

% Generic macro fir simpler titles

\newcommand*\pbml@generic[2][0]{\pbml@norangetrue\title{#2}\pbml@makeheader
    {\pbml@thanks
      \centering \Large \sf \bfseries \pbml@title\par}\vskip #1\baselineskip
     \vskip 72\p@ minus 9\p@
     \pbml@def title
     \markright{\hfill\pbml@pstitle\ (\pbml@pagerange)}%
     \pbml@mkcpr{\textcopyright\ \pbml@year\ PBML. All rights reserved.}}

% Specialized titles using the generic pattern

\newcommand*\editorial[1][0]{\pbml@generic[#1]{EDITORIAL}}
\newcommand*\EditorialBoard[1][0]{\PBMLemptylast\pbml@generic[#1]{EDITORIAL BOARD}}
\newcommand*\ListOfAuthors[1][0]{\pbml@generic[#1]{LIST OF AUTHORS}}
\newcommand*\InstructionsForAuthors{\@ifstar{\pbml@xInstructionsForAuthors}{\pbml@InstructionsForAuthors}}
\newcommand*\pbml@InstructionsForAuthors[1][0]{\pbml@generic[#1]{INSTRUCTIONS FOR AUTHORS}}
\newcommand*\PBMLcontent[1][0]{\PBMLemptylast\pbml@generic[#1]{CONTENTS}}
\let\PBMLcontents\PBMLcontent

\newcommand\pbml@xInstructionsForAuthors[2][INSTRUCTIONS FOR AUTHORS]{\title{#1}\pbml@makeheader
    {\pbml@thanks
      \centering \Large \sf \bfseries \pbml@title\par}\vskip 24\p@ minus 9\p@
     \pbml@def title
     \setkeys{PBMLbook}{#2}%
     {\parindent\z@ \Large \sf \textbf{\pbml@book@title}\par
      \ifx\pbml@book@author\undefined\else
        \bigskip
        \large\pbml@book@author\par
      \fi}\vskip 24ptminus 5pt
     \markright{\hfill\pbml@pstitle\ (\pbml@pagerange)}%
     \pbml@mkcpr{\textcopyright\ \pbml@year\ PBML. All rights reserved.}}

% 2-column layout without indent and with spaces between paragraphs

\newenvironment{PBMLtwocol}{\begin{multicols}{2}\parindent\z@\parskip\baselineskip}%
                           {\end{multicols}}

% For PBML board text written in two columns of equal with and full width below
% \PBMLboardtexts[corr]{left column}{right column}{text below}
% corr is an optional dimension (default 0mm) which will be added to the calculated width
% of the left column and subtracted from the width of the right column

\newcommand\PBMLboardtexts[4][\z@]{{\pbml@ltocwidth .5\linewidth
  \advance\pbml@ltocwidth #1
  \pbml@rtocwidth\linewidth
  \advance\pbml@rtocwidth-\pbml@ltocwidth
  \sf \parindent\z@
  \def\title##1{\vskip 3\baselineskip \textbf{##1}\par\smallskip}%
  \noindent\vtop{\hsize\pbml@ltocwidth #2}\vtop{\hsize\pbml@rtocwidth #3}\vfill #4\par}}

\def\booknotices{\title{BOOK NOTICES}\pbml@makeheader
    {\pbml@thanks
      \centering \Large \sf \bfseries \pbml@title\par}\vskip 24\p@ minus 9\p@
     \pbml@def title
     \markright{\hfill\pbml@pstitle\ (\pbml@pagerange)}%
     \pbml@mkcpr{\textcopyright\ \pbml@year\ PBML. All rights reserved.}}

\def\PBMLnote#1{\title{NOTE}\pbml@makeheader
    {\pbml@thanks
      \centering \Large \sf \bfseries \pbml@title\par}\vskip 24\p@ minus 9\p@
     \pbml@def title
     \setkeys{PBMLbook}{#1}%
     {\parindent\z@ \Large \sf \textbf{\pbml@book@title}\par
      \ifx\pbml@book@author\undefined\else
        \bigskip
        \large\pbml@book@author\par
      \fi}\vskip 24ptminus 5pt
     \markright{\hfill\pbml@pstitle\ (\pbml@pagerange)}%
     \pbml@mkcpr{\textcopyright\ \pbml@year\ PBML. All rights reserved.}}

\def\PBMLreview#1{\title{REVIEWS}\pbml@makeheader
    {\pbml@thanks
      \centering \Large \sf \bfseries \pbml@title\par}\vskip 24\p@ minus 9\p@
     \pbml@def title
     \setkeys{PBMLbook}{#1}%
     {\parindent\z@ \Large \sf \textbf{\pbml@book@title}\par\bigskip
      \large\pbml@book@author\par\bigskip
      \normalsize\pbml@book@published\par\bigskip
      Reviewed by \pbml@book@reviewed\par}\vskip 24ptminus 5pt
     \markright{\hfill\pbml@pstitle\ (\pbml@pagerange)}%
     \pbml@mkcpr{\textcopyright\ \pbml@year\ PBML. All rights reserved.}}

\define@key{PBMLbook}{title}{\def\pbml@book@title{#1}}
\define@key{PBMLbook}{author}{\def\pbml@book@author{#1}}
\define@key{PBMLbook}{published}{\def\pbml@book@published{#1}}
\define@key{PBMLbook}{noticed}{\def\pbml@book@noticed{#1}}
\define@key{PBMLbook}{reviewed}{\def\pbml@book@reviewed{#1}}

\newenvironment{booknotice}[1]{\setkeys{PBMLbook}{#1}\hrule height \p@
   \vskip 120pt \penalty -200 \vskip -108pt
   {\parindent\z@ \sffamily
    \large \textbf{\pbml@book@title}\par \bigskip
    \normalsize \pbml@book@author \par \bigskip
    \small \pbml@book@published \par
    \expandafter\ifx\csname pbml@book@noticed\endcsname\relax \else
      \bigskip Notice by \pbml@book@noticed \par
    \fi
    \vskip 12pt minus 3pt}\small}{\par\bigskip}

\define@key{PBMLtoc}{title}{\def\pbml@toc@title{#1}}
\define@key{PBMLtoc}{subtitle}{\def\pbml@toc@subtitle{#1}}
\define@key{PBMLtoc}{authors}{\def\pbml@toc@authors{#1}}
\define@key{PBMLtoc}{page}{\def\pbml@toc@page{#1}}

\def\PBMLtocline#1{{\setkeys{PBMLtoc}{#1}\noindent\vtop{\hsize\pbml@ltocwidth \raggedright
    \textbf{\pbml@toc@title}%
    \expandafter\ifx\csname pbml@toc@subtitle\endcsname\relax\else
      \\\pbml@toc@subtitle
    \fi
    \expandafter\ifx\csname pbml@toc@authors\endcsname\relax\else
      \\\textit{\pbml@toc@authors}%
    \fi}\vtop{\hsize\pbml@rtocwidth \raggedleft \pbml@toc@page}\par}}

\newdimen\pbml@ltocwidth
\newdimen\pbml@rtocwidth
\newenvironment{PBMLtoc}[1][1cc]{\parindent\z@ \parskip.5\baselineskip \sf
  \pbml@rtocwidth#1
  \pbml@ltocwidth\linewidth
  \advance\pbml@ltocwidth-#1}{\par}

\def\PBMLtoctext#1{{\LARGE\bfseries #1}\par}

% Macros for footnotes and copyright

\def\pbml@nofootnote{\typeout{\string\footnote\space is not allowed here.^^J%
    Use \string\thanks\space instead.}}

\let\thanks\@gobble
\def\pbml@thanks{\let\footnote\pbml@nofootnote}

\footnotesep\baselineskip

\def\pbml@mkcpr#1{\setbox\pbml@C@box=\vbox{\footnotesize\sffamily
    \noindent\vrule width \z@ height \footnotesep
    #1\par}\pbml@C@ht=\ht\pbml@C@box
    \advance\pbml@C@ht\skip\footins
    \divide\pbml@C@ht\baselineskip
    \multiply\pbml@C@ht\baselineskip
    \advance\pbml@C@ht\baselineskip
    \advance\pbml@C@ht-\footskip
    \expandafter\enlargethispage\expandafter{\expandafter-\the\pbml@C@ht}%
    \thispagestyle{PBMLtitle}%
    \advance\pbml@C@ht\footskip}

\newbox\pbml@C@box
\newdimen\pbml@C@ht

\renewcommand\@makefntext[1]{\parindent 14\p@ \noindent \hb@xt@ \parindent{\hss \@makefnmark}#1}

% Page styles, optionaly with crop marks, not implemented yet

\def\ps@PBMLempty{\ps@empty
    \def\@oddfoot{\pbml@maybe@crop\oddsidemargin\hfill}%
    \def\@evenfoot{\pbml@maybe@crop\evensidemargin\hfill}}

\def\ps@PBMLtitle{\ps@PBMLempty
    \def\@oddfoot{\pbml@maybe@crop\oddsidemargin\vbox to \z@{\vss
    \ifnum\c@footnote=\z@ \footnoterule \fi \copy\pbml@C@box}}}

\def\ps@PBML{%
  \def\@evenhead{\normalfont \normalsize \sf \leftmark}%
  \def\@oddhead{\normalsize \normalsize \sf \rightmark}%
  \def\@evenfoot{\pbml@maybe@crop\evensidemargin
      \normalfont \normalsize \sf \thepage \hfill}%
  \def\@oddfoot{\pbml@maybe@crop\oddsidemargin
      \normalfont \normalsize \sf \hfill \thepage}}

\def\ps@PBMLnonumber{%
  \def\@evenhead{\normalfont \normalsize \sf \leftmark}%
  \def\@oddhead{\normalsize \normalsize \sf \rightmark}%
  \def\@oddfoot{\pbml@maybe@crop\oddsidemargin\hfill}%
  \def\@evenfoot{\pbml@maybe@crop\evensidemargin\hfill}}

\def\pbml@leftmark{PBML
    \pbml@number \hfill \pbml@month\space\pbml@year}

\AtBeginDocument{\expandafter\markboth\expandafter{\pbml@leftmark}{}}

\pagestyle{PBML}

% No crop marks as default

\let\pbml@maybe@crop\@gobble

% Macro for crop marks

\newdimen\pbml@left
\newdimen\pbml@low
\pbml@low\paperheight
\advance\pbml@low -\footskip
\advance\pbml@low -\textheight
\advance\pbml@low -\headsep
\advance\pbml@low -\headheight
\advance\pbml@low -\topmargin
\advance\pbml@low -1in

\def\pbml@cropmarks#1{\pbml@left 1in \advance\pbml@left #1
    \lower\pbml@low\hbox to \z@ {\hss \unitlength 1mm \thinlines
          \begin{picture}(0,0)
          \put(0,-5){\line(0,-1){5}}
          \put(-5,0){\line(-1,0){5}}
          \put(170,-5){\line(0,-1){5}}
          \put(175,0){\line(1,0){5}}
          \put(0,245){\line(0,1){5}}
          \put(-5,240){\line(-1,0){5}}
          \put(170,245){\line(0,1){5}}
          \put(175,240){\line(1,0){5}}
          \put(5,-6){\makebox(0,0)[lt]{\normalfont \normalsize \sf
              PBML \pbml@number, \pbml@month\ \pbml@year\quad strana \thepage}}
          \put(165,-6){\makebox(0,0)[rt]{\normalfont \normalsize \sf \today}}
          \end{picture}\hskip \pbml@left}}

\def\pbml@enable@cropmarks{\let\pbml@maybe@crop\pbml@cropmarks
    \hoffset 1cm
    \voffset \hoffset
    \advance\paperheight 2\voffset
    \advance\paperwidth 2\hoffset}

\expandafter\ifx\csname pbml@use@cropmarks\endcsname\relax
\else \pbml@enable@cropmarks \fi

% Counter for included files, I do not want to rely on chapterbib

\newcount\pbml@tempcntber

% Make the prefix

\def\pbml@makeprefix{\global\advance\pbml@tempcntber\@ne
    \edef\pbml@prefix{PBML@@\number\pbml@tempcntber}}

% Conditional inclusion

\newcommand*\PBMLonly[2][]{%
  \ifcat$#1$\includeonly{#2}\else\includeonly{#1/#2}\fi}

\newif\ifpbml@include@

% Include file, usage: \PBMLinclude[directory]{file}

\def\PBMLinclude{\@ifstar\pbml@false\pbml@true}
\def\pbml@false{\pbml@include@false\pbml@include}
\def\pbml@true{\pbml@include@true\pbml@include}
\newcommand*\pbml@include[2][]{\ifcat$\pbml@prefix$
    \openout\pbml@only=\jobname.only
  \fi
  \pbml@makeprefix
  \begingroup
  \renewcommand\documentclass[2][PBML]{}%
  \renewcommand\usepackage[2][PBML]{}%
  \let\input\PBMLinput
  \renewenvironment{document}{}{\pbml@endarticle\cleardoublepage}%
  \ifcat$#1$%
    \def\pbml@fn{#2}%
    \ifpbml@include@\write\pbml@only{\string\PBMLonly{#2}}\fi
  \else
    \def\pbml@fn{#1/#2}\graphicspath{{#1/}}\def\pbml@input@path{#1/}%
    \ifpbml@include@\write\pbml@only{\string\PBMLonly[#1]{#2}}\fi
  \fi
  \include{\pbml@fn}
  \@tempcntb\c@page
  \advance\@tempcntb\m@ne
  \immediate\write\@auxout{\string\PBMLenddef{\pbml@prefix}{\number\@tempcntb}}
  \endgroup}

% Macros for user defined counters (otherwise \PBMLonly will report errors)

\let\pbml@setcounter\setcounter
\def\setcounter#1{\@ifundefined{c@#1}{\pbml@newcounter{#1}}{}\pbml@setcounter{#1}}
\let\pbml@newcounter\newcounter
\def\newcounter#1{\@ifundefined{c@#1}{\pbml@newcounter{#1}}{\pbml@setcounter{#1}{0}}}

% Modify input path so that files included from a subdir find images etc.

\def\pbml@input@path{}
\def\PBMLinput#1{\pbml@saved@input{\pbml@input@path #1}}
\let\pbml@saved@input\input

% Hacks for finding the page range, requires reprocessing (similarly as \label and \ref)

\def\PBMLbegdef#1#2{\expandafter\gdef\csname#1@@firstpage@@\endcsname{#2}}
\def\PBMLenddef#1#2{\expandafter\gdef\csname#1@@lastpage@@\endcsname{#2}}

\AtEndDocument{\write\@auxout{\string\PBMLenddef{}{\number\c@page}}}

\newwrite\pbml@only

% End-of-article hook

\newtoks\pbml@endtoks
\def\AtEndArticle#1{\global\pbml@endtoks\expandafter{\the\pbml@endtoks #1}}
\def\pbml@endarticle{\the\pbml@endtoks \global\pbml@endtoks{}}
\def\PBMLemptylast{\AtEndArticle{\clearpage\thispagestyle{PBMLnonumber}}}

% This must be the last line!

\InputIfFileExists{\jobname.inc}{}{}
\endinput
