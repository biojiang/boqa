#
# Primitive makefile
#
B4OBENCHMARK=java -Xmx2G -cp bin:jars/commons-cli-1.2.jar sonumina.b4o.B4OBenchmark

ifeq ($(ORPHANET),yes)
ANNOTATIONSFILE=data/phenotype_annotation.omim.orphanet.gz
else
ANNOTATIONSFILE=data/phenotype_annotation.omim.gz
endif

.PHONY: all
all: evaluate

.PHONY: evaluate
evaluate: benchmark evaluate-only 

.PHONY: evaluate-only
evaluate-only:
	echo 'source("benchmark_load.R");source("src/sonumina/b4o/eval.R")' | R --vanilla

.PHONY: benchmark
benchmark: build
	$(B4OBENCHMARK) -r benchmark -o data/human-phenotype-ontology.obo.gz -a $(ANNOTATIONSFILE) -c -m 6 --sizeOfScoreDistribution 250000

.PHONY: build
build: 
	ant clean
	ant build
