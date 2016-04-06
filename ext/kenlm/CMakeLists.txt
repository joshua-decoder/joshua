cmake_minimum_required(VERSION 2.8.8)
#
# The KenLM cmake files make use of add_library(... OBJECTS ...)
# 
# This syntax allows grouping of source files when compiling
# (effectively creating "fake" libraries based on source subdirs).
# 
# This syntax was only added in cmake version 2.8.8
#
# see http://www.cmake.org/Wiki/CMake/Tutorials/Object_Library


# This CMake file was created by Lane Schwartz <dowobeha@gmail.com>


# Define a single cmake project
project(kenlm)


# Compile all executables into bin/
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_BINARY_DIR}/bin)

# Compile all libraries into lib/
set(LIBRARY_OUTPUT_PATH ${PROJECT_BINARY_DIR}/lib)

# Add our CMake helper functions
include(cmake/KenLMFunctions.cmake)

# And our helper modules
list(APPEND CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake/modules)

# Tell cmake that we want unit tests to be compiled
include(CTest)
enable_testing()


## See: http://stackoverflow.com/questions/2418157/ubuntu-linux-c-error-undefined-reference-to-clock-gettime-and-clock-settim
# Note that we should add this before other libraries (Boost), otherwise it doesn't work
# Lib RT ("Real Time") is a library that is automatically added with newer compilers, but not with old ones,
# and therefore should be added
# TODO: Add test for compiler version
find_library(RT_LIBRARY rt) 
message(">>>RT_LIBRARY: " ${RT_LIBRARY})
link_libraries(${RT_LIBRARY}) 


# Note this link: http://stackoverflow.com/questions/13767699/boost-test-undefined-reference-errors
# was essential in fixing problems with static/dynamic linking of the boost unit test library

## Gideon : static linking
## SEE : http://stackoverflow.com/questions/11680168/how-can-i-optionally-link-against-static-or-dynamic-boost-library-using-cmake
# We need to avoid using a mix of static and dynamic boost libraries, which happens if we are not very careful, 
# since CMAke prefers dynamic libraries for some perverse reason


if(DEFINED ENV{STATIC_BOOST})
    message(STATUS "STATIC_BOOST environment variable defined")
    string( TOLOWER $ENV{STATIC_BOOST} USE_STATIC_BOOST_LOWER ) ## Used for robustness so that both "YES" and "yes" will work
    message(STATUS ">>"  ${USE_STATIC_BOOST_LOWER} "<<")  
    if(${USE_STATIC_BOOST_LOWER} STREQUAL "yes")
        option(USE_STATIC_BOOST "Build with static BOOST libraries instead of dynamic" YES)
    endif()
else(DEFINED ENV{STATIC_BOOST})
     message(AUTHOR_WARNING "STATIC_BOOST environment variable not defined, please define it. Valid values for the variable are 'YES' or 'NO.'\n
    Assume dynamic boost by default. If you need static, you must set the environment variable, otherwise kenlm will fail to compile.
    Use 'export STATIC_BOOST=YES' to set the variable before running 'ant kenlm'")       
   option(USE_STATIC_BOOST "Build with static BOOST libraries instead of dynamic" NO)
endif()

message(STATUS ">>>>>>> VALUE: " $ENV{STATIC_BOOST})
set(Boost_USE_STATIC_LIBS ${USE_STATIC_BOOST})
set(Boost_USE_MULTITHREADED ON)
unset(Boost_INCLUDE_DIR CACHE)
unset(Boost_LIBRARY_DIRS CACHE)


# We need boost
find_package(Boost 1.36.0 REQUIRED COMPONENTS
  program_options 
  system
  thread
  unit_test_framework
)

if(USE_STATIC_BOOST)
   message(STATUS "Linking against boost static libraries")
else(USE_STATIC_BOOST)
   message(STATUS "Linking against boost dynamic libraries")
endif(USE_STATIC_BOOST)

# We need zlib
# See: http://stackoverflow.com/questions/6173915/need-help-trying-to-make-cmake-find-third-party-libraries
find_package( ZLIB REQUIRED )
if ( ZLIB_FOUND) 
    message("I found zlib for you")
    link_libraries(${ZLIB_LIBRARIES})
    message("Boost Library Dirs: ${Boost_LIBRARY_DIRS}")
    message("Boost Libraries: ${Boost_LIBRARIES}")
    link_libraries(${Boost_LIBRARIES}) 
endif( ZLIB_FOUND )

# Define where include files live
include_directories(
  ${PROJECT_SOURCE_DIR} 
  ${Boost_INCLUDE_DIRS}
  ${ZLIB_INCLUDE_DIRS} # ZLIB needs to be included as well 
)


option(BUILD_INTERPOLATE "Build language model interpolation code" OFF)

# Process subdirectories
add_subdirectory(util)
add_subdirectory(lm)

message("Output path: "  ${EXECUTABLE_OUTPUT_PATH}) 
message("Project binary dir:" ${PROJECT_BINARY_DIR})



