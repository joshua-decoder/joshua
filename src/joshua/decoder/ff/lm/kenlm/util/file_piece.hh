#ifndef UTIL_FILE_PIECE__
#define UTIL_FILE_PIECE__

#include "util/ersatz_progress.hh"
#include "util/exception.hh"
#include "util/have.hh"
#include "util/mmap.hh"
#include "util/scoped.hh"
#include "util/string_piece.hh"

#include <string>

#include <cstddef>

namespace util {

class EndOfFileException : public Exception {
  public:
    EndOfFileException() throw();
    ~EndOfFileException() throw();
};

class ParseNumberException : public Exception {
  public:
    explicit ParseNumberException(StringPiece value) throw();
    ~ParseNumberException() throw() {}
};

class GZException : public Exception {
  public:
    explicit GZException(void *file);
    GZException() throw() {}
    ~GZException() throw() {}
};

int OpenReadOrThrow(const char *name);

extern const bool kSpaces[256];

// Return value for SizeFile when it can't size properly.  
const off_t kBadSize = -1;
off_t SizeFile(int fd);

// Memory backing the returned StringPiece may vanish on the next call.  
class FilePiece {
  public:
    // 32 MB default.
    explicit FilePiece(const char *file, std::ostream *show_progress = NULL, off_t min_buffer = 33554432) throw(GZException);
    // Takes ownership of fd.  name is used for messages.  
    explicit FilePiece(int fd, const char *name, std::ostream *show_progress = NULL, off_t min_buffer = 33554432) throw(GZException);

    ~FilePiece();
     
    char get() throw(GZException, EndOfFileException) { 
      if (position_ == position_end_) {
        Shift();
        if (at_end_) throw EndOfFileException();
      }
      return *(position_++);
    }

    // Leaves the delimiter, if any, to be returned by get().  Delimiters defined by isspace().  
    StringPiece ReadDelimited(const bool *delim = kSpaces) throw(GZException, EndOfFileException) {
      SkipSpaces(delim);
      return Consume(FindDelimiterOrEOF(delim));
    }

    // Unlike ReadDelimited, this includes leading spaces and consumes the delimiter.
    // It is similar to getline in that way.  
    StringPiece ReadLine(char delim = '\n') throw(GZException, EndOfFileException);

    float ReadFloat() throw(GZException, EndOfFileException, ParseNumberException);
    double ReadDouble() throw(GZException, EndOfFileException, ParseNumberException);
    long int ReadLong() throw(GZException, EndOfFileException, ParseNumberException);
    unsigned long int ReadULong() throw(GZException, EndOfFileException, ParseNumberException);

    // Skip spaces defined by isspace.  
    void SkipSpaces(const bool *delim = kSpaces) throw (GZException, EndOfFileException) {
      for (; ; ++position_) {
        if (position_ == position_end_) Shift();
        if (!delim[static_cast<unsigned char>(*position_)]) return;
      }
    }

    off_t Offset() const {
      return position_ - data_.begin() + mapped_offset_;
    }

    const std::string &FileName() const { return file_name_; }
    
  private:
    void Initialize(const char *name, std::ostream *show_progress, off_t min_buffer) throw(GZException);

    template <class T> T ReadNumber() throw(GZException, EndOfFileException, ParseNumberException);

    StringPiece Consume(const char *to) {
      StringPiece ret(position_, to - position_);
      position_ = to;
      return ret;
    }

    const char *FindDelimiterOrEOF(const bool *delim = kSpaces) throw (GZException, EndOfFileException);

    void Shift() throw (EndOfFileException, GZException);
    // Backends to Shift().
    void MMapShift(off_t desired_begin) throw ();

    void TransitionToRead() throw (GZException);
    void ReadShift() throw (GZException, EndOfFileException);

    const char *position_, *last_space_, *position_end_;

    scoped_fd file_;
    const off_t total_size_;
    const off_t page_;

    size_t default_map_size_;
    off_t mapped_offset_;

    // Order matters: file_ should always be destroyed after this.
    scoped_memory data_;

    bool at_end_;
    bool fallback_to_read_;

    ErsatzProgress progress_;

    std::string file_name_;

#ifdef HAVE_ZLIB
    void *gz_file_;
#endif // HAVE_ZLIB
};

} // namespace util

#endif // UTIL_FILE_PIECE__
