#include "util/exception.hh"

#ifdef __GXX_RTTI
#include <typeinfo>
#endif

#include <errno.h>
#include <string.h>

namespace util {

Exception::Exception() throw() {}
Exception::~Exception() throw() {}

Exception::Exception(const Exception &from) : std::exception() {
  stream_ << from.stream_.str();
}

Exception &Exception::operator=(const Exception &from) {
  stream_ << from.stream_.str();
  return *this;
}

const char *Exception::what() const throw() {
  text_ = stream_.str();
  return text_.c_str();
}

void Exception::SetLocation(const char *file, unsigned int line, const char *func, const char *child_name, const char *condition) {
  /* The child class might have set some text, but we want this to come first.
   * Another option would be passing this information to the constructor, but
   * then child classes would have to accept constructor arguments and pass
   * them down.  
   */
  text_ = stream_.str();
  stream_.str("");
  stream_ << file << ':' << line;
  if (func) stream_ << " in " << func << " threw ";
  if (child_name) {
    stream_ << child_name;
  } else {
#ifdef __GXX_RTTI
    stream_ << typeid(this).name();
#else
    stream_ << "an exception";
#endif
  }
  if (condition) stream_ << " because `" << condition;
  stream_ << "'.\n";
  stream_ << text_;
}

namespace {
// The XOPEN version.
const char *HandleStrerror(int ret, const char *buf) {
  if (!ret) return buf;
  return NULL;
}

// The GNU version.
const char *HandleStrerror(const char *ret, const char * /*buf*/) {
  return ret;
}
} // namespace

ErrnoException::ErrnoException() throw() : errno_(errno) {
  char buf[200];
  buf[0] = 0;
#ifdef sun
  const char *add = strerror(errno);
#else
  const char *add = HandleStrerror(strerror_r(errno, buf, 200), buf);
#endif

  if (add) {
    *this << add << ' ';
  }
}

ErrnoException::~ErrnoException() throw() {}

} // namespace util
