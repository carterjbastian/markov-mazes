/*
 * FAIR WARNING:
 * this is a grim maze generator. It can create all sorts of whacky things.
 * Check your maze out before using it.
 * There is no guaruntee of a solution.
 * There is no guaruntee of any challenge.
 * Your parameters will barely be checked.
 * This script can be broken and made to do unexpected things.
 * No one should ever use this ever (except for me).
 * It's just a jimmy-rigged, off-the-cuff solution
 */
#include <stdio.h>
#include <time.h>
#include <stdlib.h>

int main(int argc, char *argv[]) {
  int dim = 0;
  if (argc < 2)
    return -1;
  else
    dim = atoi(argv[1]);

  if (dim == 0)
    return -1;

  // Seed the PRNG
  srand((unsigned) time(NULL));

  FILE *out = fopen("output.maz", "w");
  unsigned int random = 0;
  // Generate the ASCII Maze (w/ approx 1/4 of nodes being walls)
  for (int i = 0; i < dim; i++) {
    for (int j = 0; j < dim; j++) {
      random = (unsigned int) rand();
      if (random % 4 == 1)
        fprintf(out, "#");
      else
        fprintf(out, ".");
    }
    fprintf(out, "\n");
  }

      
}
