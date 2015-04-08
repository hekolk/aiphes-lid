# Language identification

This project contains code for a language identification/document classification task as described in the e-mail.

It is self-contained, including training data and evaluation scripts.

For calling instructions and sample output, see ``sampleOutput.md`` in this folder.


## Data

In order to test and evaluate the implemented code, I chose to use an established data set. 
Data from the Discriminating between similar languages (DSL) shared task the from VarDial Workshop @ COLING 2014 is used.
  
Details on the task and the data can be found at the [website](http://corporavm.uni-koeln.de/vardial/sharedtask.html) or the [Bitbucket repo](https://bitbucket.org/alvations/dslsharedtask2014).

For an overview paper of the evaluation, see [1]. 

The data ``DSLCC.zip`` and ``DSLCC-eval.zip`` is already included in the ``data`` subdirectory of this project.

## Implemented Methods


Without going into the details of the published approaches, I therefore implemented a typical Naive Bayes classifier with Laplace smoothing.

## Comparison with published results

Looking at the results on the [workshop website](http://htmlpreview.github.io/?https://bitbucket.org/alvations/dslsharedtask2014/downloads/dsl-results.html), the implemented
approach (which is really quite straight-forward) would theoretically rank third in the eight submissions.


Looking at the overview paper of the task [1] and individual submissions, a basic yet relatively successful approach can be found in [2] and corresponds to this implementation.
From this, it is evident, that as a simple baseline approach, a Naive Bayes classifier (c.f., Table 3) based on word unigram probabilities (c.f., Table 2) easily reaps the "low-hanging fruits".

## References

1.  Zampieri, M., Tan, L., Ljubešic, N., & Tiedemann, J. (2014). A report on the DSL shared task 2014. COLING 2014, 58.
    
    [link to paper](http://www.aclweb.org/anthology/W/W14/W14-5307.pdf)
    
2.  King, B., Radev, D., & Abney, S. (2014). Experiments in sentence language identification with groups of similar languages. COLING 2014, 473, 146.

    [link to paper](http://www.aclweb.org/anthology/W/W14/W14-53.pdf#page=156)