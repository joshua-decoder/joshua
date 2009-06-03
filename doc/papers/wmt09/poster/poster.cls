%% Thu 29 Dec 2005: Adapted from infposter.cls and ancposter.cls by
%% Stephen Eglen for using in Cambridge University.  Simple change is
%% to display the CU logo rather than Edinburgh University.
%%
%%    File : infposter.cls    (LaTeX2e class file)
%%  Author : Version 1.0 by Martin Reddy (mxr@dcs.ed.ac.uk) 
%%           Version 2.0 by Mary Ellen Foster (mef@cogsci.ed.ac.uk)
%% Version : 2.2
%% Updates : 1.0 [14/2/97] - initial release.
%%           1.1 [14/3/97] - center caption, colour changes
%%           2.0 [4/5/2000] - new layout, incorporating new informatics
%%                            logo.  (Renamed to infposter)
%%           2.1 [11/28/2000] - use new informatics shield, graphics rather
%%                            than graphicx.
%%               2.2 [04/05/2001] - margins reduced to avoid blue frame
%%                              disappearing on A0 printer
%%           2.3 [10/05/2002] - fixed the way sections get redefined; added \email
%               and \homepage commands.
%% 
%% This file contains a class definition, infposter, for the LaTeX2e
%% system which defines the layout of posters in the style of
%% University of Edinburgh.
%%
%% For information on this class, please refer to "texdoc infposter"
%%
%% The following LaTeX packages are required by this class:
%%    pstricks - to embed the background EU crests and border.
%%    graphics - to display all EPS figures
%%    multicol - to produce the 3 column text.
%%    sectsty  - to redefine the font and colour of the headers
%%    url      - to typeset urls properly
%%

\NeedsTeXFormat{LaTeX2e}[1994/12/01]
\ProvidesClass{ancposter}[2000/05/05 v2.3 ANC Poster Class (MEF, AJG)]

%% tweaks to permit use with FoilTeX 2.1
\newif\if@magscaleECfonts \@magscaleECfontsfalse
\newif\if@useDCfonts \@useDCfontsfalse

%%
%% --- Initial Code ---
%%

\newlength{\posterwidth}
\newlength{\posterheight}
  \setlength\posterheight{840mm}
  \setlength\posterwidth{1188mm}

% MEF added these
\newcommand{\poster@columns}{}
\newcommand{\poster@depturl}{\url{http://www.informatics.ed.ac.uk/}}
\newcommand{\poster@deptimage}{Logo1.eps}
\newcommand{\poster@ancimage}{ANCnet.ps}
\newcommand{\poster@deptimageheight}{1.5in}
\newcommand{\poster@ancimageheight}{7.8in}
\newcommand{\poster@mainimage}{joshua-logo.eps}
\newcommand{\poster@mainedimage}{joshua-logo.eps}
\newcommand{\poster@deptname}{}
\newcommand{\poster@centre}{}

\newif\if@posterland
% Various possible layout configurations and their defaults (many of these
% cannot be set to otherwise, but I'll leave the code in just in case)
\newif\if@posteroverlap \@posteroverlaptrue
\newif\if@posterborder \@posterbordertrue
\newif\if@postercolumns \@postercolumnstrue
\newif\if@postershield \@postershieldtrue
\newif\if@posterundershield \@posterundershieldfalse

\newif\if@posteranctop \@posteranctoptrue
\newif\if@posterancaddresbox \@posterancaddresboxtrue
\newif\if@posterancnoback \@posterancnobackfalse
\newif\if@posteranced \@posterancedfalse

\newcommand{\dson}{\renewcommand{\baselinestretch}{1.5}\large\Large}
\newcommand{\dsoff}{\renewcommand{\baselinestretch}{1}\large\Large}

%%
%%  --- Options ---
%%


\DeclareOption{center}{\renewcommand{\poster@centre}{yes}}
\DeclareOption{centre}{\renewcommand{\poster@centre}{yes}}

%% Added one-column option
\DeclareOption{onecolumn}{\@postercolumnsfalse}
\DeclareOption{twocolumn}{\@postercolumnstrue\renewcommand{\poster@columns}2}
\DeclareOption{threecolumn}{\@postercolumnstrue\renewcommand{\poster@columns}3}
\DeclareOption{fourcolumn}{\@postercolumnstrue\renewcommand{\poster@columns}4}

\DeclareOption{landscape}{\@posterlandtrue}
\DeclareOption{portrait}{\@posterlandfalse}

\DeclareOption{anctop}{\@posteranctoptrue}
\DeclareOption{ancbottom}{\@posteranctopfalse}
\DeclareOption{addressbox}{\@posterancaddresboxtrue}
\DeclareOption{addressline}{\@posterancaddresboxfalse}
\DeclareOption{noback}{\@posterancnobacktrue}
\DeclareOption{edback}{\@posterancedtrue}
\DeclareOption{infback}{\@posterancedfalse}

% Should the poster have a border around it?
\DeclareOption{border}{%
    \@posterbordertrue
}
\DeclareOption{noborder}{%
    \@posterborderfalse
}

% Default options
\ExecuteOptions{threecolumn,landscape}
\ProcessOptions

%%
%% --- Class Loading (built ontop of article.cls) ---
%%

\LoadClass{article}

%%
%% --- Main Code ---
%%

\usepackage{pstcol}  % load pstricks and LaTeX color together [DCS, on
                     % suggestion of Stephen J Eglen]
                     % This fixes the problem with the column
                     % separators changing to the SectionColour of a
                     % section on the right-hand side of the separator.
% Epsfig retained for compatibility, but all graphics are included through
% graphicx
\usepackage{epsfig}
\usepackage{graphicx}
\usepackage{multicol}

%%SJE: no need for EU shield.
%%\usepackage{eushield} \shieldtype{4}
% Added this in case urls are to be displayed
\usepackage{url}
% A better way to redefine section headers!
\usepackage{sectsty}

%% Set up the default colours for the poster.
%% These can be modified directly by the user

\newcommand{\ItemizeColour}{\color{magenta}}
\newcommand{\SectionColour}{\color{blue}}
\newcommand{\TitleColour}{\color{black}}
\newcommand{\SubtitleColour}{\color{black}}
\newcommand{\AuthorColour}{\color{black}}
\newcommand{\AddressColour}{\color{black}}
\newcommand{\AbstractColour}{\color{black}}
\newcommand{\CaptionColour}{\color{red}}

%% provide a number of lengths to let the user
%% customise the spacing of the poster

\newlength{\SpaceAfterTitle}
\newlength{\SpaceAfterSubtitle}
\newlength{\SpaceAfterAuthor}
\newlength{\SpaceAfterAbstract}
\newlength{\SpaceBeforeText}
\newlength{\SpaceToColumnSep}
\newlength{\SpaceTemp}

\setlength{\SpaceToColumnSep}{5em}

\newenvironment{posterindent}
   {\list{}{\setlength\leftmargin\SpaceToColumnSep%
   \setlength\rightmargin\leftmargin}\item\relax}
   {\endlist}

%% keep a track of whether \makeposter has been called. If it has
%% then we need to do an \end{multicols}

\def\poster@true{1}
\def\poster@false{0}
\let\poster@displayed\poster@false

% End whatever environments we're in (the exact ones depend on the options)
\AtEndDocument{%
  \if\poster@displayed\poster@true \end{posterindent} 
    \if@postercolumns
      \end{multicols} 
    \fi%
  \fi%
}

%% setup the multicols environment

\columnseprule=.5mm
\flushcolumns

%% Setup some really big fonts. This is a pain to do, so I'm just
%% going to use the style files from the FoilTeX system which already
%% do this.

\input{fltfonts.def}
%% ADJUST BACK TO NORMAL EMPH!
\DeclareRobustCommand\em{\@nomath\em \ifdim \fontdimen\@ne\font >\z@
     \upshape \else \itshape \fi}
\input{foil30.clo}
\renewcommand\familydefault{\rmdefault}

% [DCS] Foiltex redefines \@list.  Change it back to the setting used by
% article so that natbib can work with the class
\let\@listi\@listI

% Modify the FoilTeX defs a bit so we can have *really* big font 
% sizes for the main title and sub title.

\DeclareFontShape{OT1}{fcmr}{bx}{n}{%
  <12><14.4><17.28><20.74><24.88><29.86><35.83><43.00><51.60><72>cmbx10}{}

\DeclareFontShape{OT1}{fcmr}{bx}{sl}{%
  <12><14.4><17.28><20.74><24.88><29.86><35.83><43.00><51.60><64>cmbxsl10}{}

%% \newcommand{\poster@maintitlefont}{%
%%   \fontencoding{OT1}\fontfamily{fcmr}\fontseries{bx}%
%%   \fontshape{n}\fontsize{72}{84}\selectfont%
%% }


\newcommand{\poster@maintitlefont}{%
  \fontfamily{ptm}\bfseries%
  \fontsize{72pt}{84pt}\selectfont%
}

\newcommand{\poster@subtitlefont}{%
  \fontencoding{OT1}\fontfamily{fcmr}\fontseries{bx}%
  \fontshape{sl}\fontsize{64}{72}\selectfont%
}

%% define the page size and various margins/offsets

\setlength{\topmargin}{0in}
\setlength{\oddsidemargin}{27.1mm}
\setlength{\evensidemargin}{27.1mm}

\setlength{\footskip}{0.5in}
\setlength{\headheight}{0in}
\setlength{\headsep}{0in}
\setlength{\voffset}{0in}
\setlength{\hoffset}{0in}

\if@posterland
  \typeout{Landscape Poster}
  \special{papersize=1188mm,840mm}
\else
  \typeout{Portrait Poster}
  \newlength\poster@tmpw
  \setlength\poster@tmpw\posterwidth
  \setlength\posterwidth\posterheight
  \setlength\posterheight\poster@tmpw
  \special{papersize=840mm,1188mm}
\fi


\setlength{\textwidth}{0.93\posterwidth}
\setlength{\textheight}{0.91\posterheight}

%% Define lengths for the poster border, shield position, etc.

\newlength{\borderheight}
\setlength{\borderheight}{\posterheight}
\addtolength{\borderheight}{-2.166in}

\newlength{\borderwidth}
\setlength{\borderwidth}{\posterwidth}
\addtolength{\borderwidth}{-2.5in}

\newlength{\shieldoffset}
\setlength{\shieldoffset}{\posterwidth}
%% SJE: -5.5in for Edinburgh.
\addtolength{\shieldoffset}{-8.5in}


\newlength{\ancoffset}
\setlength{\ancoffset}{\shieldoffset}
\if@posteranctop
  \addtolength{\ancoffset}{-5.0in}
\fi

%% Add a \special for dvips to generate correct PS
%%   a4 210mm 297mm, a3 297mm 420mm, a2 420mm 594mm
%%   a1 594mm 840mm, a0 840mm 1188mm  (33.07in x 46.77in)


%% Specify the embedded PS layout to do at the start of the document
%% This includes the greyed-logo and colour shield in the top corners
%% and the blue frame around the poster.

\AtBeginDocument{%
  % Put in the ANC logo
  \if@posterland
    \if@posteranctop 
      \rput(\ancoffset,-3.8in){
  % DCS: In case you are wondering, yes you do have to use the graphicx 
  % syntax here since the resizebox, includegraphics* form doesn't seem 
  % to clip the image to the right dimensions.
  %% SJE: comment next out.
  % \includegraphics[clip=true,bb=14 14 645 801,height=\poster@ancimageheight]{\poster@ancimage}
      }%
    \else
      %\rput(41.5in,-28in){
      \rput(\ancoffset,-28in){
        %%\resizebox{!}{\poster@ancimageheight}{\includegraphics{\poster@ancimage}}
      }%
    \fi
  \else
      \rput(28in,-42in){
        %%\resizebox{!}{\poster@ancimageheight}{\includegraphics{\poster@ancimage}}
      }%
  \fi
  % EU shield
  \if@postershield
    \rput(\shieldoffset,-2.1in){
      %%SJE: add Cambridge Shield.
      %% \resizebox{5in}{!}{\includeshield}
      %% jhu-logo downloaded from
      %% http://www.cam.ac.uk/localusersonly/cambuniv/docstyle/
      \includegraphics[width=25cm]{jhu-logo}
    }%
  \fi  
  % Put in the logo
  \if@posterancnoback \else
    \if@posterland
      \if@posteranced
        \rput(6.0in,-7.5in){
          \resizebox{!}{15in}{\includegraphics{\poster@mainedimage}}
        }%
      \else
        \rput(5in,-15.5in){
          \resizebox{!}{\textheight}{\includegraphics{\poster@mainimage}}
        }%
      \fi
    \else
      \if@posteranced
        \rput(6.0in,-7.5in){
          \resizebox{!}{15in}{\includegraphics{\poster@mainedimage}}
        }%
      \else
        \rput(7in,-22in){
          \resizebox{!}{\textheight}{\includegraphics{\poster@mainimage}}
        }%
      \fi
    \fi
  \fi
  % If there's a border, draw it
  \if@posterborder
    \psframe[linewidth=1mm,linecolor=blue,framearc=0.004,fillstyle=none]%
    (-0.5in,-\borderheight)(\borderwidth,0.166in)%
  \else
  \fi
  % Now we will add some empty boxes in order to put TeX stuff beyond
  % all the pstricks stuff, otherwise it gets ignored by dvips -E
  % Notice: this causes "overfull hbox" errors, but no problems
  \hskip -1in\hbox{\white\vrule height0.7in depth0pt width1pt}%
  \setlength\SpaceTemp{\posterwidth}%
  \addtolength\SpaceTemp{-1in}%
  \hskip \SpaceTemp%
  \hbox{\white\vrule height0.7in depth0pt width1pt}%
}

%% Macros to input the poster titles, author, etc.

\newcommand{\poster@title}{}
\newcommand{\poster@subtitle}{}
\newcommand{\poster@author}{}
\newcommand{\poster@address}{}
\newcommand{\poster@email}{}
\newcommand{\poster@url}{}
\newcommand{\poster@abstract}{}
\newcommand{\poster@empty}{}

\newcommand{\subtitle}[1]{\renewcommand{\poster@subtitle}{#1}}
\newcommand{\enquiries}[1]{\renewcommand{\poster@email}{#1}}
\newcommand{\email}[1]{\renewcommand{\poster@email}{\url{#1}}}
\newcommand{\homepage}[1]{\renewcommand{\poster@url}{\url{#1}}}
\newcommand{\address}[1]{\renewcommand{\poster@address}{#1}}
\renewcommand{\title}[1]{\renewcommand{\poster@title}{#1}}
\renewcommand{\author}[1]{\renewcommand{\poster@author}{#1}}
\renewcommand{\abstract}[1]{\renewcommand{\poster@abstract}{#1}}

\newlength{\titlewidth}
% Take into account overlapping (probably to be deleted)
\if@posteroverlap
  \setlength{\titlewidth}{.75\textwidth}
\else
  \setlength{\titlewidth}{\textwidth}
  \addtolength{\titlewidth}{-\poster@mainindent}
  \setlength{\titlewidth}{.75\titlewidth}
\fi

%% SJE: width of the author address etc.
\newlength{\authorwidth}
\newlength{\emailwidth}
\if@posterland
  \setlength{\authorwidth}{.90\titlewidth}
  \setlength{\emailwidth}{.45\titlewidth}
\else
  \setlength{\authorwidth}{.65\titlewidth}
  \setlength{\emailwidth}{.35\titlewidth}
\fi

\if@posterancaddresbox \else
  \setlength{\authorwidth}{.95\titlewidth}
  \setlength{\emailwidth}{.95\titlewidth}
\fi

%% Define the \makeposter command which generates the poster header.
%% We also keep a track of whether this is called or not so that we
%% know if we are to do anything at the end of the document.

\newcommand{\makeposter}{%
  \if\poster@displayed\poster@false
    \let\poster@displayed\poster@true
    % Indent if we're not to overlap (probably to be deleted)
    \if@posteroverlap \else \begin{mainposterindent} \fi
    \begin{posterindent}
      % If we're supposed to skip a shield, do it (probably to be deleted)
      \if@posterundershield
        \vspace*{7in}
      \fi
      % \begin{minipage}{0pt}\vspace*{5in}\hfill\end{minipage}
      \begin{minipage}{\titlewidth}
        \ifx\poster@title\poster@empty \typeout{WARNING: No Title} \else
          \vskip -20pt{\TitleColour\poster@maintitlefont\poster@title}%
          \vskip \SpaceAfterTitle%
        \fi
        \ifx\poster@subtitle\poster@empty \else
          \vskip 20pt{\SubtitleColour\poster@subtitlefont\poster@subtitle}%
          \vskip \SpaceAfterSubtitle%
        \fi
      \end{minipage}\\
      % THE AUTHOR
      \begin{minipage}{\authorwidth}
        \ifx\poster@author\poster@empty \else
           \vskip 20pt{\AuthorColour\Huge\bfseries\em\poster@author}% 
        \fi
      \end{minipage}\\
      % THE ADDRESS
      \begin{minipage}[t]{\authorwidth}
          \ifx\poster@address\poster@empty \else
              \dson
              \vskip 20pt{\Large\textsl{\poster@address}}
              \dsoff
          \fi
          \vskip \SpaceAfterAuthor%
        \ifx\poster@abstract\poster@empty \else
          \vskip 50pt{\AbstractColour\large\bfseries\underbar{Abstract}:
          \poster@abstract}\vskip \SpaceAfterAbstract%
        \fi
        \if@posterancaddresbox
          \advance\SpaceBeforeText 50pt
          \vskip \SpaceBeforeText\ \ 
        \fi
      \end{minipage}
      \if@posterancaddresbox 
        \hskip 120pt
      \else
        \\
      \fi
      \begin{minipage}[t]{\emailwidth}
        \if@posterancaddresbox
          \vskip 20pt
        \else
          \vskip 12pt
        \fi
        \ifx\poster@author\poster@empty \else
          \ifx\poster@email\poster@empty \else
            \if@posterancaddresbox
              {\Large\poster@email}
            \else
              {\Large\poster@email\/,}
            \fi
          \fi
          %% No need for URL, people will find me.
%%           \ifx\poster@url\poster@empty \else
%%             \if@posterancaddresbox
%%               \vskip 12pt{\Large\poster@url}
%%             \else
%%               \hskip 20pt
%%               {\Large\poster@url}
%%             \fi
%%          \fi
        \fi
        \if@posterancaddresbox \else
          \advance\SpaceBeforeText 50pt
          \vskip \SpaceBeforeText\ \ 
        \fi
      \end{minipage}
    \end{posterindent}
    \ifx\poster@centre\poster@empty\else\vfil\fi
    % If not one column, begin a multicols environment
    \if@postercolumns
    %%SJE: add next line to get a little bit of space between logo
    %%and the text.
    \vspace*{0mm}
      \begin{multicols}{\poster@columns}
    \fi
    \begin{posterindent}                % closed by AtEndDocument
  \else
    \typeout{WARNING: you can only call maketitle once per document}
  \fi
}
\let\maketitle\makeposter

%% do the standard header/footer stuff

\newcommand{\ps@firstpage}{%
  \let\@evenhead=\@empty
  \let\@evenfoot=\@empty
  \let\@oddhead=\@empty
  \renewcommand{\@oddfoot}{%
    \small \vbox to \topskip {\vss
      \begin{flushright}
        \leavevmode 
%          \resizebox{!}{\poster@deptimageheight}{%
%            \includegraphics{\poster@deptimage}}
      \end{flushright}%
    \vss}%
    % Now we will add an empty box in order to put TeX stuff beyond
    % all the pstricks stuff, otherwise it gets ignored by dvips -E
    \hskip -1pt\hbox{\white\vrule height0pt depth1.5in width1pt}%
  }%
}

\pagestyle{empty}
\thispagestyle{firstpage}

%% redefine sectioning, captioning, etc. to be in colour and unnumbered

\sectionfont{\SectionColour\Large\bfseries}
\subsectionfont{\SectionColour\large\bfseries}
\setcounter{secnumdepth}{-1}


\renewcommand{\caption}[1]{%
  \vbox{\begin{posterindent}\begin{center}%
  {\CaptionColour\em #1}\end{center}\end{posterindent}}
}

\renewcommand{\labelitemi}{\ItemizeColour$\bullet$}
\renewcommand{\labelitemii}{\ItemizeColour\bfseries --}
\renewcommand{\labelitemiii}{\ItemizeColour$\ast$}
\renewcommand{\labelitemiv}{\ItemizeColour$\cdot$}

%% provide a simple mechanism to incorporate EPS figures
%% \epsfigure[width-percentage]{filename}{caption}

\newdimen\poster@figwidth
\newcount\poster@figmult

\newcommand{\epsfigure}[3][75]{%
  \poster@figmult = \number#1
  \ifnum#1<5
     \typeout{WARNING: Figure width must be >= 5 percent. Resetting to 75.}
     \poster@figmult = 75
  \else \ifnum#1>100
     \typeout{WARNING: Figure width must be <= 100 percent. Resetting to 75.}
     \poster@figmult = 75
  \fi\fi
  \poster@figwidth = \linewidth
  \divide\poster@figwidth by 100
  \multiply\poster@figwidth by \poster@figmult
  \begin{center}\vbox{%
     \vskip 25pt plus 5pt minus 5pt
     \epsfig{file=#2,width=\poster@figwidth}
     \vskip 25pt plus 5pt minus 5pt
     \caption{#3}}
  \end{center}%
}

%% Turn off page numbering and setup paragraph spacing etc. 

\parindent=0in
\parskip=30pt

%%
%% EOF: infposter.cls
%%

