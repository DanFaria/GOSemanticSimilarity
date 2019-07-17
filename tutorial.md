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
Semantic similarity is a method to compare genes or gene products at the functional level. 
We are able to quantify the similarity between those genes thanks to the semantic similarity measures, a function that returns an numerical score of similarity between two genes. These metrics have already been described in the literature.

This tool is able to assess the closeness between two terms by making use of Gene Ontology annotations. Thus, if two gene products are annotated within the same schema, we can compare them by comparing the terms with which they are annotated.

####  What is the Gene Ontology?  
The [Gene Ontology](http://www.geneontology.org/) (GO) is a structured, controlled vocabulary for the classification of gene function at the molecular and cellular level. It is divided in three separate sub-ontologies or GO types: biological process (e.g., signal transduction), molecular function (e.g., ATPase activity) and cellular component (e.g., ribosome). These sub-ontologies are structured as directed acyclic graphs (a hierarchy with multi-parenting) of GO terms.

![hexose-biosynthetic-process](https://user-images.githubusercontent.com/43668147/61297834-fbbec680-a7d4-11e9-8a22-cda55c686fa5.png "Figure 1 - Gene Ontology representation. Source: http://geneontology.org/docs/ontology-documentation/")

## Semantic Similarity Analysis
To perform functional enrichment analysis, we need to have:
-   A set of genes of interest (e.g., differentially expressed genes): **study set**
-   **GO annotations**, associating the genes in the population set to GO terms
-   The **GO ontology**, with the description of GO terms and their relationships




