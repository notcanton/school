import re
import sys

import nltk
import numpy
from sklearn.linear_model import LogisticRegression


negation_words = set(['not', 'no', 'never', 'nor', 'cannot'])
negation_enders = set(['but', 'however', 'nevertheless', 'nonetheless'])
sentence_enders = set(['.', '?', '!', ';'])


# Loads a training or test corpus
# corpus_path is a string
# Returns a list of (string, int) tuples
def load_corpus(corpus_path):
    corpus = []
    with open(corpus_path, 'r') as input:
        for line in input:
            line_list = line.split()
            corpus.append((line_list[:-1], int(line_list[-1])))
    return corpus


# Checks whether or not a word is a negation word
# word is a string
# Returns a boolean
def is_negation(word):
    if word in negation_words:
        return True
    if word.endswith("n't"):
        return True
    return False


# Modifies a snippet to add negation tagging
# snippet is a list of strings
# Returns a list of strings
def tag_negation(snippet):
    snippet_list = nltk.pos_tag(snippet)

    tagged_list = []
    negation = False
    prev_word = None

    for word, pos in snippet_list:
        if is_negation(word):
            tagged_list.append(word)
            negation = True
        elif word in negation_enders or word in sentence_enders or pos == "JJR" or pos == "RBR":
            tagged_list.append(word)
            negation = False
        elif word == "only" and prev_word == "not":
            tagged_list.append(word)
            negation = False
        elif negation:
            tagged_list.append("NOT_" + word)
        else:
            tagged_list.append(word)
        prev_word = word
    return tagged_list


# Assigns to each unigram an index in the feature vector
# corpus is a list of tuples (snippet, label)
# Returns a dictionary {word: index}
def get_feature_dictionary(corpus):
    feature_dictionary = {}
    pos = 0

    for snippet, label in corpus:
        for word in snippet:
            if feature_dictionary.get(word) is None:
                feature_dictionary.update({word: pos})
                pos += 1

    return feature_dictionary
    

# Converts a snippet into a feature vector
# snippet is a list of strings
# feature_dict is a dictionary {word: index}
# Returns a Numpy array
def vectorize_snippet(snippet, feature_dict):
    
    vector = numpy.zeros(len(feature_dict))

    for word in snippet:
        index = feature_dict.get(word)
        if index is not None:
            vector[index] += 1
    
    return vector


# Trains a classification model (in-place)
# corpus is a list of tuples (snippet, label)
# feature_dict is a dictionary {word: label}
# Returns a tuple (X, Y) where X and Y are Numpy arrays
def vectorize_corpus(corpus, feature_dict):

    x = numpy.empty([len(corpus), len(feature_dict)])
    y = numpy.empty([len(corpus)])

    count = 0

    for snippet, label in corpus:
        feature_vector = vectorize_snippet(snippet, feature_dict)
        x[count, :] = feature_vector[:]
        y[count] = label
        count += 1

    return (x, y)


# Performs min-max normalization (in-place)
# X is a Numpy array
# No return value
def normalize(X):

    num_row, num_col = X.shape

    for i in range(num_col):
        max_val = numpy.max(X[:, i])
        min_val = numpy.min(X[:, i])
        if max_val != min_val:
            for j in range(num_row):
                X[j, i] = ((X[j, i] - min_val) / (max_val - min_val))

    #TODO: this is technically wrong if max_val == min_val and they are greater than 1


# Trains a model on a training corpus
# corpus_path is a string
# Returns a LogisticRegression
def train(corpus_path):
    corpus = load_corpus(corpus_path)
    tagged_corpus = []
    for snippet, label in corpus:
        tagged_corpus.append((tag_negation(snippet), label))
    feature_dict = get_feature_dictionary(tagged_corpus)
    X, Y = vectorize_corpus(tagged_corpus, feature_dict)
    normalize(X)
    model = LogisticRegression()
    model.fit(X, Y)

    return (model, feature_dict)


# Calculate precision, recall, and F-measure
# Y_pred is a Numpy array
# Y_test is a Numpy array
# Returns a tuple of floats
def evaluate_predictions(Y_pred, Y_test):

    tp = 0
    fp = 0
    fn = 0

    for i in range(len(Y_pred)):
        pred = Y_pred[i]
        test = Y_test[i]
        if pred == 1 and test == 1:
            tp += 1
        elif test == 0 and pred == 1:
            fp += 1
        elif test == 1 and pred == 0:
            fn += 1
    
    precision = tp / (tp + fp)
    recall = tp / (tp + fn)
    fmeasure = 2 * ((precision * recall) / (precision + recall))

    return (precision, recall, fmeasure)


# Evaluates a model on a test corpus and prints the results
# model is a LogisticRegression
# corpus_path is a string
# Returns a tuple of floats
def test(model, feature_dict, corpus_path):
    corpus = load_corpus(corpus_path)
    tagged_corpus = []
    for snippet, label in corpus:
        tagged_corpus.append((tag_negation(snippet), label))
    X, Y_test = vectorize_corpus(tagged_corpus, feature_dict)
    normalize(X)

    Y_pred =  model.predict(X)

    return evaluate_predictions(Y_pred, Y_test)


# Selects the top k highest-weight features of a logistic regression model
# logreg_model is a trained LogisticRegression
# feature_dict is a dictionary {word: index}
# k is an int
def get_top_features(logreg_model, feature_dict, k=1):
    index_list = []
    word_list = []
    for i in range(len(logreg_model.coef_[0])):
        index_list.append((i, logreg_model.coef_[0][i]))
    index_list.sort(key=lambda x: x[1], reverse=True)
    
    for i in range(k):
        index, weight = index_list[i]
        word_list.append((list(feature_dict.keys())[list(feature_dict.values()).index(index)], weight))

    return word_list



def main(args):
    model, feature_dict = train('train.txt')

    print(test(model, feature_dict, 'test.txt'))

    weights = get_top_features(model, feature_dict, 5)
    for weight in weights:
        print(weight)
    
if __name__ == '__main__':

    # corpus = load_corpus('mytest.txt')
    # features = get_feature_dictionary(corpus)
    # array = vectorize_corpus(corpus, features)
    # X = normalize(array[0])
    sys.exit(main(sys.argv[1:]))
