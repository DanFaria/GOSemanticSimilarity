# GOSemanticSimilarity

GOSemanticSimilarity is a tool for performing semantic similarity analysis, based on Gene Ontology annotations, of a set of gene products or two sets of gene products.

#### It requires as input:

    1. A Gene Ontology file in either OBO or OWL, and either the full GO or a GOSlim

    2. An Annotation file, which can be in GAF format (from the Gene Ontology website), BLAST2GO format, or in tabular format (with gene ids in the first column and GO term ids in the second one)

    3. Either a Study Set file listing the gene ids in the study (one gene product per line) or pairs of genes [NOTE: the gene ids in the Study Set file must match the gene ids in the Annotation file].
    
    4. The analysis mode: comparing all vs all, specific pairs of genes or set vs set.

    5. The semantic similarity measure to apply for similarity analysis.

    6. Optionally, the GO type (sub-ontology) to use, which can be Molecular Function, Biological Process or Cellular Component.

#### It produces as output, depending on the mode:

    - All vs all: A tabular Result file listing the semantic similarity between all of the genes and a graph PNG heat map.
    
    - Specific pairs: A tabular Result file listing the semantic similarity between the specific pairs of genes and a graph PNG heat map.
    
    - Set vs set: A Result File containing the similarity score obtained for the two sets and a list of relevant common GO terms. 

The XML file is setup for the Galaxy platform

#### Command Line Usage

To run the GSS.jar file from the command line, you need to have Java installed in your computer. You can run it by typing:

"java -jar GSS.jar [OPTIONS]"

The options are:

"-g,--go FILE_PATH" => Path to the Gene Ontology OBO or OWL file [Mandatory]

"-a,--annotation FILE_PATH" => Path to the tabular annotation file in GAF, BLAST2GO or 2-column table format [Mandatory]

"-s1,--study FILE_PATH" => Path to the file listing the first study set gene products [Mandatory]

"-s2,--study FILE_PATH" => Path to the file listing the second study set gene products [Optional]

"-t, --type" => Sub-Ontology; Options: 'molecular_function'/'biological_process'/'cellular_component'(Default: all)

"-st --structural" => Compute structural IC [Optional] (Default: FALSE)

"-r,--use_all_relations" => Infer annotations through 'part_of' and other non-hierarchical relations [Optional] (Default: FALSE)

"-lp, --list_of_pairs" => If the mode chosen is 'specific pairs' [Mandatory]

"-set, --set" => If the mode chosen is 'set vs set' [Mandatory]

"-res, --result FILE_PATH" => Path to the output similarity result file [Optional] (Default: "results.txt")

"-gsm, --gene_metric" => Metric used to analyse semantic similarity between genes or gene products [Mandatory]

"-tsm, --term_metric" => Metric used to analyse semantic similarity between ontology terms [Mandatory when using Best Match Average gene metric only]

"-h,--help" => Display command line usage instructions
