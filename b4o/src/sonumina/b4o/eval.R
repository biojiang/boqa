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

if (file.exists("fnd.txt"))
{
	if ((!file.exists("d.RObj")) || (file.info("d.RObj")$mtime < file.info("fnd.txt")$mtime))
	{
		d<-read.table("fnd.txt",h=F,stringsAsFactors=F,colClasses=c("integer","integer","numeric","numeric","numeric","numeric","numeric","numeric","numeric","numeric","numeric"))
		colnames(d)<-c("run","label","score","marg","marg.ideal", "score.freq","marg.freq", "marg.freq.ideal", "resnick.avg", "resnick.avg.p","freq")
		save(d,file="d.RObj");
	} else
	{
		load("d.RObj")
	}
	message("Data read")
	d<-d[order(d$run),]

    # As below
	resnick.avg.rank<-unlist(tapply(d$resnick.avg,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,resnick.avg.rank)

	marg.ideal.rank<-unlist(tapply(d$marg.ideal,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,marg.ideal.rank)

	marg.rank<-unlist(tapply(d$marg,d$run,function(x) {r<-rank(x);return (max(r) - r + 1)}))
	d<-cbind(d,marg.rank)


#	resnick.avg.p.rank<-unlist(tapply(d$resnick.avg.p, d$run,function(x) {r<-rank(x);return (max(r) - r + 1)})) 
	f<-data.frame(p=d$resnick.avg.p,s=d$resnick.avg)
	resnick.avg.p.rank<-unsplit(lapply(split(f,d$run),function (x)
	{
		o<-order(x$p,-x$s)						# determine the order
#		xs<-x[o,]								# sort
		r[o]<-1:length(o)
#		r<-1:length(o)
#		r[o]<-ave(1:length(o),xs$p,xs$s)		# for each rank, the mean of the factor group, filled at the correct order
		return (r)
	}),d$run)
	d<-cbind(d,resnick.avg.p.rank)

	message("Data prepared")
	
	# all vs all
	res.list<-evaluate(d,v)
	res.list.complete<-res.list
	save(res.list.complete,file="b4o_res.list.complete.RObj")
	message("all vs. all done");
	
	# only freq vs all. the freq column is 1 if the item in question has frequencies
	d2<-subset(d,d$freq==T)
	res.list.freq.vs.all<-evaluate(d2,v)
	save(res.list.freq.vs.all,file="b4o_res.list.freq.vs.all.RObj")
} else
{
	# only freq vs freq
	if ((!file.exists("d-freq-only.RObj")) || (file.info("d-freq-only.RObj")$mtime < file.info("fnd-freq-only.txt")$mtime))
	{
		d<-read.table("fnd-freq-only.txt",h=F,stringsAsFactors=F)
		colnames(d)<-c("run","label","score","marg","marg.ideal", "score.freq","marg.freq", "marg.freq.ideal", "resnick.avg", "resnick.avg.p","freq")
		save(d,file="d-freq-only.RObj");
	} else
	{
		load("d-freq-only.RObj")
	}
d<-d[d$run==1,]
	
	message("Data read")
	d<-d[order(d$run),]

    # As above
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

	# Calculate avg ranks
	d.label.idx<-which(d$label==1)

	res.list<-evaluate(d,v)
	res.list.freq.vs.freq<-res.list
	save(res.list.freq.vs.freq,file="b4o_res.list.freq.vs.freq.RObj")
}

# ssss

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
