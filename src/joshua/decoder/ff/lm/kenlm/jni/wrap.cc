#include "lm/enumerate_vocab.hh"
#include "lm/model.hh"
#include "util/murmur_hash.hh"

#include <iostream>

#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include <pthread.h>

// Grr.  Everybody's compiler is slightly different and I'm trying to not depend on boost.   
#include <ext/hash_map>

// This is needed to compile on OS X Lion / gcc 4.2.1
namespace __gnu_cxx {
  template<>
    struct hash<unsigned long long int>
    {
      size_t
      operator()(unsigned long long int __x) const
      { return __x; }
    };
}

// Verify that jint and lm::ngram::WordIndex are the same size.  If this breaks for you, there's a need to revise probString.
namespace {

// TODO: force sync with lmStartSymID in Java.  Joshua reserves this.  
const lm::WordIndex klmStartSymID = 10000;

template <bool> struct StaticCheck {};
template <> struct StaticCheck<true> { typedef bool StaticAssertionPassed; };
typedef StaticCheck<sizeof(jint) == sizeof(lm::WordIndex)>::StaticAssertionPassed FloatSize;

// Vocab ids above what the vocabulary knows about are unknown and should be mapped to that.  
template <class Model> void FixArray(const Model &model, jint *begin, jint *end) {
  jint lm_bound = static_cast<jint>(model.GetVocabulary().Bound()) + klmStartSymID;
  for (jint *i = begin; i < end; ++i) {
    if (*i < klmStartSymID || *i >= lm_bound) { 
      *i = 0;
    } else {
      *i -= klmStartSymID;
    }
  }
}

char *PieceCopy(const StringPiece &str) {
  char *ret = (char*)malloc(str.size() + 1);
  memcpy(ret, str.data(), str.size());
  ret[str.size()] = 0;
  return ret;
}

// The normal kenlm vocab isn't growable (words can't be added to it).  However, Joshua requires this.  So I wrote this.  Not the most efficient thing in the world.  
class GrowableVocab : public lm::EnumerateVocab {
  public:
    GrowableVocab() {
      UTIL_THROW_IF(pthread_rwlock_init(&lock_, NULL), util::ErrnoException, "Failed to initialize phtread lock.");
    }

    ~GrowableVocab() {
      for (std::vector<char *>::const_iterator i = id_to_string_.begin(); i != id_to_string_.end(); ++i) {
        free(*i);
      }
      if (pthread_rwlock_destroy(&lock_)) {
        std::cerr << "Failed to destroy pthread lock." << std::endl;
      }
    }

    // For enumerate vocab.  Adds klmStartSymID to the requested index.  
    void Add(lm::WordIndex index, const StringPiece &str) {
      index += klmStartSymID;
      if (index < id_to_string_.size() && id_to_string_[index]) UTIL_THROW(lm::VocabLoadException, "Vocab id " << index << " already contains " << id_to_string_[index] << " and an attempt was made to insert " << str << ".  This is an error even if the strings are the same.");
      std::pair<uint64_t, lm::WordIndex> to_ins;
      to_ins.first = util::MurmurHashNative(str.data(), str.size());
      to_ins.second = index;
      if (!string_to_id_.insert(to_ins).second) {
        UTIL_THROW(lm::VocabLoadException, "Duplicate word " << str);
      }
      if (id_to_string_.size() <= index) {
        id_to_string_.resize(index + 1);
      }
      id_to_string_[index] = PieceCopy(str);
    }

    // For Java API.  
    lm::WordIndex FindOrAdd(const StringPiece &str) {
      std::pair<uint64_t, lm::WordIndex> to_ins;
      to_ins.first = util::MurmurHashNative(str.data(), str.size());
      to_ins.second = id_to_string_.size();
      ReadLockOrDie();
      const __gnu_cxx::hash_map<uint64_t, lm::WordIndex> &force_const = string_to_id_;
      __gnu_cxx::hash_map<uint64_t, lm::WordIndex>::const_iterator i = force_const.find(to_ins.first);
      if (i != force_const.end()) {
        UnlockOrDie();
        return i->second;
      }
      UnlockOrDie();
      WriteLockOrDie();
      std::pair<__gnu_cxx::hash_map<uint64_t, lm::WordIndex>::iterator, bool> ret(string_to_id_.insert(to_ins));
      // Found?
      if (!ret.second) { UnlockOrDie(); return ret.first->second; }
      id_to_string_.push_back(PieceCopy(str));
      UnlockOrDie();
      return to_ins.second;
    }

    const char *Word(lm::WordIndex index) const {
      ReadLockOrDie();
      assert(index < in_to_string_.size());
      const char *ret = id_to_string_[index];
      UnlockOrDie();
      return ret;
    }

  private:
    // I really wish I had boost threads.  
    void ReadLockOrDie() const {
      if (pthread_rwlock_rdlock(&lock_)) {
        std::cerr << "Failed to pthread read lock." << std::endl;
        abort();
      }
    }
    void WriteLockOrDie() const {
      if (pthread_rwlock_wrlock(&lock_)) {
        std::cerr << "Failed to pthread write lock." << std::endl;
        abort();
      }
    }
 
    void UnlockOrDie() const {
      if (pthread_rwlock_unlock(&lock_)) {
        std::cerr << "Failed to pthread unlock." << std::endl;
        abort();
      }
    }

    __gnu_cxx::hash_map<uint64_t, lm::WordIndex> string_to_id_;
    std::vector<char *> id_to_string_;

    mutable pthread_rwlock_t lock_;
};

/* Rather than handle several different instantiations over JNI, we'll just do virtual calls C++-side. */
class VirtualBase {
  public:
    virtual ~VirtualBase() {}

    GrowableVocab &Vocab() { return vocab_; }

    const GrowableVocab &Vocab() const { return vocab_; }

    virtual float Prob(jint *begin, jint *end) const = 0;

    virtual float ProbString(jint *const begin, jint *const end, jint start) const = 0;

    virtual uint8_t Order() const = 0;

  protected:
    VirtualBase() {}

  private:
    GrowableVocab vocab_;
};

lm::ngram::Config MungeConfig(GrowableVocab &vocab) {
  lm::ngram::Config ret;
  ret.enumerate_vocab = &vocab;
  return ret;
}

template <class Model> class VirtualImpl : public VirtualBase {
  public:
    VirtualImpl(const char *name, float fake_oov_cost) : m_(name, MungeConfig(Vocab())), fake_oov_cost_(fake_oov_cost) {}

    ~VirtualImpl() {}

    float Prob(jint *const begin, jint *const end) const {
      FixArray(m_, begin, end);

      std::reverse(begin, end - 1);
      lm::ngram::State ignored;
      return *(end - 1) ? m_.FullScoreForgotState(reinterpret_cast<const lm::WordIndex*>(begin), reinterpret_cast<const lm::WordIndex*>(end - 1), *(end - 1), ignored).prob : fake_oov_cost_;
    }

    float ProbString(jint *const begin, jint *const end, jint start) const {
      FixArray(m_, begin, end);

      float prob;
      lm::ngram::State state;
      if (start == 0) {
        prob = 0;
        state = m_.NullContextState();
      } else {
        // Yes this rearranges the values in the copy.  No we won't refer to them again.   
        std::reverse(begin, begin + start);
        prob = m_.FullScoreForgotState(reinterpret_cast<const lm::WordIndex*>(begin), reinterpret_cast<const lm::WordIndex*>(begin + start), begin[start], state).prob;
        if (begin[start] == 0) prob = fake_oov_cost_;
        ++start;
      }
      lm::ngram::State state2;
      for (const jint *i = begin + start; ;) {
        if (i >= end) break;
        float got = m_.Score(state, *i, state2);
        prob += *(i++) ? got : fake_oov_cost_;
        if (i >= end) break;
        got = m_.Score(state2, *i, state);
        prob += *(i++) ? got : fake_oov_cost_;
      }
      return prob;
    }

    uint8_t Order() const {
      return m_.Order();
    }

  private:
    Model m_;
    float fake_oov_cost_;
};

VirtualBase *ConstructModel(const char *file_name, float fake_oov_cost) {
  using namespace lm::ngram;
  ModelType model_type;
  if (!RecognizeBinary(file_name, model_type)) model_type = HASH_PROBING;
  switch (model_type) {
    case HASH_PROBING:
      return new VirtualImpl<ProbingModel>(file_name, fake_oov_cost);
    case TRIE_SORTED:
      return new VirtualImpl<TrieModel>(file_name, fake_oov_cost);
    case ARRAY_TRIE_SORTED:
      return new VirtualImpl<ArrayTrieModel>(file_name, fake_oov_cost);
    case QUANT_TRIE_SORTED:
      return new VirtualImpl<QuantTrieModel>(file_name, fake_oov_cost);
    case QUANT_ARRAY_TRIE_SORTED:
      return new VirtualImpl<QuantArrayTrieModel>(file_name, fake_oov_cost);
    default:
      UTIL_THROW(lm::FormatLoadException, "Unrecognized file format " << (unsigned)model_type << " in file " << file_name);
  }
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenLM_construct(JNIEnv *env, jclass, jstring file_name, jfloat fake_oov_cost) {
  const char *str = env->GetStringUTFChars(file_name, 0);
  if (!str) return 0;
  jlong ret;
  try {
    ret = reinterpret_cast<jlong>(ConstructModel(str, fake_oov_cost));
  } catch (std::exception &e) {
    std::cerr << e.what() << std::endl;
    abort();
  }
  env->ReleaseStringUTFChars(file_name, str);
  return ret;
}

JNIEXPORT void JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenLM_destroy(JNIEnv *env, jclass, jlong pointer) {
  delete reinterpret_cast<VirtualBase*>(pointer);
}

JNIEXPORT jint JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenLM_order(JNIEnv *env, jclass, jlong pointer) {
  return reinterpret_cast<VirtualBase*>(pointer)->Order();
}

JNIEXPORT jint JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenLM_vocabFindOrAdd(JNIEnv *env, jclass, jlong pointer, jstring word) {
  const char *str = env->GetStringUTFChars(word, 0);
  if (!str) return 0;
  jint ret;
  try {
    ret = reinterpret_cast<VirtualBase*>(pointer)->Vocab().FindOrAdd(str);
  } catch (std::exception &e) {
    std::cerr << e.what() << std::endl;
    abort();
  }
  env->ReleaseStringUTFChars(word, str);
  return ret;
}

JNIEXPORT jstring JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenLM_vocabWord(JNIEnv *env, jclass, jlong pointer, jint index) {
  return env->NewStringUTF(reinterpret_cast<const VirtualBase*>(pointer)->Vocab().Word(index));
}

JNIEXPORT jfloat JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenLM_prob(JNIEnv *env, jclass, jlong pointer, jintArray arr) {
  jint length = env->GetArrayLength(arr);
  if (length <= 0) return 0.0;
  // Yes it's gcc only.  Sue me.  
  jint values[length];
  env->GetIntArrayRegion(arr, 0, length, values);

  return reinterpret_cast<const VirtualBase*>(pointer)->Prob(values, values + length);
}

JNIEXPORT jfloat JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenLM_probString(JNIEnv *env, jclass, jlong pointer, jintArray arr, jint start) {
  jint length = env->GetArrayLength(arr);
  if (length <= start) return 0.0;
  // Yes it's gcc only.  Sue me.  
  jint values[length];
  env->GetIntArrayRegion(arr, 0, length, values);

  return reinterpret_cast<const VirtualBase*>(pointer)->ProbString(values, values + length, start);
}

} // extern
