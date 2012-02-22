library(compiler)
library(parallel)

VERBOSE<-F

#' Evaluates a given data frame for classification performance
#' 
#' @param d represents a frame, from which data is gathered
#' @param v represents a column matrix, in which the name of the slots 
#'        that are used for the plots of the data frame can be specified. The last
#'        column represents whether high values are good or not.
evaluate.def<-function(d,v)
{
	if (nrow(d)==0)
	{
		return()
	}
	
	colnames(v)<-c("short","full","high.is.good")

  	eval.single<-function(i)
  	{
    	# get the type
    	type<-v[i,1]
    
    	if (VERBOSE) {message(type);}
    
    	# get primary and optional secondary values
    	if (type == "resnik.avg.p.opt")
    	{
    	  primary.values<-d$resnik.avg.p
    	  secondary.values<-1-d$label
    	} else
    	{
      	primary.values<-d[,type]
      
      	if (type == "resnik.avg.p")
      	{
      	  secondary.values<- -d$resnik.avg
      	} else if (type == "lin.avg.p")
      	{
      	  secondary.values<- -d$lin.avg
      	} else if (type == "jc.avg.p")
      	{
      	  secondary.values<- -d$jc.avg.p
      	} else
      	{
        	secondary.values<-NA
    	  }
    	}
    
    	# get the order
    	if (is.na(secondary.values[1]))
    	{
    	  ord<-order(primary.values,decreasing=as.logical(v[i,3]))
    	} else
    	{
    	  ord<-order(primary.values,secondary.values,decreasing=as.logical(v[i,3]))
    	}
    
    	# data is ordered. Threshold is such that the values
    	# above an element are flagged as positive (inclusive)
    	# and values below an element as negative.
    	values<-primary.values[ord]
    	labels<-d[ord,]$label
    
    	tps<-cumsum(labels)					# true positives
    	fps<-(1:length(labels)) - tps		# false positives
    
    	tpr<-tps / tps[length(tps)]			# true positives rate
    	fpr<-fps / fps[length(fps)]			# false positives rate
    	prec<-tps/(1:length(values))		# number of true positives / (number of all positives = (true positives + false negatives))
    	recall<-tps/sum(labels)      		# number of true positives / (true positives + false negatives = all positive samples)
    
    	l<-list(name=v[i,2],short=v[i,1])
    
    	# precision/recall values
    	idx.dots<-cumsum(hist(recall,plot=F,breaks=15)$counts)
    	idx.lines<-cumsum(hist(recall,plot=F,breaks=300)$counts)
    	l<-c(l,prec.lines=list(prec[idx.lines]),recall.lines=list(recall[idx.lines]))
    	l<-c(l,prec.dots =list(prec[idx.dots]), recall.dots =list(recall[idx.dots]))
    
    	#
    	# true positive / false postive
    	#
    	idx.dots<-cumsum(hist(fpr,plot=F,breaks=25)$counts)
    	idx.lines<-c(1,cumsum(hist(fpr,plot=F,breaks=300)$counts))
    
    	# For AUROC scores we request a higher resolution 
    	idx.auroc<-c(1,cumsum(hist(fpr,plot=F,breaks=1000)$counts))
    
    	# calculate the AUROC. Note that diff() returns the difference of
    	# consecutive elements. We calculate the lower bound of the area.
    	auroc<-sum(c(diff(fpr[idx.lines]),0) * tpr[idx.lines])
    
    	l<-c(l,fpr.lines=list(fpr[idx.lines]), tpr.lines=list(tpr[idx.lines]))
    	l<-c(l,fpr.dots= list(fpr[idx.dots]),  tpr.dots= list(tpr[idx.dots]))
    	l<-c(l,auroc=auroc)
    	return(l)
	}

	res<-mclapply(1:nrow(v),eval.single,mc.cores=detectCores());
	return(res)
}

evaluate<-cmpfun(evaluate.def)

v<-matrix(c("marg","BN", T,
	"marg.ideal", "BN'", T,
	"marg.freq","FABN", T,
	"marg.freq.ideal", "FABN'", T,
	"resnik.avg", "Resnik",T,
	"resnik.avg.rank", "Resnik (rank)",F,
	"resnik.avg.p", "Resnik P",F,
	"resnik.avg.p.opt", "Resnik P*",F,
	"lin.avg", "Lin", T,
	"lin.avg.p", "Lin P", F,
	"jc.avg", "JC",T,
	"jc.avg.p", "JC P", F),ncol=3,byrow=T)

b4o.name.robj<-paste(b4o.base.name,"RObj",sep=".")
b4o.name.result.robj<-paste(b4o.base.name,"_result.RObj",sep="")

# only freq vs freq
if ((!file.exists(b4o.name.robj)) || (file.info(b4o.name.robj)$mtime < file.info(b4o.name)$mtime))
{
	message("Loading data");
	d<-b4o.load.data()

	d<-d[order(d$run),]

	resnik.avg.rank<-unlist(tapply(d$resnik.avg,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,resnik.avg.rank)
		
	marg.ideal.rank<-unlist(tapply(d$marg.ideal,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,marg.ideal.rank)
		
	marg.rank<-unlist(tapply(d$marg,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,marg.rank)
		
	marg.freq.rank<-unlist(tapply(d$marg.freq,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,marg.freq.rank)
		
	marg.freq.ideal.rank<-unlist(tapply(d$marg.freq.ideal,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,marg.freq.ideal.rank)
		
	resnik.avg.p.rank <- unlist(tapply(d$resnik.avg.p,d$run,function(x) {r<-rank(-x);return (max(r) - r + 1)})) 
	d<-cbind(d,resnik.avg.p.rank)
		
	f<-data.frame(p=d$resnik.avg.p,s=d$resnik.avg)
	resnik.avg.p.rank<-unsplit(lapply(split(f,d$run),function (x)
	{
		o<-order(x$p,-x$s)
		r<-1:length(o)
		r[o]<-1:length(o)
		return (r)
	}),d$run)
	d<-cbind(d,resnik.avg.p.rank)

	save(d,file=b4o.name.robj);
	message("Data loaded, preprocessed, and stored");
} else
{
	load(b4o.name.robj)
	message("Data loaded from storage")
}

# Calculate avg ranks
d.label.idx<-which(d$label==1)

message("Evaluating")

res.list<-evaluate(d,v)
save(res.list,file=b4o.name.result.robj,compress=T)

# values for the table (freq)
freq.important<-which(d$marg.freq > 0.5)
freq.positives<-length(freq.important)
freq.tp<-sum(d$label[freq.important])
print(sprintf("tp=%d tp+fp=%d ppv=%g",freq.tp,freq.positives,freq.tp/freq.positives))

freq.ideal.important<-which(d$marg.freq.ideal > 0.5)
freq.ideal.positives<-length(freq.ideal.important)
freq.ideal.tp<-sum(d$label[freq.ideal.important])
print(sprintf("tp=%d tp+fp=%d ppv=%g",freq.ideal.tp,freq.ideal.positives,freq.ideal.tp/freq.ideal.positives))

avg.p.important<-which(d$resnik.avg.p<0.05/2368)
avg.p.positives<-length(avg.p.important)
avg.p.tp<-sum(d$label[avg.p.important])
print(sprintf("tp=%d tp+fp=%d ppv=%g",avg.p.tp,avg.p.positives,avg.p.tp/avg.p.positives))

#col<-c("red","blue","cyan","green","gray","orange","magenta", "black")
col<-rainbow(length(res.list))

pdf(paste(b4o.base.name,"-precall.pdf",sep=""))

plot.new()
plot.window(xlim=c(0,1),ylim=c(0,1),xlab="Precision")
axis(1)
axis(2)
box()
for (i in 1:length(res.list))
{
	lines(res.list[[i]]$recall.lines,res.list[[i]]$prec.lines,type="l",col=col[i])
	points(res.list[[i]]$recall.dots,res.list[[i]]$prec.dots,pch=i,col=col[i])
}

legend(x="bottomleft",as.character(lapply(res.list,function(x) x$name)),col=col,lty=1,pch=1:length(res.list),cex=0.9)

dev.off()
