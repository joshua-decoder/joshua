#ifndef UTIL_PROBING_HASH_TABLE__
#define UTIL_PROBING_HASH_TABLE__

#include "util/exception.hh"

#include <algorithm>
#include <cstddef>
#include <functional>

#include <assert.h>

namespace util {

/* Thrown when table grows too large */
class ProbingSizeException : public Exception {
  public:
    ProbingSizeException() throw() {}
    ~ProbingSizeException() throw() {}
};

/* Non-standard hash table
 * Buckets must be set at the beginning and must be greater than maximum number
 * of elements, else an infinite loop happens.
 * Memory management and initialization is externalized to make it easier to
 * serialize these to disk and load them quickly.
 * Uses linear probing to find value.
 * Only insert and lookup operations.  
 */

template <class PackingT, class HashT, class EqualT = std::equal_to<typename PackingT::Key> > class ProbingHashTable {
  public:
    typedef PackingT Packing;
    typedef typename Packing::Key Key;
    typedef typename Packing::MutableIterator MutableIterator;
    typedef typename Packing::ConstIterator ConstIterator;

    typedef HashT Hash;
    typedef EqualT Equal;

    static std::size_t Size(std::size_t entries, float multiplier) {
      return std::max(entries + 1, static_cast<std::size_t>(multiplier * static_cast<float>(entries))) * Packing::kBytes;
    }

    // Must be assigned to later.  
    ProbingHashTable() : entries_(0)
#ifdef DEBUG
      , initialized_(false)
#endif
    {}

    ProbingHashTable(void *start, std::size_t allocated, const Key &invalid = Key(), const Hash &hash_func = Hash(), const Equal &equal_func = Equal())
      : begin_(Packing::FromVoid(start)),
        buckets_(allocated / Packing::kBytes),
        end_(begin_ + (allocated / Packing::kBytes)),
        invalid_(invalid),
        hash_(hash_func),
        equal_(equal_func),
        entries_(0)
#ifdef DEBUG
        , initialized_(true),
#endif
    {}

    template <class T> void Insert(const T &t) {
      if (++entries_ >= buckets_)
        UTIL_THROW(ProbingSizeException, "Hash table with " << buckets_ << " buckets is full.");
#ifdef DEBUG
      assert(initialized_);
#endif
      for (MutableIterator i(begin_ + (hash_(t.GetKey()) % buckets_));;) {
        if (equal_(i->GetKey(), invalid_)) { *i = t; return; }
        if (++i == end_) { i = begin_; }
      }
    }

    void FinishedInserting() {}

    void LoadedBinary() {}

    // Don't change anything related to GetKey,  
    template <class Key> bool UnsafeMutableFind(const Key key, MutableIterator &out) {
      for (MutableIterator i(begin_ + (hash_(key) % buckets_));;) {
        Key got(i->GetKey());
        if (equal_(got, key)) { out = i; return true; }
        if (equal_(got, invalid_)) return false;
        if (++i == end_) i = begin_;
      }    
    }

    template <class Key> bool Find(const Key key, ConstIterator &out) const {
#ifdef DEBUG
      assert(initialized_);
#endif
      for (ConstIterator i(begin_ + (hash_(key) % buckets_));;) {
        Key got(i->GetKey());
        if (equal_(got, key)) { out = i; return true; }
        if (equal_(got, invalid_)) return false;
        if (++i == end_) i = begin_;
      }    
    }

  private:
    MutableIterator begin_;
    std::size_t buckets_;
    MutableIterator end_;
    Key invalid_;
    Hash hash_;
    Equal equal_;
    std::size_t entries_;
#ifdef DEBUG
    bool initialized_;
#endif
};

} // namespace util

#endif // UTIL_PROBING_HASH_TABLE__
