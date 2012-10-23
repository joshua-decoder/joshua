#include "lm/search_hashed.hh"
#include "lm/value.hh"
#include <iostream>

int main() {
  using namespace lm::ngram;
  std::cout << "Probability entry " << sizeof(detail::ProbEntry) << std::endl;
  std::cout << "Backoff lower " << sizeof(BackoffValue::ProbingEntry) << std::endl;
  std::cout << "Rest lower " << sizeof(RestValue::ProbingEntry) << std::endl;
  std::vector<uint64_t> counts;
  counts.push_back(1567052);
  counts.push_back(38563409);
  counts.push_back(40161180);
  counts.push_back(55001632);
  counts.push_back(56501848);
  Config config;
  std::cout << "Backoff Hashed::Size " << detail::HashedSearch<BackoffValue>::Size(counts, config) << std::endl;
  std::cout << "Rest Hashed::Size " << detail::HashedSearch<RestValue>::Size(counts, config) << std::endl;
}
