#include <stdio.h>
#include <stdlib.h>

#include "appwrapper.h"

int main(int argc, char **argv) {
    graal_isolate_t *isolate = NULL;
    graal_isolatethread_t *thread = NULL;
    
    if (graal_create_isolate(NULL, &isolate, &thread) != 0) {
        fprintf(stderr, "initialization error\n");
        return 1;
    }
    
    int result = run_main(argc, argv);
    
    graal_tear_down_isolate(thread);
    return result;
}
