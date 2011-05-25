#include "lm/model.hh"

#include <iostream>

#include <stdlib.h>
#include <jni.h>

// Verify that jint and lm::ngram::WordIndex are the same size.  If this breaks for you, there's a need to revise probString.
namespace {
template <bool> struct StaticCheck {};
template <> struct StaticCheck<true> { typedef bool StaticAssertionPassed; };
typedef StaticCheck<sizeof(jint) == sizeof(lm::WordIndex)>::StaticAssertionPassed FloatSize;
} // namespace

extern "C" {

JNIEXPORT jint JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenLM_classify(JNIEnv *env, jclass, jstring file_name) {
  const char *str = env->GetStringUTFChars(file_name, 0);
  if (!str) return 0;
  lm::ngram::ModelType model_type;
  bool is_binary = false;
  try {
    is_binary = RecognizeBinary(str, model_type);
  } catch (std::exception &e) {
    std::cerr << e.what() << std::endl;
    abort();
  }
  env->ReleaseStringUTFChars(file_name, str);
  return is_binary ? -1 : static_cast<int>(model_type);
}

/* Probing */
JNIEXPORT jlong JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenProbing_create(JNIEnv *env, jclass, jstring file_name) {
  const char *str = env->GetStringUTFChars(file_name, 0);
  if (!str) return 0;
  jlong ret;
  try {
    ret = reinterpret_cast<jlong>(new lm::ngram::ProbingModel(str));
  } catch (std::exception &e) {
    std::cerr << e.what() << std::endl;
    abort();
  }
  env->ReleaseStringUTFChars(file_name, str);
  return ret;
}

JNIEXPORT void JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenProbing_destroy(JNIEnv *env, jclass, jlong pointer) {
  delete reinterpret_cast<lm::ngram::ProbingModel*>(pointer);
}

JNIEXPORT jint JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenProbing_order(JNIEnv *env, jclass, jlong pointer) {
  return reinterpret_cast<const lm::ngram::ProbingModel*>(pointer)->Order();
}

JNIEXPORT jint JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenProbing_vocab(JNIEnv *env, jclass, jlong pointer, jstring word) {
  const char *str = env->GetStringUTFChars(word, 0);
  if (!str) return 0;
  jint ret = reinterpret_cast<const lm::ngram::ProbingModel*>(pointer)->GetVocabulary().Index(str);
  env->ReleaseStringUTFChars(word, str);
  return ret;
}

JNIEXPORT jfloat JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenProbing_prob(JNIEnv *env, jclass, jlong pointer, jintArray arr) {
  jint length = env->GetArrayLength(arr);
  if (length <= 0) return 0.0;
  // Yes it's gcc only.  Sue me.  
  jint values[length];
  env->GetIntArrayRegion(arr, 0, length, values);
  const lm::ngram::ProbingModel &model = *reinterpret_cast<const lm::ngram::ProbingModel*>(pointer);

  std::reverse(values, values + length - 1);
  lm::ngram::State ignored;
  return model.FullScoreForgotState(reinterpret_cast<const lm::WordIndex*>(values), reinterpret_cast<const lm::WordIndex*>(values + length - 1), values[length - 1], ignored).prob;
}

JNIEXPORT jfloat JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenProbing_probString(JNIEnv *env, jclass, jlong pointer, jintArray arr, jint start) {
  jint length = env->GetArrayLength(arr);
  if (length <= start) return 0.0;
  // Yes it's gcc only.  Sue me.  
  jint values[length];
  env->GetIntArrayRegion(arr, 0, length, values);
  const lm::ngram::ProbingModel &model = *reinterpret_cast<const lm::ngram::ProbingModel*>(pointer);

  float prob = 0;
  lm::ngram::State state;
  if (start == 0) {
    state = model.NullContextState();
  } else {
    // Yes this rearranges the values in the copy.  No we won't refer to them again.   
    std::reverse(values, values + start);
    prob += model.FullScoreForgotState(reinterpret_cast<const lm::WordIndex*>(values), reinterpret_cast<const lm::WordIndex*>(values + start), values[start], state).prob;
    ++start;
  }
  lm::ngram::State state2;
  const jint *const last = values + length;
  for (const jint *i = values + start; ;) {
    if (i >= last) break;
    prob += model.FullScore(state, *(i++), state2).prob;
    if (i >= last) break;
    prob += model.FullScore(state2, *(i++), state).prob;
  }
  return prob;
}

/* Duplicate of the above with s/Probing/Trie/g */
JNIEXPORT jlong JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenTrie_create(JNIEnv *env, jclass, jstring file_name) {
  const char *str = env->GetStringUTFChars(file_name, 0);
  if (!str) return 0;
  jlong ret;
  try {
    ret = reinterpret_cast<jlong>(new lm::ngram::TrieModel(str));
  } catch (std::exception &e) {
    std::cerr << e.what() << std::endl;
    abort();
  }
  env->ReleaseStringUTFChars(file_name, str);
  return ret;
}

JNIEXPORT void JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenTrie_destroy(JNIEnv *env, jclass, jlong pointer) {
  delete reinterpret_cast<lm::ngram::TrieModel*>(pointer);
}

JNIEXPORT jint JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenTrie_order(JNIEnv *env, jclass, jlong pointer) {
  return reinterpret_cast<const lm::ngram::TrieModel*>(pointer)->Order();
}

JNIEXPORT jint JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenTrie_vocab(JNIEnv *env, jclass, jlong pointer, jstring word) {
  const char *str = env->GetStringUTFChars(word, 0);
  if (!str) return 0;
  jint ret = reinterpret_cast<const lm::ngram::TrieModel*>(pointer)->GetVocabulary().Index(str);
  env->ReleaseStringUTFChars(word, str);
  return ret;
}

JNIEXPORT jfloat JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenTrie_prob(JNIEnv *env, jclass, jlong pointer, jintArray arr) {
  jint length = env->GetArrayLength(arr);
  if (length <= 0) return 0.0;
  // Yes it's gcc only.  Sue me.  
  jint values[length];
  env->GetIntArrayRegion(arr, 0, length, values);
  const lm::ngram::TrieModel &model = *reinterpret_cast<const lm::ngram::TrieModel*>(pointer);

  std::reverse(values, values + length - 1);
  lm::ngram::State ignored;
  return model.FullScoreForgotState(reinterpret_cast<const lm::WordIndex*>(values), reinterpret_cast<const lm::WordIndex*>(values + length - 1), values[length - 1], ignored).prob;
}

JNIEXPORT jfloat JNICALL Java_joshua_decoder_ff_lm_kenlm_jni_KenTrie_probString(JNIEnv *env, jclass, jlong pointer, jintArray arr, jint start) {
  jint length = env->GetArrayLength(arr);
  if (length <= start) return 0.0;
  // Yes it's gcc only.  Sue me.  
  jint values[length];
  env->GetIntArrayRegion(arr, 0, length, values);
  const lm::ngram::TrieModel &model = *reinterpret_cast<const lm::ngram::TrieModel*>(pointer);

  float prob = 0;
  lm::ngram::State state;
  if (start == 0) {
    state = model.NullContextState();
  } else {
    // Yes this rearranges the values in the copy.  No we won't refer to them again.   
    std::reverse(values, values + start);
    prob += model.FullScoreForgotState(reinterpret_cast<const lm::WordIndex*>(values), reinterpret_cast<const lm::WordIndex*>(values + start), values[start], state).prob;
    ++start;
  }
  lm::ngram::State state2;
  const jint *const last = values + length;
  for (const jint *i = values + start; ;) {
    if (i >= last) break;
    prob += model.FullScore(state, *(i++), state2).prob;
    if (i >= last) break;
    prob += model.FullScore(state2, *(i++), state).prob;
  }
  return prob;
}

} // extern
