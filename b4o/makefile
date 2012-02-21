BENCHMARK=java -Xmx2G -cp bin:jars/commons-cli-1.2.jar sonumina.b4o.B4O

.PHONY: all
all:
	ant clean
	ant build
	$(BENCHMARK) -o data/human-phenotype-ontology.obo.gz -a data/phenotype_annotation.omim.gz -c -m 6 --sizeOfScoreDistribution 50000
