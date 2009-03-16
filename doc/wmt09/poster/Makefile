#
# Makefile to generate infposter LaTeX documents - M.Reddy (MXR), 14/2/97
# Updated by MEF, 5/5/2000
# Updated by neilb, 11/5/2001 to reposition A0 poster on the paper better
# Updated by neilb, 1/5/2002 Fixed and added to scalea? targets.
# Updated by cc 21/7/2003 Added David Sterratt's oversizea0 target
#
# Targets: (assuming BASENAME is defined as "poster")
#
#   make [preview]   - compile LaTeX, convert to EPS, display with xdvi
#   make view        - compile LaTeX, convert to EPS, display with ghostview
#   make scalea0     - scales the EPS to A0 size for output to A0 plotter
#   make scalea3     - scales the EPS to A3 size for better screen previewing
#   make scalea4     - scales the EPS to A4 size for better screen previewing
#   make scaleoversizea0 - scales the EPS to "oversize A0" size for output 
#                          to "A0" plotter in JCMB
#   make clean       - delete all temporary / backup files
#

## You should change the definition of BASENAME to be the base filename
## that you use for your LaTeX document, i.e. if your poster is defined
## in the file "poster.tex", then you should set BASENAME to "poster".

BASENAME=poster

STYLEFILE=poster.cls

## By default, xdvi will be used for screen viewing. You can set "default"
## to "view" if you would rather use ghostview (longer previewing times
## though).

default: scalea4			# alternative = "view"

## By default, the PostScript will be scaled to A4 size for faster and
## more efficient previewing on screen (and A4 printing). If you want to
## generate A0 output, then change "default_scale" to "scalea0"

default_scale: scalea4			# alternative = "scalea0"

## By default, the PostScript file will contain the bitmap versions of
## the Computer Modern font. If you prefer to use the outline versions,
## then you can change "default_fonts" to "outline_fonts"

default_fonts: bitmap_fonts		# alternative = "outline_fonts"


################ NOTHING TO CHANGE BELOW THIS POINT #######################


## define the routines for draft and final on-screen viewing

preview: $(BASENAME).dvi
	@echo "Previewing poster with xdvi..."
	@xdvi -hush -nopost -s 10 $(BASENAME) 2> /dev/null &

view: $(BASENAME)
	@echo "Viewing poster with ghostview..."
	@gv -swap -nocenter $(BASENAME).eps &

## define the targets for generating the DVI and PS files

$(BASENAME): default_scale

$(BASENAME).eps: $(BASENAME).dvi
	@echo "Generating PostScript..."
	@dvips -E $(BASENAME) -o $(BASENAME).eps

$(BASENAME).dvi: $(BASENAME).tex Makefile $(STYLEFILE)
	@echo "Compiling LaTeX source..."
	@latex $(BASENAME)

## routines to scale the EPS file for different page sizes


# Look at http://www.rzg.mpg.de/visualisation/hardware/printers/A0.html
# for these dimensions.
scaleoversizea0: default_fonts
	@echo "Scaling PostScript to Oversize A0..."
	@epsffit -c -m 23 54 2574 3583 $(BASENAME).eps $(BASENAME)-oversize-a0.eps
	@mv $(BASENAME)-oversize-a0.eps $(BASENAME).eps

scalea0: default_fonts
	@echo "Scaling PostScript to A0..."
	@epsffit -c -m 53 80 2328 3288 $(BASENAME).eps $(BASENAME)-a0.eps
	@mv $(BASENAME)-a0.eps $(BASENAME).eps

scalea1: default_fonts
	@echo "Scaling PostScript to A1..."
	@epsffit -c -m 40 40 1644 2344 $(BASENAME).eps $(BASENAME)-a1.eps
	@mv $(BASENAME)-a1.eps $(BASENAME).eps

scalea2: default_fonts
	@echo "Scaling PostScript to A2..."
	@epsffit -c -m 20 20 1150 1644 $(BASENAME).eps $(BASENAME)-a2.eps
	@mv $(BASENAME)-a2.eps $(BASENAME).eps

scalea3: default_fonts
	@echo "Scaling PostScript to A3..."
	@epsffit -c -m 20 20 822 1150 $(BASENAME).eps $(BASENAME)-a3.eps
	@mv $(BASENAME)-a3.eps $(BASENAME).eps

scalea4: default_fonts
	@echo "Scaling PostScript to A4..."
	@epsffit -c -m 20 20 575 822 $(BASENAME).eps $(BASENAME)-a4.eps
	@mv $(BASENAME)-a4.eps $(BASENAME).eps

## Control the use of either bitmap or outline CM fonts

bitmap_fonts: $(BASENAME).eps

outline_fonts: $(BASENAME).dvi
	@echo "Generating PostScript (outline)..."
	@dvips -a -Pcmoutline -E $(BASENAME) $(BASENAME).eps

pdf: $(BASENAME).eps
	ps2pdf $(BASENAME).eps $(BASENAME).pdf

## other sundry options

clean:
	@echo "Removing DVI, EPS, and temporary files..."
	@rm -f $(BASENAME).aux $(BASENAME).log $(BASENAME)*~
	@rm -f $(BASENAME).dvi $(BASENAME).eps $(BASENAME).ps
	@rm -f $(BASENAME).pdf
	@rm -f *.aux *.log *.dvi


# EOF: Makefile
