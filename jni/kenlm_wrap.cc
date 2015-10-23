#include "lm/enumerate_vocab.hh"
#include "lm/model.hh"
#include "lm/left.hh"
#include "lm/state.hh"
#include "util/murmur_hash.hh"
#include "util/pool.hh"

#include <iostream>

#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include <pthread.h>

// Grr.  Everybody's compiler is slightly different and I'm trying to not depend on boost.   
#include <unordered_map>

// Verify that jint and lm::ngram::WordIndex are the same size. If this breaks
// for you, there's a need to revise probString.
namespace {

template<bool> struct StaticCheck {
};

template<> struct StaticCheck<true> {
  typedef bool StaticAssertionPassed;
};

typedef StaticCheck<sizeof(jint) == sizeof(lm::WordIndex)>::StaticAssertionPassed FloatSize;

typedef std::unordered_map<uint64_t, lm::ngram::ChartState*> PoolHash;

/**
 * A Chart bundles together a hash_map that maps ChartState signatures to a single object
 * instantiated using a pool. This allows duplicate states to avoid allocating separate
 * state objects at multiple places throughout a sentence, and also allows state to be
 * shared across KenLMs for the same sentence.
 */
struct Chart {
  // A cache for allocated chart objects
  PoolHash* poolHash;
  // Pool used to allocate new ones
  util::Pool* pool;

  Chart() {
    poolHash = new PoolHash();
    pool = new util::Pool();
  }

  ~Chart() {
    delete poolHash;
    pool->FreeAll();
    delete pool;
  }

  lm::ngram::ChartState* put(const lm::ngram::ChartState& state) {
    uint64_t hashValue = lm::ngram::hash_value(state);
  
    if (poolHash->find(hashValue) == poolHash->end()) {
      lm::ngram::ChartState* pointer = (lm::ngram::ChartState *)pool->Allocate(sizeof(lm::ngram::ChartState));
      *pointer = state;
      (*poolHash)[hashValue] = pointer;
    }

    return (*poolHash)[hashValue];
  }
};

// Vocab ids above what the vocabulary knows about are unknown and should
// be mapped to that. 
void MapArray(const std::vector<lm::WordIndex>& map, jint *begin, jint *end) {
  for (jint *i = begin; i < end; ++i) {
    *i = map[*i];
  }
}

char *PieceCopy(const StringPiece &str) {
  char *ret = (char*) malloc(str.size() + 1);
  memcpy(ret, str.data(), str.size());
  ret[str.size()] = 0;
  return ret;
}

// Rather than handle several different instantiations over JNI, we'll just
// do virtual calls C++-side.
class VirtualBase {
public:
  virtual ~VirtualBase() {
  }

  virtual float Prob(jint *begin, jint *end) const = 0;

  virtual float ProbRule(jlong *begin, jlong *end, lm::ngram::ChartState& state) const = 0;

  virtual float ProbString(jint * const begin, jint * const end,
      jint start) const = 0;

  virtual float EstimateRule(jlong *begin, jlong *end) const = 0;

  virtual uint8_t Order() const = 0;

  virtual bool RegisterWord(const StringPiece& word, const int joshua_id) = 0;

  void RememberReturnMethod(jclass chart_pair, jmethodID chart_pair_init) {
    chart_pair_ = chart_pair;
    chart_pair_init_ = chart_pair_init;
  }

  jclass ChartPair() const { return chart_pair_; }
  jmethodID ChartPairInit() const { return chart_pair_init_; }

protected:
  VirtualBase() {
  }

private:
  // Hack: these are remembered so we can avoid looking them up every time.
  jclass chart_pair_;
  jmethodID chart_pair_init_;
};

template<class Model> class VirtualImpl: public VirtualBase {
public:
  VirtualImpl(const char *name) :
      m_(name) {
    // Insert unknown id mapping.
    map_.push_back(0);
  }

  ~VirtualImpl() {
  }

  float Prob(jint * const begin, jint * const end) const {
    MapArray(map_, begin, end);

    std::reverse(begin, end - 1);
    lm::ngram::State ignored;
    return m_.FullScoreForgotState(
        reinterpret_cast<const lm::WordIndex*>(begin),
        reinterpret_cast<const lm::WordIndex*>(end - 1), *(end - 1),
        ignored).prob;
  }

  float ProbRule(jlong * const begin, jlong * const end, lm::ngram::ChartState& state) const {
    if (begin == end) return 0.0;
    lm::ngram::RuleScore<Model> ruleScore(m_, state);

    if (*begin < 0) {
      ruleScore.BeginNonTerminal(*reinterpret_cast<const lm::ngram::ChartState*>(-*begin));
    } else {
      const lm::WordIndex word = map_[*begin];
      if (word == m_.GetVocabulary().BeginSentence()) {
        ruleScore.BeginSentence();
      } else {
        ruleScore.Terminal(word);
      }
    }
    for (jlong* i = begin + 1; i != end; i++) {
      long word = *i;
      if (word < 0)
        ruleScore.NonTerminal(*reinterpret_cast<const lm::ngram::ChartState*>(-word));
      else
        ruleScore.Terminal(map_[word]);
    }
    return ruleScore.Finish();
  }

  float EstimateRule(jlong * const begin, jlong * const end) const {
    if (begin == end) return 0.0;
    lm::ngram::ChartState nullState;
    lm::ngram::RuleScore<Model> ruleScore(m_, nullState);

    if (*begin < 0) {
      ruleScore.Reset();
    } else {
      const lm::WordIndex word = map_[*begin];
      if (word == m_.GetVocabulary().BeginSentence()) {
        ruleScore.BeginSentence();
      } else {
        ruleScore.Terminal(word);
      }
    }
    for (jlong* i = begin + 1; i != end; i++) {
      long word = *i;
      if (word < 0)
        ruleScore.Reset();
      else
        ruleScore.Terminal(map_[word]);
    }
    return ruleScore.Finish();
  }

  float ProbString(jint * const begin, jint * const end, jint start) const {
    MapArray(map_, begin, end);

    float prob;
    lm::ngram::State state;
    if (start == 0) {
      prob = 0;
      state = m_.NullContextState();
    } else {
      std::reverse(begin, begin + start);
      prob = m_.FullScoreForgotState(
          reinterpret_cast<const lm::WordIndex*>(begin),
          reinterpret_cast<const lm::WordIndex*>(begin + start),
          begin[start], state).prob;
      ++start;
    }
    lm::ngram::State state2;
    for (const jint *i = begin + start;;) {
      if (i >= end)
        break;
      float got = m_.Score(state, *i, state2);
      i++;
      prob += got;
      if (i >= end)
        break;
      got = m_.Score(state2, *i, state);
      i++;
      prob += got;
    }
    return prob;
  }

  uint8_t Order() const {
    return m_.Order();
  }

  bool RegisterWord(const StringPiece& word, const int joshua_id) {
    if (map_.size() <= joshua_id) {
      map_.resize(joshua_id + 1, 0);
    }
    bool already_present = false;
    if (map_[joshua_id] != 0)
      already_present = true;
    map_[joshua_id] = m_.GetVocabulary().Index(word);
    return already_present;
  }

private:
  Model m_;
  std::vector<lm::WordIndex> map_;
};

VirtualBase *ConstructModel(const char *file_name) {
  using namespace lm::ngram;
  ModelType model_type;
  if (!RecognizeBinary(file_name, model_type))
    model_type = HASH_PROBING;
  switch (model_type) {
  case PROBING:
    return new VirtualImpl<ProbingModel>(file_name);
  case REST_PROBING:
    return new VirtualImpl<RestProbingModel>(file_name);
  case TRIE:
    return new VirtualImpl<TrieModel>(file_name);
  case ARRAY_TRIE:
    return new VirtualImpl<ArrayTrieModel>(file_name);
  case QUANT_TRIE:
    return new VirtualImpl<QuantTrieModel>(file_name);
  case QUANT_ARRAY_TRIE:
    return new VirtualImpl<QuantArrayTrieModel>(file_name);
  default:
    UTIL_THROW(
        lm::FormatLoadException,
        "Unrecognized file format " << (unsigned) model_type
            << " in file " << file_name);
  }
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_joshua_decoder_ff_lm_KenLM_construct(
    JNIEnv *env, jclass, jstring file_name) {
  const char *str = env->GetStringUTFChars(file_name, 0);
  if (!str)
    return 0;

  VirtualBase *ret;
  try {
    ret = ConstructModel(str);

    // Get a class reference for the type pair that char
    jclass local_chart_pair = env->FindClass("joshua/decoder/ff/lm/KenLM$StateProbPair");
    UTIL_THROW_IF(!local_chart_pair, util::Exception, "Failed to find joshua/decoder/ff/lm/KenLM$StateProbPair");
    jclass chart_pair = (jclass)env->NewGlobalRef(local_chart_pair);
    env->DeleteLocalRef(local_chart_pair);

    // Get the Method ID of the constructor which takes an int
    jmethodID chart_pair_init = env->GetMethodID(chart_pair, "<init>", "(JF)V");
    UTIL_THROW_IF(!chart_pair_init, util::Exception, "Failed to find init method");

    ret->RememberReturnMethod(chart_pair, chart_pair_init);
  } catch (std::exception &e) {
    std::cerr << e.what() << std::endl;
    abort();
  }
  env->ReleaseStringUTFChars(file_name, str);
  return reinterpret_cast<jlong>(ret);
}

JNIEXPORT void JNICALL Java_joshua_decoder_ff_lm_KenLM_destroy(
    JNIEnv *env, jclass, jlong pointer) {
  VirtualBase *base = reinterpret_cast<VirtualBase*>(pointer);
  env->DeleteGlobalRef(base->ChartPair());
  delete base;
}

JNIEXPORT long JNICALL Java_joshua_decoder_ff_lm_KenLM_createPool(
    JNIEnv *env, jclass) {
  return reinterpret_cast<long>(new Chart());
}

JNIEXPORT void JNICALL Java_joshua_decoder_ff_lm_KenLM_destroyPool(
    JNIEnv *env, jclass, jlong pointer) {
  Chart* chart = reinterpret_cast<Chart*>(pointer);
  delete chart;
}

JNIEXPORT jint JNICALL Java_joshua_decoder_ff_lm_KenLM_order(
    JNIEnv *env, jclass, jlong pointer) {
  return reinterpret_cast<VirtualBase*>(pointer)->Order();
}

JNIEXPORT jboolean JNICALL Java_joshua_decoder_ff_lm_KenLM_registerWord(
    JNIEnv *env, jclass, jlong pointer, jstring word, jint id) {
  const char *str = env->GetStringUTFChars(word, 0);
  if (!str)
    return false;
  jint ret;
  try {
    ret = reinterpret_cast<VirtualBase*>(pointer)->RegisterWord(str, id);
  } catch (std::exception &e) {
    std::cerr << e.what() << std::endl;
    abort();
  }
  env->ReleaseStringUTFChars(word, str);
  return ret;
}

JNIEXPORT jfloat JNICALL Java_joshua_decoder_ff_lm_KenLM_prob(
    JNIEnv *env, jclass, jlong pointer, jintArray arr) {
  jint length = env->GetArrayLength(arr);
  if (length <= 0)
    return 0.0;
  // GCC only.
  jint values[length];
  env->GetIntArrayRegion(arr, 0, length, values);

  return reinterpret_cast<const VirtualBase*>(pointer)->Prob(values,
      values + length);
}

JNIEXPORT jfloat JNICALL Java_joshua_decoder_ff_lm_KenLM_probString(
    JNIEnv *env, jclass, jlong pointer, jintArray arr, jint start) {
  jint length = env->GetArrayLength(arr);
  if (length <= start)
    return 0.0;
  // GCC only.
  jint values[length];
  env->GetIntArrayRegion(arr, 0, length, values);

  return reinterpret_cast<const VirtualBase*>(pointer)->ProbString(values,
      values + length, start);
}

JNIEXPORT jobject JNICALL Java_joshua_decoder_ff_lm_KenLM_probRule(
  JNIEnv *env, jclass, jlong pointer, jlong chartPtr, jlongArray arr) {
  jint length = env->GetArrayLength(arr);
  // GCC only.
  jlong values[length];
  env->GetLongArrayRegion(arr, 0, length, values);

  // Compute the probability
  lm::ngram::ChartState outState;
  const VirtualBase *base = reinterpret_cast<const VirtualBase*>(pointer);
  float prob = base->ProbRule(values, values + length, outState);

  Chart* chart = reinterpret_cast<Chart*>(chartPtr);
  lm::ngram::ChartState* outStatePtr = chart->put(outState);

  // Call back constructor to allocate a new instance, with an int argument
  return env->NewObject(base->ChartPair(), base->ChartPairInit(), (long)outStatePtr, prob);
}

JNIEXPORT jfloat JNICALL Java_joshua_decoder_ff_lm_KenLM_estimateRule(
  JNIEnv *env, jclass, jlong pointer, jlongArray arr) {
  jint length = env->GetArrayLength(arr);
  // GCC only.
  jlong values[length];
  env->GetLongArrayRegion(arr, 0, length, values);

  // Compute the probability
  return reinterpret_cast<const VirtualBase*>(pointer)->EstimateRule(values,
      values + length);
}

} // extern
