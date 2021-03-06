# Improved citation context detection methods
### A master thesis project in Natural Langage Processing and Machine Learning. 
---
## 1. Intro
This repository contains the code used in my master thesis about citation context detection. To get an understanding of the area and the specific task of this project, one should read the thesis. Simply put the task is: "given a research paper and one of its references, find all sentences in the paper that implicitly cites this reference." 

* **explicit citation**: A citation to another paper found in running text, that uses some well-defined format.
* **implicit citation**: A citation that does not use such a format.

As an example, the text snippet below consists of three sentences, of which the first one is not a citation, the second one is an explicit citation, and the last one is an implicit citation.

*"The purpose of this project is to develop efficient sorting algorithms. Johnson et. al (1987) proposed the bubble sort algorithm. It proved a failure."*

Two distinct methods are used for solving the classification problem: a Machine Learning classifier (Awais and Athar), and an iterative algorithm based on a graphical model (Qazvinian and Radev).

## 2. Code structure
The code is written in Java 8. The repository consists of the following packages:
* **main** - contains main classes that correspond to specific tasks of the thesis, such as initializing a dataset, or running a classifier.
* **dataset** - contains classes that are concerned with the data used as input to the algorithms. 
* **machineLearning** - contains classes used for the machine learning approach (using the weka library).
* **graphical** - contains the graphical belief propagation algorithm.
* **semanticSim** - contains code for semantic similarity text measures, that are used to extend the classification algorithms. Some of the code ended up not being used in the thesis.
* **util** - contains general classes that don't fit in any other package, as well as classes for representing classification results.

## 3. Getting started
To run the algorithms, one first needs the data. I used the dataset annotated by Awais Athar available at http://www.cl.cam.ac.uk/~aa496/citation-context-corpus/. The first main classes to run are the ones that generate different datasets in XML-format. Once one has a basic XML-dataset one can extend it with other text representations. The classes for running the actual algorithms (Machine Learning classifier, and iterative algorithm) are also found in the main-package. The algorithms and all dataset-classes are generic in the way that they accept different text representations. Specifying which representation should be used is done in the main methods. 

The project has several dependencies to other libraries, depending on which parts are run.
* jsoup 1.8.1
* stanford-corenlp 3.5.1
* jwi 2.3.3
* ws4j 1.0.1
* Apache Commons Lang 3.3.2
* Weka 3.7.12
* GNU Trove 3.1a1
* pdfbox-app 1.8.8
* S-Space Package 2.0.4

All code was run on a laptop with 64-bit Linux (Ubuntu 14.04) and 8GB RAM.
