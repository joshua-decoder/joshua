#include "lm/bhiksha.hh"
#include "lm/config.hh"

#include <limits>

namespace lm {
namespace ngram {
namespace trie {

DontBhiksha::DontBhiksha(const void * /*base*/, uint64_t /*max_offset*/, uint64_t max_next, const Config &/*config*/) : 
  next_(util::BitsMask::ByMax(max_next)) {}

const uint8_t kArrayBhikshaVersion = 0;

void ArrayBhiksha::UpdateConfigFromBinary(int fd, Config &config) {
  uint8_t version;
  uint8_t configured_bits;
  if (read(fd, &version, 1) != 1 || read(fd, &configured_bits, 1) != 1) {
    UTIL_THROW(util::ErrnoException, "Could not read from binary file");
  }
  if (version != kArrayBhikshaVersion) UTIL_THROW(FormatLoadException, "This file has sorted array compression version " << (unsigned) version << " but the code expects version " << (unsigned)kArrayBhikshaVersion);
  config.pointer_bhiksha_bits = configured_bits;
}

namespace {

// Find argmin_{chopped \in [0, RequiredBits(max_next)]} ChoppedDelta(max_offset)
uint8_t ChopBits(uint64_t max_offset, uint64_t max_next, const Config &config) {
  uint8_t required = util::RequiredBits(max_next);
  uint8_t best_chop = 0;
  int64_t lowest_change = std::numeric_limits<int64_t>::max();
  // There are probably faster ways but I don't care because this is only done once per order at construction time.  
  for (uint8_t chop = 0; chop <= std::min(required, config.pointer_bhiksha_bits); ++chop) {
    int64_t change = (max_next >> (required - chop)) * 64 /* table cost in bits */
      - max_offset * static_cast<int64_t>(chop); /* savings in bits*/
    if (change < lowest_change) {
      lowest_change = change;
      best_chop = chop;
    }
  }
  return best_chop;
}

std::size_t ArrayCount(uint64_t max_offset, uint64_t max_next, const Config &config) {
  uint8_t required = util::RequiredBits(max_next);
  uint8_t chopping = ChopBits(max_offset, max_next, config);
  return (max_next >> (required - chopping)) + 1 /* we store 0 too */;
}
} // namespace

std::size_t ArrayBhiksha::Size(uint64_t max_offset, uint64_t max_next, const Config &config) {
  return sizeof(uint64_t) * (1 /* header */ + ArrayCount(max_offset, max_next, config)) + 7 /* 8-byte alignment */;
}

uint8_t ArrayBhiksha::InlineBits(uint64_t max_offset, uint64_t max_next, const Config &config) {
  return util::RequiredBits(max_next) - ChopBits(max_offset, max_next, config);
}

namespace {

void *AlignTo8(void *from) {
  uint8_t *val = reinterpret_cast<uint8_t*>(from);
  std::size_t remainder = reinterpret_cast<std::size_t>(val) & 7;
  if (!remainder) return val;
  return val + 8 - remainder;
}

} // namespace

ArrayBhiksha::ArrayBhiksha(void *base, uint64_t max_offset, uint64_t max_next, const Config &config)
  : next_inline_(util::BitsMask::ByBits(InlineBits(max_offset, max_next, config))),
    offset_begin_(reinterpret_cast<const uint64_t*>(AlignTo8(base)) + 1 /* 8-byte header */),
    offset_end_(offset_begin_ + ArrayCount(max_offset, max_next, config)),
    write_to_(reinterpret_cast<uint64_t*>(AlignTo8(base)) + 1 /* 8-byte header */ + 1 /* first entry is 0 */),
    original_base_(base) {}

void ArrayBhiksha::FinishedLoading(const Config &config) {
  // *offset_begin_ = 0 but without a const_cast.
  *(write_to_ - (write_to_ - offset_begin_)) = 0;

  if (write_to_ != offset_end_) UTIL_THROW(util::Exception, "Did not get all the array entries that were expected.");

  uint8_t *head_write = reinterpret_cast<uint8_t*>(original_base_);
  *(head_write++) = kArrayBhikshaVersion;
  *(head_write++) = config.pointer_bhiksha_bits;
}

void ArrayBhiksha::LoadedBinary() {
}

} // namespace trie
} // namespace ngram
} // namespace lm
