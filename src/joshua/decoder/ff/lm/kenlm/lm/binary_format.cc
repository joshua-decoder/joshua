#include "lm/binary_format.hh"

#include "lm/lm_exception.hh"
#include "util/file_piece.hh"

#include <limits>
#include <string>

#include <fcntl.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

namespace lm {
namespace ngram {
namespace {
const char kMagicBeforeVersion[] = "mmap lm http://kheafield.com/code format version";
const char kMagicBytes[] = "mmap lm http://kheafield.com/code format version 4\n\0";
// This must be shorter than kMagicBytes and indicates an incomplete binary file (i.e. build failed). 
const char kMagicIncomplete[] = "mmap lm http://kheafield.com/code incomplete\n";
const long int kMagicVersion = 4;

// Test values.  
struct Sanity {
  char magic[sizeof(kMagicBytes)];
  float zero_f, one_f, minus_half_f;
  WordIndex one_word_index, max_word_index;
  uint64_t one_uint64;

  void SetToReference() {
    std::memcpy(magic, kMagicBytes, sizeof(magic));
    zero_f = 0.0; one_f = 1.0; minus_half_f = -0.5;
    one_word_index = 1;
    max_word_index = std::numeric_limits<WordIndex>::max();
    one_uint64 = 1;
  }
};

const char *kModelNames[3] = {"hashed n-grams with probing", "hashed n-grams with sorted uniform find", "bit packed trie"};

std::size_t Align8(std::size_t in) {
  std::size_t off = in % 8;
  if (!off) return in;
  return in + 8 - off;
}

std::size_t TotalHeaderSize(unsigned char order) {
  return Align8(sizeof(Sanity) + sizeof(FixedWidthParameters) + sizeof(uint64_t) * order);
}

void ReadLoop(int fd, void *to_void, std::size_t size) {
  uint8_t *to = static_cast<uint8_t*>(to_void);
  while (size) {
    ssize_t ret = read(fd, to, size);
    if (ret == -1) UTIL_THROW(util::ErrnoException, "Failed to read from binary file");
    if (ret == 0) UTIL_THROW(util::ErrnoException, "Binary file too short");
    to += ret;
    size -= ret;
  }
}

void WriteHeader(void *to, const Parameters &params) {
  Sanity header = Sanity();
  header.SetToReference();
  memcpy(to, &header, sizeof(Sanity));
  char *out = reinterpret_cast<char*>(to) + sizeof(Sanity);

  *reinterpret_cast<FixedWidthParameters*>(out) = params.fixed;
  out += sizeof(FixedWidthParameters);

  uint64_t *counts = reinterpret_cast<uint64_t*>(out);
  for (std::size_t i = 0; i < params.counts.size(); ++i) {
    counts[i] = params.counts[i];
  }
}

} // namespace

uint8_t *SetupJustVocab(const Config &config, uint8_t order, std::size_t memory_size, Backing &backing) {
  if (config.write_mmap) {
    std::size_t total = TotalHeaderSize(order) + memory_size;
    backing.vocab.reset(util::MapZeroedWrite(config.write_mmap, total, backing.file), total, util::scoped_memory::MMAP_ALLOCATED);
    strncpy(reinterpret_cast<char*>(backing.vocab.get()), kMagicIncomplete, TotalHeaderSize(order));
    return reinterpret_cast<uint8_t*>(backing.vocab.get()) + TotalHeaderSize(order);
  } else {
    backing.vocab.reset(util::MapAnonymous(memory_size), memory_size, util::scoped_memory::MMAP_ALLOCATED);
    return reinterpret_cast<uint8_t*>(backing.vocab.get());
  }
}

uint8_t *GrowForSearch(const Config &config, std::size_t memory_size, Backing &backing) {
  if (config.write_mmap) {
    // Grow the file to accomodate the search, using zeros.  
    if (-1 == ftruncate(backing.file.get(), backing.vocab.size() + memory_size))
      UTIL_THROW(util::ErrnoException, "ftruncate on " << config.write_mmap << " to " << (backing.vocab.size() + memory_size) << " failed");

    // We're skipping over the header and vocab for the search space mmap.  mmap likes page aligned offsets, so some arithmetic to round the offset down.  
    off_t page_size = sysconf(_SC_PAGE_SIZE);
    off_t alignment_cruft = backing.vocab.size() % page_size;
    backing.search.reset(util::MapOrThrow(alignment_cruft + memory_size, true, util::kFileFlags, false, backing.file.get(), backing.vocab.size() - alignment_cruft), alignment_cruft + memory_size, util::scoped_memory::MMAP_ALLOCATED);

    return reinterpret_cast<uint8_t*>(backing.search.get()) + alignment_cruft;
  } else {
    backing.search.reset(util::MapAnonymous(memory_size), memory_size, util::scoped_memory::MMAP_ALLOCATED);
    return reinterpret_cast<uint8_t*>(backing.search.get());
  } 
}

void FinishFile(const Config &config, ModelType model_type, const std::vector<uint64_t> &counts, Backing &backing) {
  if (config.write_mmap) {
    if (msync(backing.search.get(), backing.search.size(), MS_SYNC) || msync(backing.vocab.get(), backing.vocab.size(), MS_SYNC)) 
      UTIL_THROW(util::ErrnoException, "msync failed for " << config.write_mmap);
    // header and vocab share the same mmap.  The header is written here because we know the counts.  
    Parameters params;
    params.counts = counts;
    params.fixed.order = counts.size();
    params.fixed.probing_multiplier = config.probing_multiplier;
    params.fixed.model_type = model_type;
    params.fixed.has_vocabulary = config.include_vocab;
    WriteHeader(backing.vocab.get(), params);
  }
}

namespace detail {

bool IsBinaryFormat(int fd) {
  const off_t size = util::SizeFile(fd);
  if (size == util::kBadSize || (size <= static_cast<off_t>(sizeof(Sanity)))) return false;
  // Try reading the header.  
  util::scoped_memory memory;
  try {
    util::MapRead(util::LAZY, fd, 0, sizeof(Sanity), memory);
  } catch (const util::Exception &e) {
    return false;
  }
  Sanity reference_header = Sanity();
  reference_header.SetToReference();
  if (!memcmp(memory.get(), &reference_header, sizeof(Sanity))) return true;
  if (!memcmp(memory.get(), kMagicIncomplete, strlen(kMagicIncomplete))) {
    UTIL_THROW(FormatLoadException, "This binary file did not finish building");
  }
  if (!memcmp(memory.get(), kMagicBeforeVersion, strlen(kMagicBeforeVersion))) {
    char *end_ptr;
    const char *begin_version = static_cast<const char*>(memory.get()) + strlen(kMagicBeforeVersion);
    long int version = strtol(begin_version, &end_ptr, 10);
    if ((end_ptr != begin_version) && version != kMagicVersion) {
      UTIL_THROW(FormatLoadException, "Binary file has version " << version << " but this implementation expects version " << kMagicVersion << " so you'll have to use the ARPA to rebuild your binary");
    }
    UTIL_THROW(FormatLoadException, "File looks like it should be loaded with mmap, but the test values don't match.  Try rebuilding the binary format LM using the same code revision, compiler, and architecture");
  }
  return false;
}

void ReadHeader(int fd, Parameters &out) {
  if ((off_t)-1 == lseek(fd, sizeof(Sanity), SEEK_SET)) UTIL_THROW(util::ErrnoException, "Seek failed in binary file");
  ReadLoop(fd, &out.fixed, sizeof(out.fixed));
  if (out.fixed.probing_multiplier < 1.0)
    UTIL_THROW(FormatLoadException, "Binary format claims to have a probing multiplier of " << out.fixed.probing_multiplier << " which is < 1.0.");

  out.counts.resize(static_cast<std::size_t>(out.fixed.order));
  ReadLoop(fd, &*out.counts.begin(), sizeof(uint64_t) * out.fixed.order);
}

void MatchCheck(ModelType model_type, const Parameters &params) {
  if (params.fixed.model_type != model_type) {
    if (static_cast<unsigned int>(params.fixed.model_type) >= (sizeof(kModelNames) / sizeof(const char *)))
      UTIL_THROW(FormatLoadException, "The binary file claims to be model type " << static_cast<unsigned int>(params.fixed.model_type) << " but this is not implemented for in this inference code.");
    UTIL_THROW(FormatLoadException, "The binary file was built for " << kModelNames[params.fixed.model_type] << " but the inference code is trying to load " << kModelNames[model_type]);
  }
}

uint8_t *SetupBinary(const Config &config, const Parameters &params, std::size_t memory_size, Backing &backing) {
  const off_t file_size = util::SizeFile(backing.file.get());
  // The header is smaller than a page, so we have to map the whole header as well.  
  std::size_t total_map = TotalHeaderSize(params.counts.size()) + memory_size;
  if (file_size != util::kBadSize && static_cast<uint64_t>(file_size) < total_map)
    UTIL_THROW(FormatLoadException, "Binary file has size " << file_size << " but the headers say it should be at least " << total_map);

  util::MapRead(config.load_method, backing.file.get(), 0, total_map, backing.search);

  if (config.enumerate_vocab && !params.fixed.has_vocabulary)
    UTIL_THROW(FormatLoadException, "The decoder requested all the vocabulary strings, but this binary file does not have them.  You may need to rebuild the binary file with an updated version of build_binary.");

  if (config.enumerate_vocab) {
    if ((off_t)-1 == lseek(backing.file.get(), total_map, SEEK_SET))
      UTIL_THROW(util::ErrnoException, "Failed to seek in binary file to vocab words");
  }
  return reinterpret_cast<uint8_t*>(backing.search.get()) + TotalHeaderSize(params.counts.size());
}

void ComplainAboutARPA(const Config &config, ModelType model_type) {
  if (config.write_mmap || !config.messages) return;
  if (config.arpa_complain == Config::ALL) {
    *config.messages << "Loading the LM will be faster if you build a binary file." << std::endl;
  } else if (config.arpa_complain == Config::EXPENSIVE && model_type == TRIE_SORTED) {
    *config.messages << "Building " << kModelNames[model_type] << " from ARPA is expensive.  Save time by building a binary format." << std::endl;
  }
}

} // namespace detail

bool RecognizeBinary(const char *file, ModelType &recognized) {
  util::scoped_fd fd(util::OpenReadOrThrow(file));
  if (!detail::IsBinaryFormat(fd.get())) return false;
  Parameters params;
  detail::ReadHeader(fd.get(), params);
  recognized = params.fixed.model_type;
  return true;
}

} // namespace ngram
} // namespace lm
