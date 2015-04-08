# Sample Output

## Model building and classification

Sample output of running the main class on the data (e.g., via the Gradle build script)

```
$ ./gradlew execute
:compileJava UP-TO-DATE
:processResources UP-TO-DATE
:classes UP-TO-DATE
:execute
Apr 08, 2015 11:00:54 PM st.kolkhor.projects.languageid.NaiveBayesClassifier$NBModelBuilder buildModel
INFORMATION: building model for 13 languages and vocabulary of size 607832
Apr 08, 2015 11:00:54 PM st.kolkhor.projects.languageid.NaiveBayesClassifier$NBModelBuilder buildModel
INFORMATION: using 9486016 from 234000 documents
Apr 08, 2015 11:00:58 PM st.kolkhor.projects.languageid.LanguageIdentifier main
INFORMATION: learned models for the following languages: [bs, pt-PT, en-US, es-AR, pt-BR, cz, sk, es-ES, hr, id, my, en-GB, sr]
Apr 08, 2015 11:01:01 PM st.kolkhor.projects.languageid.LanguageIdentifier main
INFORMATION: learning and classification took 18.077 seconds
Apr 08, 2015 11:01:01 PM st.kolkhor.projects.languageid.LanguageIdentifier main
INFORMATION: the classified test data can be found in /media/data2/repos/personal/language_id/data/eval/2015-04-08_23-00-aiphes-close-run1.txt
Apr 08, 2015 11:01:01 PM st.kolkhor.projects.languageid.LanguageIdentifier main
INFORMATION: call 'cd data/eval; python2 dslevalscript.py 2015-04-08_23-00-aiphes-close-run1.txt.zip' from the project dir for scoring results

BUILD SUCCESSFUL

Total time: 20.77 secs
```

## Result scoring

Executing the eval script (see output above for corresponding command) yields

```
$ cd data/eval; python2 dslevalscript.py 2015-04-08_23-00-aiphes-close-run1.txt.zip
2015-04-08_23-00-aiphes-close-run1.txt
Overall Accuracy: 0.9435
Macro-avg Fscore: 0.9432

Best Overall Accuracy:	0.9435	from	2015-04-08_23-00-aiphes-close-run1.txt
Best Macro-average F-score:	0.9432	from	2015-04-08_23-00-aiphes-close-run1.txt

Best per-group Accuracy:
Group	A:	0.926666666667	from	2015-04-08_23-00-aiphes-close-run1.txt
Group	B:	0.9955	from	2015-04-08_23-00-aiphes-close-run1.txt
Group	C:	1.0	from	2015-04-08_23-00-aiphes-close-run1.txt
Group	D:	0.945	from	2015-04-08_23-00-aiphes-close-run1.txt
Group	E:	0.8585	from	2015-04-08_23-00-aiphes-close-run1.txt
```