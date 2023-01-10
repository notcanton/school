import sys

import nltk
from nltk.corpus import brown
import numpy
from scipy.sparse import csr_matrix
from sklearn.linear_model import LogisticRegression
import pickle # TODO: TO THIS OUT LATER
# Load the Brown corpus with Universal Dependencies tags
# proportion is a float
# Returns a tuple of lists (sents, tags)
def load_training_corpus(proportion=1.0):
    brown_sentences = brown.tagged_sents(tagset='universal')
    num_used = int(proportion * len(brown_sentences))

    corpus_sents, corpus_tags = [None] * num_used, [None] * num_used
    for i in range(num_used):
        corpus_sents[i], corpus_tags[i] = zip(*brown_sentences[i])
    return (corpus_sents, corpus_tags)


# Generate word n-gram features
# words is a list of strings
# i is an int
# Returns a list of strings
def get_ngram_features(words, i):

    prev2word = words[i - 2] if i > 1 else "<s>"
    prev1word = words[i - 1] if i > 0 else "<s>"
    next1word = words[i + 1] if i < len(words) - 1 else "</s>"
    next2word = words[i + 2] if i < len(words) - 2 else "</s>"

    prevbigram = "prevbigram-" + prev1word
    nextbigram = "nextbigram-" + next1word
    prevskip = "prevskip-" + prev2word
    nextskip = "nextskip-" + next2word
    prevtrigram = "prevtrigram-" + prev1word + "-" + prev2word
    nexttrigram = "nexttrigram-" + next1word + "-" + next2word
    centertrigram = "centertrigram-" + prev1word + "-" + next1word

    return [prevbigram, nextbigram, prevskip, nextskip, prevtrigram, nexttrigram, centertrigram]


# Generate word-based features
# word is a string
# returns a list of strings
def get_word_features(word):

    result = []
    result.append("word-" + word)
    if word[0].isupper():
        result.append("capital")
    if word.isupper():
        result.append("allcaps")

    shape = ""
    for char in word:
        if char.isupper():
            shape += "X"
        elif char.islower():
            shape += "x"
        elif char.isdigit():
            shape += "d"
        else:
            shape += char

    shortshape = ""
    for i in range(len(shape) - 1):
        if shape[i] != shape[i + 1]:
            shortshape += shape[i]
    shortshape += shape[-1]


    result.append("wordshape-" + shape)
    result.append("short-wordshape-" + shortshape)
    
    if any(char.isdigit() for char in word):
        result.append("number")
    
    if any(char == "-" for char in word):
        result.append("hyphen")

    for i in range(1, 5):
        result.append("prefix" + str(i) + "-" + word[:i])
    
    for i in range(1, 5):
        result.append("suffix" + str(i) + "-" + word[-i:])

    return result


# Wrapper function for get_ngram_features and get_word_features
# words is a list of strings
# i is an int
# prevtag is a string
# Returns a list of strings
def get_features(words, i, prevtag):
    
    ngram_feature = get_ngram_features(words, i)
    word_feature = get_word_features(words[i])
    features = ngram_feature + word_feature
    features.append("tagbigram-" + prevtag)

    for i in range(len(features)):
        if not (features[i].startswith("wordshape") or features[i].startswith("short-wordshape")):
            features[i] = features[i].lower()

    return features



# Remove features that occur fewer than a given threshold number of time
# corpus_features is a list of lists, where each sublist corresponds to a sentence and has elements that are lists of strings (feature names)
# threshold is an int
# Returns a tuple (corpus_features, common_features)
def remove_rare_features(corpus_features, threshold=5):

    features = {}
    common_features = set()
    rare_features = set()

    for sentence in corpus_features:
        for feature_list in sentence:
            for feature in feature_list:
                count = features.get(feature, 0)
                features.update({feature : count + 1})


    for key, value in features.items():
        if value < threshold:
            rare_features.add(key)
        else:
            common_features.add(key)
    
    culled_corpus = []
    for sentence in corpus_features:
        culled_sentence = []
        for feature_list in sentence:
            culled_features = []
            for feature in feature_list:
                if feature in common_features:
                    culled_features.append(feature)
            culled_sentence.append(culled_features)
        culled_corpus.append(culled_sentence)

    return (culled_corpus, common_features)


# Build feature and tag dictionaries
# common_features is a set of strings
# corpus_tags is a list of lists of strings (tags)
# Returns a tuple (feature_dict, tag_dict)
def get_feature_and_label_dictionaries(common_features, corpus_tags):

    feature_dict = {}
    tag_dict = {}
    pos = 0
    for feature in common_features:
        if feature_dict.get(feature) is None:
            feature_dict.update({feature : pos})
            pos += 1

    pos = 0

    for sentence in corpus_tags:
        for tag in sentence:
            if tag_dict.get(tag) is None:
                tag_dict.update({tag : pos})
                pos += 1

    return (feature_dict, tag_dict)

# Build the label vector Y
# corpus_tags is a list of lists of strings (tags)
# tag_dict is a dictionary {string: int}
# Returns a Numpy array
def build_Y(corpus_tags, tag_dict):
    tag_list = []

    for sentence in corpus_tags:
        for tag in sentence:
            tag_list.append(tag_dict.get(tag))

    return numpy.array(tag_list)

# Build a sparse input matrix X
# corpus_features is a list of lists, where each sublist corresponds to a sentence and has elements that are lists of strings (feature names)
# feature_dict is a dictionary {string: int}
# Returns a Scipy.sparse csr_matrix
def build_X(corpus_features, feature_dict):

    rows = []
    cols = []

    word_index = 0
    for sentence in corpus_features:
        for word in sentence:
            for feature in word:
                if not feature_dict.get(feature) is None:
                    rows.append(word_index)
                    cols.append(feature_dict.get(feature))
            word_index += 1

    vals = [1] * len(rows)

    return csr_matrix((numpy.array(vals), (numpy.array(rows), numpy.array(cols))), shape=(word_index, max(feature_dict.values()) + 1))


# Train an MEMM tagger on the Brown corpus
# proportion is a float
# Returns a tuple (model, feature_dict, tag_dict)
def train(proportion=1.0):
    corpus, corpus_tags = load_training_corpus(proportion)
    corpus_features = []

    for sentence, tags in zip(corpus, corpus_tags):
        sentence_features = []
        for i in range(len(sentence)):
            features = get_features(sentence, i, tags[i - 1] if i > 0 else "<s>")
            sentence_features.append(features)
        corpus_features.append(sentence_features)

    corpus_features, common_features = remove_rare_features(corpus_features)
    feature_dict, tag_dict = get_feature_and_label_dictionaries(common_features, corpus_tags)

    X = build_X(corpus_features, feature_dict)
    Y = build_Y(corpus_tags, tag_dict)

    model = LogisticRegression(class_weight='balanced', solver='saga', multi_class='multinomial')
    model.fit(X, Y)

    return (model, feature_dict, tag_dict)

# Load the test set
# corpus_path is a string
# Returns a list of lists of strings (words)
def load_test_corpus(corpus_path):
    with open(corpus_path) as inf:
        lines = [line.strip().split() for line in inf]
    return [line for line in lines if len(line) > 0]


# Predict tags for a test sentence
# test_sent is a list containing a single list of strings
# model is a trained LogisticRegression
# feature_dict is a dictionary {string: int}
# reverse_tag_dict is a dictionary {int: string}
# Returns a tuple (Y_start, Y_pred)
def get_predictions(test_sent, model, feature_dict, reverse_tag_dict):
    
    Y_pred = numpy.empty([len(test_sent[0]) - 1, len(reverse_tag_dict), len(reverse_tag_dict)])

    for i in range(len(test_sent[0]))[1:]:
        features = []
        for prevtag in reverse_tag_dict.values():
            features.append(get_features(test_sent[0], i, prevtag))

        X = build_X([features], feature_dict)

        Y = model.predict_log_proba(X)
        Y_pred[i - 1, :, :] = Y[:, :]
    
    start_features = get_features(test_sent[0], 0, "<s>")
    X = build_X([[start_features]], feature_dict)
    Y_start = model.predict_log_proba(X)

    return (Y_start[0], Y_pred)


# Perform Viterbi decoding using predicted log probabilities
# Y_start is a Numpy array of size (1, T)
# Y_pred is a Numpy array of size (n-1, T, T)
# Returns a list of strings (tags)
def viterbi(Y_start, Y_pred):

    V = numpy.zeros([Y_pred.shape[0] + 1, Y_pred.shape[1]])
    BP = numpy.zeros([Y_pred.shape[0] + 1, Y_pred.shape[1]])

    V[0, :] = Y_start[:]

    # num_words = Y_pred.shape[0] + 1
    # num_tokens =Y_pred.shape[1]

    for word_index in range(Y_pred.shape[0]):
        for curr_index in range(Y_pred.shape[2]):
            curr_list = []
            for prev_index in range(Y_pred.shape[1]):
                curr_list.append(V[word_index, prev_index] + Y_pred[word_index, prev_index, curr_index])
            V[word_index + 1, curr_index] = max(curr_list)
            BP[word_index + 1, curr_index] = curr_list.index(max(curr_list))
    
    result = []
    curr_index = int(numpy.argmax(V[-1]))
    result.insert(0, curr_index)


    for word in BP[:0:-1]:
        curr_index = int(word[curr_index])
        result.insert(0, curr_index)

    return result


# Predict tags for a test corpus using a trained model
# corpus_path is a string
# model is a trained LogisticRegression
# feature_dict is a dictionary {string: int}
# tag_dict is a dictionary {string: int}
# Returns a list of lists of strings (tags)
def predict(corpus_path, model, feature_dict, tag_dict):
    prediction = []
    corpus = load_test_corpus(corpus_path) 
    reverse_tag_dict = dict((index, tag) for tag, index in tag_dict.items())

    for sentence in corpus:
        Y_start, Y_pred = get_predictions([sentence], model, feature_dict, reverse_tag_dict)
        tag_indices = viterbi(Y_start, Y_pred)
        print(tag_indices)
        tags = [reverse_tag_dict.get(index) for index in tag_indices]
        prediction.append(tags)

    return prediction


def main(args):
    # model, feature_dict, tag_dict = train(0.5)

    model = pickle.load(open("model.p", "rb"))
    feature_dict = pickle.load(open("feature_dict.p", "rb"))
    tag_dict = pickle.load(open("tag_dict.p", "rb"))

    # reverse_tag_dict = dict((index, tag) for tag, index in tag_dict.items())

    # Y_start, Y_pred = get_predictions([["I", "walked", "home", "."]], model, feature_dict, reverse_tag_dict)

    # viterbi(Y_start, Y_pred)

    # print(reverse_tag_dict)


    predictions = predict('test.txt', model, feature_dict, tag_dict)
    for test_sent in predictions:
       print(test_sent)


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
