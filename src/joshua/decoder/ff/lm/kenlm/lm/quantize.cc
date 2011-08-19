#include "lm/quantize.hh"

#include "lm/lm_exception.hh"

#include <algorithm>
#include <numeric>

#include <unistd.h>

namespace lm {
namespace ngram {

/* Quantize into bins of equal size as described in
 * M. Federico and N. Bertoldi. 2006. How many bits are needed
 * to store probabilities for phrase-based translation? In Proc.
 * of the Workshop on Statistical Machine Translation, pages
 * 94–101, New York City, June. Association for Computa-
 * tional Linguistics.
 */

namespace {

void MakeBins(float *values, float *values_end, float *centers, uint32_t bins) {
  std::sort(values, values_end);
  const float *start = values, *finish;
  for (uint32_t i = 0; i < bins; ++i, ++centers, start = finish) {
    finish = values + (((values_end - values) * static_cast<uint64_t>(i + 1)) / bins);
    if (finish == start) {
      // zero length bucket.
      *centers = i ? *(centers - 1) : -std::numeric_limits<float>::infinity();
    } else {
      *centers = std::accumulate(start, finish, 0.0) / static_cast<float>(finish - start);
    }
  }
}

const char kSeparatelyQuantizeVersion = 2;

} // namespace

void SeparatelyQuantize::UpdateConfigFromBinary(int fd, const std::vector<uint64_t> &/*counts*/, Config &config) {
  char version;
  if (read(fd, &version, 1) != 1 || read(fd, &config.prob_bits, 1) != 1 || read(fd, &config.backoff_bits, 1) != 1) 
    UTIL_THROW(util::ErrnoException, "Failed to read header for quantization.");
  if (version != kSeparatelyQuantizeVersion) UTIL_THROW(FormatLoadException, "This file has quantization version " << (unsigned)version << " but the code expects version " << (unsigned)kSeparatelyQuantizeVersion);
  AdvanceOrThrow(fd, -3);
}

void SeparatelyQuantize::SetupMemory(void *start, const Config &config) {
  // Reserve 8 byte header for bit counts.  
  start_ = reinterpret_cast<float*>(static_cast<uint8_t*>(start) + 8);
  prob_bits_ = config.prob_bits;
  backoff_bits_ = config.backoff_bits;
  // We need the reserved values.  
  if (config.prob_bits == 0) UTIL_THROW(ConfigException, "You can't quantize probability to zero");
  if (config.backoff_bits == 0) UTIL_THROW(ConfigException, "You can't quantize backoff to zero");
  if (config.prob_bits > 25) UTIL_THROW(ConfigException, "For efficiency reasons, quantizing probability supports at most 25 bits.  Currently you have requested " << static_cast<unsigned>(config.prob_bits) << " bits.");
  if (config.backoff_bits > 25) UTIL_THROW(ConfigException, "For efficiency reasons, quantizing backoff supports at most 25 bits.  Currently you have requested " << static_cast<unsigned>(config.backoff_bits) << " bits.");
}

void SeparatelyQuantize::Train(uint8_t order, std::vector<float> &prob, std::vector<float> &backoff) {
  TrainProb(order, prob);

  // Backoff
  float *centers = start_ + TableStart(order) + ProbTableLength();
  *(centers++) = kNoExtensionBackoff;
  *(centers++) = kExtensionBackoff;
  MakeBins(&*backoff.begin(), &*backoff.end(), centers, (1ULL << backoff_bits_) - 2);
}

void SeparatelyQuantize::TrainProb(uint8_t order, std::vector<float> &prob) {
  float *centers = start_ + TableStart(order);
  *(centers++) = kBlankProb;
  MakeBins(&*prob.begin(), &*prob.end(), centers, (1ULL << prob_bits_) - 1);
}

void SeparatelyQuantize::FinishedLoading(const Config &config) {
  uint8_t *actual_base = reinterpret_cast<uint8_t*>(start_) - 8;
  *(actual_base++) = kSeparatelyQuantizeVersion; // version
  *(actual_base++) = config.prob_bits;
  *(actual_base++) = config.backoff_bits;
}

} // namespace ngram
} // namespace lm
