import argparse
import math
import random
from nltk.tokenize import sent_tokenize, word_tokenize
from typing import List
from typing import Tuple
from typing import Generator


# Generator for all n-grams in text
# n is a (non-negative) int
# text is a list of strings
# Yields n-gram tuples of the form (string, context), where context is a tuple of strings
def get_ngrams(n: int, text: List[str]) -> Generator[Tuple[str, Tuple[str, ...]], None, None]:

    # Appends n - 1 start tokens 
    for i in range(n - 1):
        text.insert(0, '<s>')

    # Appends end token
    text.append('</s>')
    
    # Generate next tuple
    for i in range(n - 1, len(text)):
        yield (text[i], tuple(text[i - (n - 1) : i]))



# Loads and tokenizes a corpus
# corpus_path is a string
# Returns a list of sentences, where each sentence is a list of strings
def load_corpus(corpus_path: str) -> List[List[str]]:
    words = []

    with open(corpus_path, 'r') as corpus_file:
        text = corpus_file.read()

    for paragraph in text.split('\n\n'):
        for sentence in sent_tokenize(paragraph):
            words.append(word_tokenize(sentence))
    return words


# Builds an n-gram model from a corpus
# n is a (non-negative) int
# corpus_path is a string
# Returns an NGramLM
def create_ngram_lm(n: int, corpus_path: str) -> 'NGramLM':
    corpus = load_corpus(corpus_path)
    NGram = NGramLM(n)

    for sentence in corpus:
        NGram.update(sentence)

    return NGram


# An n-gram language model
class NGramLM:
    def __init__(self, n: int):
        self.n = n
        self.ngram_counts = {}
        self.context_counts = {}
        self.vocabulary = set()

    # Updates internal counts based on the n-grams in text
    # text is a list of strings
    # No return value
    def update(self, text: List[str]) -> None:
        for ngram in get_ngrams(self.n, text):
            
            # Updates ngram_counts
            ngram_count = self.ngram_counts.get(ngram, 0)
            self.ngram_counts.update({ngram: ngram_count + 1})

            # Updates context_counts
            context_count = self.context_counts.get(ngram[1], 0)
            self.context_counts.update({ngram[1]: context_count + 1})

            # Updates vocabulary
            self.vocabulary.add(ngram[0])

    # Calculates the MLE probability of an n-gram
    # word is a string
    # context is a tuple of strings
    # delta is an float
    # Returns a float
    def get_ngram_prob(self, word: str, context: Tuple[str, ...], delta= .0) -> float:

        context_count = self.context_counts.get(context, .0)

        # With Laplace smoothing
        if (delta != .0):
            return ((self.ngram_counts.get((word, context), .0) + delta) / (context_count + (delta * (len(self.vocabulary)))))


        # No Laplace smoothing
        # The context has not been seen -> return 1 / |V|
        elif (context_count == .0):
            return (1 / len(self.vocabulary))

        # Else return c(v, w) / c(v)
        else:
            return (self.ngram_counts.get((word, context), .0) / context_count)

    # Calculates the log probability of a sentence
    # sent is a list of strings
    # delta is a float
    # Returns a float
    def get_sent_log_prob(self, sent: List[str], delta=.0) -> float:
        sum_prob = 0.0

        for ngram in get_ngrams(self.n, sent):
            prob = self.get_ngram_prob(ngram[0], ngram[1], delta)
            if prob == 0:
                return float('-inf')
            sum_prob += math.log(self.get_ngram_prob(ngram[0], ngram[1], delta), 2)

        return sum_prob

    # Calculates the perplexity of a language model on a test corpus
    # corpus is a list of lists of strings
    # Returns a float
    def get_perplexity(self, corpus: List[List[str]], delta=.0) -> float:
        corpus_prob = 0.0
        n = 0

        for sentence in corpus:
            n += len(sentence)
            corpus_prob +=  self.get_sent_log_prob(sentence, delta)


        return math.pow(2, (-(corpus_prob/n)))

    # Samples a word from the probability distribution for a given context
    # context is a tuple of strings
    # delta is an float
    # Returns a string
    def generate_random_word(self, context: Tuple[str, ...], delta=.0) -> str:
        r = random.random()
        curr_prob_dist = 0

        for word in sorted(self.vocabulary):
            curr_prob_dist += self.get_ngram_prob(word, context, delta)
            if (r < curr_prob_dist):
                return word

    # Generates a random sentence
    # max_length is an int
    # delta is a float
    # Returns a string
    def generate_random_text(self, max_length: int, delta=.0) -> str:
        context = []

        for i in range(self.n - 1):
            context.append('<s>')

        for i in range(max_length):
            next_word = self.generate_random_word(tuple(context[i:i + self.n]), delta)
            context.append(next_word)
            if next_word == '</s>':
                break
    
        return ' '.join(context[self.n - 1:])


def main(corpus_path: str, delta: float, seed: int):
    trigram_lm = create_ngram_lm(3, corpus_path)
    s1 = 'God has given it to me, let him who touches it beware!'
    s2 = 'Where is the prince, my Dauphin?'

    #print(trigram_lm.get_sent_log_prob(word_tokenize(s1)))
    # print(trigram_lm.get_sent_log_prob(word_tokenize(s2)))
    #print(trigram_lm.get_perplexity([word_tokenize(s1)]))
    print(trigram_lm.generate_random_text(20))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="N-gram")
    parser.add_argument('corpus_path', nargs="?", type=str, default='warpeace.txt', help='Path to corpus file')
    parser.add_argument('delta', nargs="?", type=float, default=.0, help='Delta value used for smoothing')
    parser.add_argument('seed', nargs="?", type=int, default=324057, help='Random seed used for text generation')
    args = parser.parse_args()
    random.seed(args.seed)
    main(args.corpus_path, args.delta, args.seed)
