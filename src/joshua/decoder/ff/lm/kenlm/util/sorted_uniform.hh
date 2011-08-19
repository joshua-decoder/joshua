#ifndef UTIL_SORTED_UNIFORM__
#define UTIL_SORTED_UNIFORM__

#include <algorithm>
#include <cstddef>

#include <assert.h>
#include <inttypes.h>

namespace util {

template <class T> class IdentityAccessor {
  public:
    typedef T Key;
    T operator()(const T *in) const { return *in; }
};

struct Pivot64 {
  static inline std::size_t Calc(uint64_t off, uint64_t range, std::size_t width) {
    std::size_t ret = static_cast<std::size_t>(static_cast<float>(off) / static_cast<float>(range) * static_cast<float>(width));
    // Cap for floating point rounding
    return (ret < width) ? ret : width - 1;
  }
};

// Use when off * width is <2^64.  This is guaranteed when each of them is actually a 32-bit value.   
struct Pivot32 {
  static inline std::size_t Calc(uint64_t off, uint64_t range, uint64_t width) {
    return static_cast<std::size_t>((off * width) / (range + 1));
  }
};

// Usage: PivotSelect<sizeof(DataType)>::T
template <unsigned> struct PivotSelect;
template <> struct PivotSelect<8> { typedef Pivot64 T; };
template <> struct PivotSelect<4> { typedef Pivot32 T; };
template <> struct PivotSelect<2> { typedef Pivot32 T; };

/* Binary search. */
template <class Iterator, class Accessor> bool BinaryFind(
    const Accessor &accessor,
    Iterator begin,
    Iterator end,
    const typename Accessor::Key key, Iterator &out) {
  while (end > begin) {
    Iterator pivot(begin + (end - begin) / 2);
    typename Accessor::Key mid(accessor(pivot));
    if (mid < key) {
      begin = pivot + 1;
    } else if (mid > key) {
      end = pivot;
    } else {
      out = pivot;
      return true;
    }
  }
  return false;
}

// Search the range [before_it + 1, after_it - 1] for key.  
// Preconditions:
// before_v <= key <= after_v
// before_v <= all values in the range [before_it + 1, after_it - 1] <= after_v
// range is sorted.
template <class Iterator, class Accessor, class Pivot> bool BoundedSortedUniformFind(
    const Accessor &accessor,
    Iterator before_it, typename Accessor::Key before_v,
    Iterator after_it, typename Accessor::Key after_v,
    const typename Accessor::Key key, Iterator &out) {
  while (after_it - before_it > 1) {
    Iterator pivot(before_it + (1 + Pivot::Calc(key - before_v, after_v - before_v, after_it - before_it - 1)));
    typename Accessor::Key mid(accessor(pivot));
    if (mid < key) {
      before_it = pivot;
      before_v = mid;
    } else if (mid > key) {
      after_it = pivot;
      after_v = mid;
    } else {
      out = pivot;
      return true;
    }
  }
  return false;
}

template <class Iterator, class Accessor, class Pivot> bool SortedUniformFind(const Accessor &accessor, Iterator begin, Iterator end, const typename Accessor::Key key, Iterator &out) {
  if (begin == end) return false;
  typename Accessor::Key below(accessor(begin));
  if (key <= below) {
    if (key == below) { out = begin; return true; }
    return false;
  }
  // Make the range [begin, end].  
  --end;
  typename Accessor::Key above(accessor(end));
  if (key >= above) {
    if (key == above) { out = end; return true; }
    return false;
  }
  return BoundedSortedUniformFind<Iterator, Accessor, Pivot>(accessor, begin, below, end, above, key, out);
}

// May return begin - 1.
template <class Iterator, class Accessor> Iterator BinaryBelow(
    const Accessor &accessor,
    Iterator begin,
    Iterator end,
    const typename Accessor::Key key) {
  while (end > begin) {
    Iterator pivot(begin + (end - begin) / 2);
    typename Accessor::Key mid(accessor(pivot));
    if (mid < key) {
      begin = pivot + 1;
    } else if (mid > key) {
      end = pivot;
    } else {
      for (++pivot; (pivot < end) && accessor(pivot) == mid; ++pivot) {}
      return pivot - 1;
    }
  }
  return begin - 1;
}

// To use this template, you need to define a Pivot function to match Key.  
template <class PackingT> class SortedUniformMap {
  public:
    typedef PackingT Packing;
    typedef typename Packing::ConstIterator ConstIterator;
    typedef typename Packing::MutableIterator MutableIterator;

    struct Accessor {
      public:
        typedef typename Packing::Key Key;
        const Key &operator()(const ConstIterator &i) const { return i->GetKey(); }
        Key &operator()(const MutableIterator &i) const { return i->GetKey(); }
    };

    // Offer consistent API with probing hash.
    static std::size_t Size(std::size_t entries, float /*ignore*/ = 0.0) {
      return sizeof(uint64_t) + entries * Packing::kBytes;
    }

    SortedUniformMap() 
#ifdef DEBUG
      : initialized_(false), loaded_(false) 
#endif
    {}

    SortedUniformMap(void *start, std::size_t /*allocated*/) : 
      begin_(Packing::FromVoid(reinterpret_cast<uint64_t*>(start) + 1)),
      end_(begin_), size_ptr_(reinterpret_cast<uint64_t*>(start)) 
#ifdef DEBUG
      , initialized_(true), loaded_(false) 
#endif
      {}

    void LoadedBinary() {
#ifdef DEBUG
      assert(initialized_);
      assert(!loaded_);
      loaded_ = true;
#endif
      // Restore the size.  
      end_ = begin_ + *size_ptr_;
    }

    // Caller responsible for not exceeding specified size.  Do not call after FinishedInserting.  
    template <class T> void Insert(const T &t) {
#ifdef DEBUG
      assert(initialized_);
      assert(!loaded_);
#endif
      *end_ = t;
      ++end_;
    }

    void FinishedInserting() {
#ifdef DEBUG
      assert(initialized_);
      assert(!loaded_);
      loaded_ = true;
#endif
      std::sort(begin_, end_);
      *size_ptr_ = (end_ - begin_);
    }

    // Don't use this to change the key.
    template <class Key> bool UnsafeMutableFind(const Key key, MutableIterator &out) {
#ifdef DEBUG
      assert(initialized_);
      assert(loaded_);
#endif
      return SortedUniformFind<MutableIterator, Accessor, Pivot64>(begin_, end_, key, out);
    }

    // Do not call before FinishedInserting.  
    template <class Key> bool Find(const Key key, ConstIterator &out) const {
#ifdef DEBUG
      assert(initialized_);
      assert(loaded_);
#endif
      return SortedUniformFind<ConstIterator, Accessor, Pivot64>(Accessor(), ConstIterator(begin_), ConstIterator(end_), key, out);
    }

    ConstIterator begin() const { return begin_; }
    ConstIterator end() const { return end_; }

  private:
    typename Packing::MutableIterator begin_, end_;
    uint64_t *size_ptr_;
#ifdef DEBUG
    bool initialized_;
    bool loaded_;
#endif
};

} // namespace util

#endif // UTIL_SORTED_UNIFORM__
