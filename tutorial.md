--
layout: tutorial_hands_on

title: GO Semantic Similarity Analysis
level: Introductory
zenodo_link: 'https://zenodo.org/record/3339362#.XTBT5_x7nCI'

questions:
- How can I compare gene products at the functional level?
- How can I obtain the inputs necessary for semantic similarity analysis?

objectives:
- How to perform a GO Semantic Similarity Analysis using three different modes
- How to interpret and simplify the results

time_estimation: '30 min'
key_points:
- "The GOSemanticSimilarity tool can be used to perform GO Semantic Similarity analysis" 
- "We can use Semantic Similarity analysis to cluster genes according to their molecular function, biological processes and cellular components."

contributors:
- Daniel Faria
- Beatriz Lima
---

# Introduction
{:.no_toc}

Semantic similarity is a method to compare genes or gene products at the functional level. 
We are able to quantify the similarity between those genes thanks to the semantic similarity measures, functions that returns an numerical score of similarity between two genes.

This tool is able to assess the closeness between two terms by making use of Gene Ontology annotations. Thus, if two gene products are annotated within the same schema, we can compare them by comparing the terms with which they are annotated. 

###  What is the Gene Ontology?  
The [Gene Ontology](http://www.geneontology.org/) (GO) is a structured, controlled vocabulary for the classification of gene function at the molecular and cellular level. It is divided in three separate sub-ontologies or GO types: biological process (e.g., signal transduction), molecular function (e.g., ATPase activity) and cellular component (e.g., ribosome). These sub-ontologies are structured as directed acyclic graphs (a hierarchy with multi-parenting) of GO terms.

![hexose-biosynthetic-process](https://user-images.githubusercontent.com/43668147/61297834-fbbec680-a7d4-11e9-8a22-cda55c686fa5.png)

**Figure 1** - Gene Ontology representation. Source: http://geneontology.org/docs/ontology-documentation/

### What are GO annotations?
Genes are associated to GO terms via GO annotations. Thus, each GO Term refers to a function or characteristic of the gene. Each gene can have multiple annotations, even of the same GO type. 

An important notion to take into account when using GO is that, according to the **true path rule**, a gene annotated to a term is also implicitly annotated to each ancestor of that term in the GO graph.

**How to get an annotation file for my study set?** 
GO annotations (by species) can be obtained from the [Gene Ontology website](http://geneontology.org/page/download-go-annotations), or from species-specific databases. One useful resource to obtain GO annotations is [Ensembl biomart](http://www.ensembl.org/biomart/martview). 

> ### {% icon comment %} Comments
> Take note to when and from where you obtained your annotations. For example, if you obtained your data from Ensembl, record the release you used.
{: .comment}

> ### Agenda
>
> In this tutorial we will deal with:
> {:toc}
>
{: .agenda}

# Semantic Similarity Analysis
To perform functional enrichment analysis, we need to have:
- A set of genes of interest (e.g., differentially expressed genes): **study set**
- **GO annotations**, associating the genes in the population set to GO terms
- The **GO ontology**, with the description of GO terms and their relationships, which you can find [here](http://geneontology.org/docs/download-ontology/)

We also need to decide:
- The semantic similarity measure to apply for similarity analysis.
- The analysis mode: comparing all vs all, specific pairs of genes or set vs set.

### Semantic similarity metrics
This tool implements most metrics that have been described in the [literature.](https://doi.org/10.1371/journal.pcbi.1000443)
The options available are:

![image](https://user-images.githubusercontent.com/43668147/61377275-cd0b2380-a89a-11e9-87ad-cf97fbf63d5e.png)
**Figure 2** - Metrics available for semantic similarity analysis.

All term based metrics use pairwise metric Best Match Average as a combinatory strategy to compare at the gene level.

___

For the first example we will perform an all vs all analysis. The study sets were obtained from RNA-seq of poly-A enriched total RNA of brain, heart and skeletal muscle samples from mouse (Source: [Expression Atlas](https://www.ebi.ac.uk/gxa/experiments/E-MTAB-3725/Downloads)).

## All vs all analysis
> ### {% icon hands_on %} Hands-on: All vs all analysis
>
> The data for this tutorial is available at [Zenodo](https://zenodo.org/record/3339362#.XTBT5_x7nCI) to download. For convenience and reproducibility of results, we already added the GO ontology and annotations in the Zenodo repository.
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
>   - {% icon param-select %} *"Semantic Similarity Metric"*: SimUI
>   - {% icon param-select %} *"GO type"*: Molecular function
>   - Use the default options for the rest. 
>
> # (Inserir screenshot....)
>
> 4. Press **Execute**
>
>    > ### {% icon question %} Questions
>    >
>    > What were the results from running GOSemanticSimilarity on mode *all vs all*?
>    > <details>
>    >
>    > <summary>Click to view answers</summary>
>    > This will generate 2 files with the respective default names: 'results' and 'HeatChart.png'. 'Results' is a tabular list of pairs of GO terms present in the study set and their respective semantic similarity score. The graph file is a visual representation of the tabular file.
>    > </details>
>    {: .question}
{: .hands_on}

## Specific pairs analysis
You may find it interesting to compare only specific gene pairs, instead of comparing all the genes in the list within themselves. 

> ### {% icon hands_on %} Hands-on: Specific pairs analysis
> 1. Come back to [Zenodo](https://zenodo.org/record/3339362#.XTBT5_x7nCI) and upload the file pairs_set.txt to Galaxy.
>
> 2. Take a look {% icon solution %} at this file. It is a list of pairs of genes, separated by a comma (or "\t", ";", " ").
> These genes are overexpressed in different tissues, as the following schema illustrates:
>   > | heart | heart |
>   > | heart | muscle |
>   > | heart | brain |
>   {: .matrix}
>
> 3. GOSemanticSimilarity {% icon tool %} tool with the following parameters:
>   - {% icon param-file %} *“Gene Ontology File”*: go.obo
>   - {% icon param-file %} *“Gene Product Annotation File”*: annot.txt
>   - {% icon param-select %} *"Compare"*: List of pairs of genes
>   - {% icon param-file %} *“Study set File”*: pairs_set.txt
>   - {% icon param-select %} *"Semantic Similarity Metric"*: SimUI
>   - {% icon param-select %} *"GO type"*: Molecular function
>   - Use the default options for the rest. 
>
> 4. Press **Execute**
>
> ### {% icon question %} Questions
>    >
>    > What can we infer from the results of this study set with GOSemanticSimilarity on *specific pairs* mode?
>    > <details>
>    >
>    > <summary>Click to view answers</summary>
>    > The functional similarity between overexpressed genes in the same tissue (heart) is the biggest. The similarity between overexpressed genes in the heart and skeletal muscle is bigger than the similarity between overexpressed genes in the heart and brain. This suggests that the heart is functionally more similar to the skeletal muscle than the brain.
>    > </details>
>    {: .question}
{: .hands_on}

## Set vs set analysis
To further discuss the similarity between the tissues, the *set vs set* mode is the most useful, in which we compare sets of overexpressed genes in the different tissues.

> ### {% icon hands_on %} Hands-on: Set vs set
> 1. Go to [Zenodo](https://zenodo.org/record/3339362#.XTBT5_x7nCI) and upload the files muscle.txt and heart.txt to Galaxy. As the brain.txt file, these are sets of overexpressed genes in the muscle and heart.
>
> 2. GOSemanticSimilarity {% icon tool %} tool with the following parameters:
>   - {% icon param-file %} *“Gene Ontology File”*: go.obo
>   - {% icon param-file %} *“Gene Product Annotation File”*: annot.txt
>   - {% icon param-select %} *"Compare"*: Sets
>   - {% icon param-file %} *“Study set File 1”*: muscle.txt
>   - {% icon param-file %} *“Study set File 2”*: heart.txt
>   - {% icon param-select %} *"Semantic Similarity Metric"*: SimUI
>   - {% icon param-select %} *"GO type"*: Molecular function
>   - Use the default options for the rest. 
>
> 3. Press **Execute**
>
> 4. Repeat steps 2. and 3., but this time comparing *muscle.txt - brain.txt* and *heart.txt - brain.txt*.
>
> 5. Open the **results.txt** files.
>
> ### {% icon question %} Questions
>    >
>    > What can we infer from the results of these sets' comparison with GOSemanticSimilarity?
>    > <details>
>    >
>    > <summary>Click to view answers</summary>
>    > The functional similarity between the muscle and the heart is the biggest, followed by the similarity between the heart and the brain. The muscle and brain are hardly similar.
>    > </details>
>    {: .question}
{: .hands_on}


# Interpretation of the results 
Semantic similarity analysis, and in particular, this tool, has many applications:
 - Clustering genes according to their molecular function, biological processes in which they are involved, and the cellular component in which they perform a function.
  - Find genes related to a given gene of interest, according to their functional clustering.
  - Correlate functional similarity with another feature of interest (e.g., sequence similarity, interactions, diseases).

# Conclusion
{:.no_toc}
GOSemanticSimilarity analysis makes use of Gene Ontology annotations to compare genes or gene products at the functional level. 
The Gene Ontology is a trustworthy knowlegdebase with information on the molecular function, cellular component and biological processes of genes. This tool offers an easy to use platform and visual results for better understanding of your transcriptomics or proteomics output data.


