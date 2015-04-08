#!/usr/bin/env python -*- coding: utf-8 -*-

import codecs, os, re, zipfile, sys
import operator
from collections import defaultdict, Counter

# Gold files.
GOLD_A2E = 'test-gold.txt' 
GOLD_F = 'test-eng-gold.txt'

# Classes.
CLASSES_A2E = ['a_bs', 'a_hr','a_sr',
               'b_id', 'b_my',
               'c_cz', 'c_sk',
               'd_pt-br', 'd_pt-pt',
               'e_es-ar', 'e_es-es']

CLASSES_F = ['f_en-gb','f_en-us']
  

def is_eng_submission(filename):
  """ Check whether submission is for Group F. """
  return True if "-eng-run" in filename else False

def generate_confusion_matrix(submission_zipfile):
  """ . """
  submissionfile_confusionmatrix = {}
  with zipfile.ZipFile(submission_zipfile, 'r') as inzipfile:
    for infile in inzipfile.namelist():
      # Skip description files.
      if not re.match(r'.*run[1-3]', infile):
        continue
      # Reading the correct gold data file for evaluation.
      goldfile = GOLD_F if is_eng_submission(infile) else GOLD_A2E
      gold_classes = CLASSES_F if is_eng_submission(infile) else CLASSES_A2E
      gold_data = ["_".join(i.strip().lower().split("\t")[1:3]) \
                   for i in open(goldfile,"r'")]
      ##print infile, goldfile
      
      # Reading system output.
      system_output = ["_".join(i.strip().lower().split("\t")[1:3]) \
                      for i in inzipfile.read(infile).split('\n')]
      
      # Computing the confusion matrix
      confusion_matrix = defaultdict(Counter)
      for lang in gold_classes:
        for ans, gold in zip(system_output, gold_data):
          if lang != gold: continue
          confusion_matrix[gold][ans]+=1
      
      filename = infile.rpartition('/')[2]
      submissionfile_confusionmatrix[filename] = confusion_matrix
  return submissionfile_confusionmatrix

def try_except(myFunction, *params):
  """ Generic try-except to catch ZeroDivisionError. """
  try:
    return myFunction(*params)
  except ZeroDivisionError as e:
    return 0
  
class Score():
  def __init__(self, true_pos, false_neg, false_pos):
    
    cal_prec = lambda tp, fp : tp / float(tp + fp)
    cal_rec = lambda tp, fn : tp / float(tp + fn)
    cal_fscore = lambda prec,rec: 2*prec*rec / float(prec+rec)
    
    self.precision = try_except(cal_prec, true_pos, false_pos)
    self.recall = try_except(cal_prec, true_pos, false_neg)
    self.fscore = try_except(cal_fscore, self.precision,self.recall)

def compute_overall_scores(confusionmatrix, submissionfile):  
  # Computing TP, FN and FP.
  lang_tpfnfp = defaultdict(Counter)
  overall_tpfnfp = Counter()
  for lang in confusionmatrix:
    true_positives = confusionmatrix[lang][lang]
    false_negatives = sum(confusionmatrix[lang].values()) - true_positives
    false_positives = sum([confusionmatrix[i][lang] for i in confusionmatrix \
                           if i != lang])
    lang_tpfnfp[lang]['tp'] = true_positives
    lang_tpfnfp[lang]['fn'] = false_negatives
    lang_tpfnfp[lang]['fp'] = false_positives
    overall_tpfnfp['tp'] += true_positives
    overall_tpfnfp['fn'] += false_negatives
    overall_tpfnfp['fp'] += false_positives
  
  # Calculating Overall Accuracy.
  numsent = 2*800 if is_eng_submission(submissionfile) else 11*1000
  overall_accuracy = overall_tpfnfp['tp']/float(numsent)
  
  # Calculating Micro-average Fscore.
  microavg_scores = Score(overall_tpfnfp['tp'], overall_tpfnfp['fn'],
                          overall_tpfnfp['fp'])
  microavg_fscore = microavg_scores.fscore
  # Calculating per-group scores.
  lang_fscore = {}
  for lang in lang_tpfnfp:
    fscore = Score(lang_tpfnfp[lang]['tp'], lang_tpfnfp[lang]['fn'],
                   lang_tpfnfp[lang]['fp']).fscore
    lang_fscore[lang] = fscore

  # Calculating Macro-average Fscore.
  macroavg_fscore = sum(lang_fscore.values())/float(len(lang_fscore))
  
  # Since the number of documents per language is the same, we see that 
  # micro-avg prec == microavg recall == microavgFscore == overall accuracy
  print submissionfile
  #print "Micro-avg Fscore:", microavg_fscore
  print "Overall Accuracy:", '%.4f' % overall_accuracy
  print "Macro-avg Fscore:", '%.4f' % macroavg_fscore
  #print "\t".join(map(str,[submissionfile, macroavg_fscore, overall_accuracy]))
  #print
  return overall_accuracy, macroavg_fscore 

def compute_pergroup_scores(confusionmatrix):
  pergroupscores = defaultdict(Counter)
  ##print "Per-group scores:"
  for lang in sorted(confusionmatrix):
    group = lang.split('_')[0]
    pergroupscores[group]['tp']+= confusionmatrix[lang][lang]
    pergroupscores[group]['total']+= sum(confusionmatrix[lang].values())
  
  pergroupaccuracy = {} 
  for group in pergroupscores:
    _scores = pergroupscores[group]
    acc = _scores['tp']/float(_scores['total'])
    ##print group.upper(), acc
    pergroupaccuracy[group.upper()] = acc
  print
  return pergroupaccuracy


def main(submission):
  bestoverall_macroavgfscore = {}
  bestoverall_accuracy = {}
  bestoverall_group = defaultdict(Counter)
  
  for submissionfile, confusionmatrix in \
  sorted(generate_confusion_matrix(submission).iteritems()):
    ##print submissionfile, confusionmatrix
    overall_accuracy, macroavg_fscore = compute_overall_scores(confusionmatrix,
                                                               submissionfile)
    pergroupaccuracy = compute_pergroup_scores(confusionmatrix)
    
    bestoverall_macroavgfscore[submissionfile] = macroavg_fscore
    bestoverall_accuracy[submissionfile] = overall_accuracy
    
    for lang in pergroupaccuracy:
      bestoverall_group[lang][submissionfile] = pergroupaccuracy[lang]
  
  
  bestaccfile ,bestacc = \
  max(bestoverall_accuracy.iteritems(), key=operator.itemgetter(1))
  print "\t".join(map(str,["Best Overall Accuracy:", '%.4f' % bestacc, \
                           "from", bestaccfile]))
  
  bestfscorefile ,bestfscore = \
  max(bestoverall_macroavgfscore.iteritems(), key=operator.itemgetter(1))
  print "\t".join(map(str,["Best Macro-average F-score:", \
  '%.4f' % bestfscore, "from", bestfscorefile]))
  print
  print "Best per-group Accuracy:"
  for group in sorted(bestoverall_group):
    bestgroupfilename, bestacc = \
    max(bestoverall_group[group].iteritems(), key=operator.itemgetter(1))
    #print "Group", group+":",  bestacc, "from", bestgroupfilename
    print "\t".join(map(str,["Group", group+":",  bestacc, "from", bestgroupfilename]))
  print
###########################################################


#def main(team_submission):
#  pass

if __name__ == '__main__':
  import sys
  if len(sys.argv) != 2:
    sys.stderr.write('Usage: python %s teamsubmission.zip\n' % sys.argv[0])
    sys.exit(1)
  main(sys.argv[1])


##################################################################
# To generate results for each group.
##################################################################

def redirect_output(file_name):
  def decorator(func):
    def wrapper(*args):
      with open(file_name, 'w') as f:
        original_stdout = sys.stdout
        sys.stdout = f
        func(*args)
      sys.stdout = original_stdout
    return wrapper
  return decorator
     

#submissions = ['rae-dslrae.zip', 'nrc-catego.zip',
#               'michigan-umich.zip', 'lira-dislang.zip',
#               'ude-dkpro-tc.zip','unimelbnlp-unimelbnlp.zip',
#               'clcg-disident.zip', 'qmul-systemd.zip']

#for team_submission in submissions:
#  outputfilename = team_submission.rpartition('.zip')[0]
#  newmain = redirect_output('results/'+outputfilename)(main)
#  newmain(team_submission)
