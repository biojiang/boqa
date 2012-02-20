library(compiler)

#' Evaluates a given data frame for classification performance
#' 
#' @param d represents a frame, from which date is gathered
#' @param v represents a column matrix, in which the name of the slots 
#'        that are used for the plots of the data frame can be specified. The last
#'        column represents whether high values are good or not.
evaluate.def<-function(d,v)
{
	if (nrow(d)==0)
	{
		return()
	}
	
	res<-list();
	
	colnames(v)<-c("short","full","high.is.good")

	for (i in (1:nrow(v)))
	{
		# get the type
		type<-v[i,1]

		# get primary and optional secondary values
		if (type == "resnick.avg.p.opt")
		{
			primary.values<-d$resnick.avg.p
			secondary.values<-1-d$label
		} else
		{
		    primary.values<-d[,type]
		    
		    if (type == "resnick.avg.p")
		    {
		    	secondary.values<- -d$resnick.avg
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
		
		res<-c(res,list(l));
	}
	return(res)
}

evaluate<-cmpfun(evaluate.def)



c<-data.frame(a=c(1,1,1,2,2),b=c(1,1,2,2,2))

#
# 
#
rank2.def<-function(c)
{
	t<-c(rowSums(c[-nrow(c),]==c[-1,])!=ncol(c),T)
	rank<-numeric(nrow(c))
	r<-1
	for (i in 1:length(t))
	{
		if (t[i] == T)
		{
			diff<- i - r + 1
			val <- r + (diff - 1) / 2
			rank[r:i]<-val
			print(paste(r,i,val))
			r <- i + 1;
		}
	}
}

v<-matrix(c("marg","Marg. Prob.", T,
      "marg.ideal", "Marg. Prob. (Ideal)", T,
		  "marg.freq","Marg. Prob. (Freq)", T,
		  "marg.freq.ideal", "Marg. Prob. (Freq,Ideal)", T,
      "resnick.avg", "Resnik",T,
			"resnick.avg.rank", "Resnik (rank)",F,
			"resnick.avg.p", "Resnik P",F,
			"resnick.avg.p.opt", "Resnik P*",F),ncol=3,byrow=T)

b4o.name.robj<-paste(b4o.name,"RObj",sep=".")
b4o.name.result.robj<-paste(b4o.name,"_result.RObj",sep="")

# only freq vs freq
if ((!file.exists(b4o.name.robj)) || (file.info(b4o.name.robj)$mtime < file.info(b4o.name)$mtime))
{
	d<-b4o.load.data()

	d<-d[order(d$run),]

	resnick.avg.rank<-unlist(tapply(d$resnick.avg,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,resnick.avg.rank)
		
	marg.ideal.rank<-unlist(tapply(d$marg.ideal,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,marg.ideal.rank)
		
	marg.rank<-unlist(tapply(d$marg,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,marg.rank)
		
	marg.freq.rank<-unlist(tapply(d$marg.freq,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,marg.freq.rank)
		
	marg.freq.ideal.rank<-unlist(tapply(d$marg.freq.ideal,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,marg.freq.ideal.rank)
		
	resnick.avg.p.rank <- unlist(tapply(d$resnick.avg.p,d$run,function(x) {r<-rank(-x);return (max(r) - r + 1)})) 
	d<-cbind(d,resnick.avg.p.rank)
		
	f<-data.frame(p=d$resnick.avg.p,s=d$resnick.avg)
	resnick.avg.p.rank<-unsplit(lapply(split(f,d$run),function (x)
	{
		o<-order(x$p,-x$s)
		r<-1:length(o)
		r[o]<-1:length(o)
		return (r)
	}),d$run)
	d<-cbind(d,resnick.avg.p.rank)

	save(d,file=b4o.name.robj);
	message("Data loaded, preprocessed, and stored");
} else
{
	load("d-freq-only.RObj")
	message("Data loaded from storage")
}

# Calculate avg ranks
d.label.idx<-which(d$label==1)

res.list<-evaluate(d,v)
res.list.freq.vs.freq<-res.list
save(res.list.freq.vs.freq,file=b4o.name.result.robj,compress=T)

# values for the table (freq)
freq.important<-which(d$marg.freq > 0.5)
freq.positives<-length(freq.important)
freq.tp<-sum(d$label[freq.important])
print(sprintf("tp=%d tp+fp=%d ppv=%g",freq.tp,freq.positives,freq.tp/freq.positives))

freq.ideal.important<-which(d$marg.freq.ideal > 0.5)
freq.ideal.positives<-length(freq.ideal.important)
freq.ideal.tp<-sum(d$label[freq.ideal.important])
print(sprintf("tp=%d tp+fp=%d ppv=%g",freq.ideal.tp,freq.ideal.positives,freq.ideal.tp/freq.ideal.positives))

avg.p.important<-which(d$resnick.avg.p<0.05/2368)
avg.p.positives<-length(avg.p.important)
avg.p.tp<-sum(d$label[avg.p.important])
print(sprintf("tp=%d tp+fp=%d ppv=%g",avg.p.tp,avg.p.positives,avg.p.tp/avg.p.positives))

col<-c("red","blue","cyan","green","gray","orange","magenta", "black")

pdf("b4o-precall.pdf")

plot(ylim=c(0,1),res.list[[1]]$recall.lines,res.list[[1]]$prec.lines,type="l",col=col[1],xlab="Recall",ylab="Precision")
lines(res.list[[2]]$recall.lines,res.list[[2]]$prec.lines,type="l",col=col[2])
lines(res.list[[3]]$recall.lines,res.list[[3]]$prec.lines,type="l",col=col[3])
lines(res.list[[4]]$recall.lines,res.list[[4]]$prec.lines,type="l",col=col[4])
lines(res.list[[5]]$recall.lines,res.list[[5]]$prec.lines,type="l",col=col[5])
lines(res.list[[6]]$recall.lines,res.list[[6]]$prec.lines,type="l",col=col[6])
lines(res.list[[7]]$recall.lines,res.list[[7]]$prec.lines,type="l",col=col[7])
lines(res.list[[8]]$recall.lines,res.list[[8]]$prec.lines,type="l",col=col[8])

legend(x="topright",as.character(lapply(res.list,function(x) x$name)),col=col,lty=1)

dev.off()
