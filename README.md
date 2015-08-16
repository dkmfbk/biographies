Wikipedia is the largest collection of encyclopedic data ever written in the history of humanity. Thanks to its coverage and its availability in machine-readable format, it has become a primary resource for large-scale research in historical and cultural studies. In this work, we focus on the subset of pages describing persons, and we investigate the task of recognizing biographical sections from them: given a person’s page, we identify the list of sections where information about her/his life is present. We model this as a sequence classification problem, and propose a supervised setting, in which the training data are acquired automatically. Besides, we show that six simple features extracted only from the section titles are very informative and yield good results well above a strong baseline.

In this GitHub project, you'll find the material and source code for running our experiments.

* [Annotated material](https://github.com/dkmfbk/biographies/tree/master/material) such as training/dev/test sets of pages, training data for [CRFsuite](www.chokkan.org/software/crfsuite/), the results of the classification and the annotation agreement.

To compile the source code of the project, just clone it with `git` and run `mvn package` from the shell.

This software is released under the GPLv3 license.
