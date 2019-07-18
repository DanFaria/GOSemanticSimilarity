--
layout: tutorial_hands_on

title: GO Semantic Similarity Analysis
level: Introductory
zenodo_link: ''

questions:
- How can I compare gene products at the functional level?
- How can I obtain the inputs necessary for semantic similarity analysis?

objectives:
- How to perform a GO Semantic Similarity Analysis using three different modes
- How to interpret and simplify the results

time_estimation: ''
key_points:
- The take-home messages
- They will appear at the end of the tutorial

contributors:
- Daniel Faria
- Beatriz Lima
---

## Introduction
{:.no_toc}

Semantic similarity is a method to compare genes or gene products at the functional level. 
We are able to quantify the similarity between those genes thanks to the semantic similarity measures, a function that returns an numerical score of similarity between two genes.

This tool is able to assess the closeness between two terms by making use of Gene Ontology annotations. Thus, if two gene products are annotated within the same schema, we can compare them by comparing the terms with which they are annotated. It is ideal for scientists that want to compare their transcriptomics or proteomics outputs and see if there’s any functional clusters. 

####  What is the Gene Ontology?  
The [Gene Ontology](http://www.geneontology.org/) (GO) is a structured, controlled vocabulary for the classification of gene function at the molecular and cellular level. It is divided in three separate sub-ontologies or GO types: biological process (e.g., signal transduction), molecular function (e.g., ATPase activity) and cellular component (e.g., ribosome). These sub-ontologies are structured as directed acyclic graphs (a hierarchy with multi-parenting) of GO terms.

![hexose-biosynthetic-process](https://user-images.githubusercontent.com/43668147/61297834-fbbec680-a7d4-11e9-8a22-cda55c686fa5.png)
**Figure 1** - Gene Ontology representation. Source: http://geneontology.org/docs/ontology-documentation/


> ### Agenda
>
> In this tutorial we will deal with:
> {:toc}
>
{: .agenda}

## Semantic Similarity Analysis
To perform functional enrichment analysis, we need to have:
- A set of genes of interest (e.g., differentially expressed genes): **study set**
- **GO annotations**, associating the genes in the population set to GO terms
- The **GO ontology**, with the description of GO terms and their relationships

We also need to decide:
- The semantic similarity measure to apply for similarity analysis.
- The analysis mode: comparing all vs all, specific pairs of genes or set vs set.

#### Semantic similarity metrics
This tool implements most metrics that have been described in the [literature.](https://doi.org/10.1371/journal.pcbi.1000443)
The options available are:
![image](https://user-images.githubusercontent.com/43668147/61377275-cd0b2380-a89a-11e9-87ad-cf97fbf63d5e.png)
All term based metrics use pairwise metric Best Match Average as a combinatory strategy to compare at the gene level.

For the first example we will perform an all vs all analysis. The study sets were obtained from RNA-seq of poly-A enriched total RNA of brain, heart and skeletal muscle samples from mouse (Source: [Expression Atlas](https://www.ebi.ac.uk/gxa/experiments/E-MTAB-3725/Downloads)).

> ### {% icon hands_on %} Hands-on: All vs all analysis
>
> The data for this tutorial is available at [Zenodo] # (link!!!!!!!!!!!) to download. For convenience and reproducibility of results, we already added the GO ontology and annotations in the Zenodo repository.
>
> 1. **Upload to the Galaxy the following files**:
> - go.obo
> - annot.txt
> - brain.txt
>
>    > ### {% icon tip %} Tip: Importing data via links
>    >* **Click** on the upload button in the upper left of the interface.
>    >
>    >![image](https://user-images.githubusercontent.com/43668147/61382012-d5b42780-a8a3-11e9-8917-52116c124885.png)
>    >
>    > * Press **Choose local file** and search for your file.
>    > * Press **Start** and wait for the upload to finish
>    {: .tip}
>
> 2. If you press the {% icon solution %} icon of brain.txt you should see a list of overexpressed gene ids in the brain. The annot.txt file is just a tabular file associating the genes in the population set to GO terms.
>
> 3. GOSemanticSimilarity {% icon tool %} tool with the following parameters:
>   - {% icon param-file %} *“Gene Ontology File”*: go.obo
>   - {% icon param-file %} *“Gene Product Annotation File”*: annot.txt
>   - {% icon param-select %} *"Compare"*: List of genes
>   - {% icon param-file %} *“Study set File”*: brain.txt
>   - {% icon param-select %} *"GO type"*: Molecular function
>   - Use the default options for the rest. 
>
> #(Inserir screenshot....)




## Interpretation of the results 
Semantic similarity analysis, and in particular, this tool, has many applications as clustering genes according to their molecular function, biological processes in which they are involved, and the cellular component in which they perform a function.

It is also useful to find genes related to a given gene of interest, according to their functional clustering.

Moreover, it allows us to correlate functional similarity with another feature of interest (e.g., sequence similarity, interactions, diseases).



