#rch=x64_64!/bin/bash

source /home/zli/work/dirlist

qsubHdr=/home/zli/work/qsubhdr
outdir=./out
java_decoder_root=/home/zli/release_mtdecoder/zhifei/

sourcelist=/home/zli/work/zli@gale/mt06-data/gale_p3_run/lm.list.withweights #contain lines with format: lm_file;weight
fini_common=/home/zli/work/zli@gale/mt06-data/gale_p3_run/config.template
fremote_lm_list=/home/zli/work/zli@gale/mt06-data/gale_p3_run/remote.lm.server.list
>$fremote_lm_list


while read line; do #for each data source
    flm=${line%;*}
    weight=${line#*;}		
    fini=$flm.config
    basename=`basename $flm`
    tag=$basename
    
    sh=$outdir/lmserver-$tag.sh
    cat $qsubHdr > $sh  
    
    #create an ini file
    echo "cat $fini_common > $fini" >> $sh
    echo "export lzf_hostname=\`hostname\`" >> $sh
    echo "echo hostname=\$lzf_hostname >> $fini" >> $sh
    echo "echo lm_file=$flm >> $fini" >> $sh
    echo "echo interpolation_weight=$weight >> $fini" >> $sh
    
    #load the lm
    echo "ulimit -s 1024" >> $sh #set the stack size as 2048k, linux default is  10240k
    echo "cd $java_decoder_root" >> $sh
    echo "/apps/share/jdk1.6.0_03/bin/java -server -Xmx128m -Xms128m -Xss1024k edu.jhu.ckyDecoder.LMServer $fini" >> $sh
    
    echo "$fini" >> $fremote_lm_list
  
    chmod 755 $sh
    cat $sh
    ferr=$outdir/err-lmserver-$tag
    rm $ferr
    depends="nothing"
    #qsub -hard -l greedy=2 -q all.q@z10.clsp.jhu.edu -l ram_free=3.0G -hold_jid $depends -j y -o $outdir/err-lmserver-$tag $sh
    qsub -hard -l greedy=2 -l cpu_arch=x86_64 -l mem_free=2.6G -hold_jid $depends -j y -o $ferr $sh
  # qsub -hard -l greedy=2 -l ram_free=3.0G -hold_jid $depends -j y -o $outdir/err-lmserver-$tag $sh

    #qsub -hard -l greedy=2 -l cpu_arch=x86_64 -l ram_free=2.5G -hold_jid $depends -j y -o $outdir/err-lmserver-$tag $sh

done < $sourcelist


