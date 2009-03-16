## Show W81 DRP

library(sjedrp)
data.dir <- "~/mosaics/data/"
m623.on <- as.matrix(read.table(paste(data.dir, "M623-ON.dat", sep=""),
                            header=F,sep="", skip=0))

m623.of <- as.matrix(read.table(paste(data.dir, "M623-OF.dat", sep=""),
                                header=F,sep="", skip=0))

m623.w <- c(20, 1022, 35, 1116)
res <- autodrp(m623.of[,1], m623.of[,2], 20, 10)

plot.sjedrp <- function (x, scale = 1, title = NULL) 
{
    hts <- (x$ds * scale)
    last.bin <- x$nbins * x$r
    plot.label <- paste(title, "eff rad", signif(x$effrad, 3), 
        "pack", signif(x$p, 3), "maxr", signif(x$maxr, 3), "rel", 
        signif(x$k, 3))
    barplot(hts, col = "orangered", space = 0, width = x$r,
            yaxt='n', ylim=c(0,120),
            xlim = c(0, last.bin)) ##, main = plot.label)
    lines(c(0, last.bin), c(x$density, x$density) * scale, col='blue', lty=2)
    ##axis(1, at = c(0, last.bin))
    axis(1, at = c(0, 50, 100, 150, 200), mgp=c(2,0.5,0))
    axis(2, at = seq(from=0, to=100, by=20), mgp=c(2,0.7,0))
    lines(c(x$effrad, x$effrad), scale * c(0, x$density))
    points(c(x$maxr), c(0), pch = "|")
}

postscript(file="|psfbb> w81on_drp.ps", width=6, height=3, horiz=F)
par(mar=c(2.5,3,1,1), las=1)
plot(res, scale=1e6)
title(xlab=expression(paste("distance (", mu, "m)")), mgp=c(1.5,0.7,0))
title(ylab=expression(paste("density (", mm^{-2}, ")")), mgp=c(2,1,0))
text(x=35, y=105, "DRP for M623 (off)")
dev.off()
