OBJS = GDAOptimization.o HCOptimization.o Problem.o \
            IterOptimization.o ProblemTest.o RRTOptimization.o \
            MYOptimization.o SAOptimization.o TAOptimization.o \
            Optimization.o KategProblemTest.o KategProblemKBC.o \
            KategProblemWBC.o KategProblem.o StatVar.o general.o \
            mkcls.o

CFLAGS = $(CFLAGS_GLOBAL) -Wall -W -DNDEBUG -O3 -funroll-loops -std=c++11

.cpp.o:
	$(CXX) $(CFLAGS) -c $< -o $@

LDFLAGS =

mkcls: $(OBJS) 
	$(CXX) $(CFLAGS) -o mkcls $(OBJS) $(LDFLAGS)

remove clean:
	-rm -f *.o mkcls




