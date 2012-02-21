#
# Primitive makefile
#
BENCHMARK=java -Xmx2G -cp bin:jars/commons-cli-1.2.jar sonumina.b4o.B4O

.PHONY: all
all: evaluate

.PHONY: evaluate
evaluate: benchmark evaluate-only 

.PHONY: evaluate-only
evaluate-only:
	echo 'source("benchmark_load.R");source("src/sonumina/b4o/eval.R")' | R --vanilla

.PHONY: benchmark
benchmark: build
	$(BENCHMARK) -o data/human-phenotype-ontology.obo.gz -a data/phenotype_annotation.omim.gz -c -m 6 --sizeOfScoreDistribution 50000

.PHONY: build
build: 
	ant clean
	ant build
