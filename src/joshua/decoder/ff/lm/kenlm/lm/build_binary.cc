#include "lm/model.hh"
#include "util/file_piece.hh"

#include <cstdlib>
#include <exception>
#include <iostream>
#include <iomanip>

#include <math.h>
#include <stdlib.h>
#include <unistd.h>

namespace lm {
namespace ngram {
namespace {

void Usage(const char *name) {
  std::cerr << "Usage: " << name << " [-u log10_unknown_probability] [-s] [-n] [-p probing_multiplier] [-t trie_temporary] [-m trie_building_megabytes] [type] input.arpa output.mmap\n\n"
"-u sets the default log10 probability for <unk> if the ARPA file does not have\n"
"one.\n"
"-s allows models to be built even if they do not have <s> and </s>.\n"
"-i allows buggy models from IRSTLM by mapping positive log probability to 0.\n"
"type is one of probing, trie, or sorted:\n\n"
"probing uses a probing hash table.  It is the fastest but uses the most memory.\n"
"-p sets the space multiplier and must be >1.0.  The default is 1.5.\n\n"
"trie is a straightforward trie with bit-level packing.  It uses the least\n"
"memory and is still faster than SRI or IRST.  Building the trie format uses an\n"
"on-disk sort to save memory.\n"
"-t is the temporary directory prefix.  Default is the output file name.\n"
"-m limits memory use for sorting.  Measured in MB.  Default is 1024MB.\n\n"
/*"sorted is like probing but uses a sorted uniform map instead of a hash table.\n"
"It uses more memory than trie and is also slower, so there's no real reason to\n"
"use it.\n\n"*/
"See http://kheafield.com/code/kenlm/benchmark/ for data structure benchmarks.\n"
"Passing only an input file will print memory usage of each data structure.\n"
"If the ARPA file does not have <unk>, -u sets <unk>'s probability; default 0.0.\n";
  exit(1);
}

// I could really use boost::lexical_cast right about now.  
float ParseFloat(const char *from) {
  char *end;
  float ret = strtod(from, &end);
  if (*end) throw util::ParseNumberException(from);
  return ret;
}
unsigned long int ParseUInt(const char *from) {
  char *end;
  unsigned long int ret = strtoul(from, &end, 10);
  if (*end) throw util::ParseNumberException(from);
  return ret;
}

void ShowSizes(const char *file, const lm::ngram::Config &config) {
  std::vector<uint64_t> counts;
  util::FilePiece f(file);
  lm::ReadARPACounts(f, counts);
  std::size_t probing_size = ProbingModel::Size(counts, config);
  // probing is always largest so use it to determine number of columns.  
  long int length = std::max<long int>(5, lrint(ceil(log10(probing_size))));
  std::cout << "Memory estimate:\ntype    ";
  // right align bytes.  
  for (long int i = 0; i < length - 5; ++i) std::cout << ' ';
  std::cout << "bytes\n"
    "probing " << std::setw(length) << probing_size << " assuming -p " << config.probing_multiplier << "\n"
    "trie    " << std::setw(length) << TrieModel::Size(counts, config) << "\n";
}

} // namespace ngram
} // namespace lm
} // namespace

int main(int argc, char *argv[]) {
  using namespace lm::ngram;

  try {
    lm::ngram::Config config;
    int opt;
    while ((opt = getopt(argc, argv, "siu:p:t:m:")) != -1) {
      switch(opt) {
        case 'u':
          config.unknown_missing_logprob = ParseFloat(optarg);
          break;
        case 'p':
          config.probing_multiplier = ParseFloat(optarg);
          break;
        case 't':
          config.temporary_directory_prefix = optarg;
          break;
        case 'm':
          config.building_memory = ParseUInt(optarg) * 1048576;
          break;
        case 's':
          config.sentence_marker_missing = lm::SILENT;
          break;
        case 'i':
          config.positive_log_probability = lm::SILENT;
          break;
        default:
          Usage(argv[0]);
      }
    }
    if (optind + 1 == argc) {
      ShowSizes(argv[optind], config);
    } else if (optind + 2 == argc) {
      config.write_mmap = argv[optind + 1];
      ProbingModel(argv[optind], config);
    } else if (optind + 3 == argc) {
      const char *model_type = argv[optind];
      const char *from_file = argv[optind + 1];
      config.write_mmap = argv[optind + 2];
      if (!strcmp(model_type, "probing")) {
        ProbingModel(from_file, config);
      } else if (!strcmp(model_type, "trie")) {
        TrieModel(from_file, config);
      } else {
        Usage(argv[0]);
      }
    } else {
      Usage(argv[0]);
    }
  }
  catch (std::exception &e) {
    std::cerr << e.what() << std::endl;
    abort();
  }
  return 0;
}
