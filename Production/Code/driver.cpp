#include <stdio.h>
extern "C"
{
    // int printf(char* format, ...);
    int M = 1;
    int N = 5;
    void asmMain(void);
}
int main(void)
{
    // char[256] split="---------------------------\n";
    printf("Calling asmMain:\n"
                "----------------------------\n\n"
                "\033[35;3m");
    asmMain();
    printf("\n\033[0m----------------------------\n"
                "Returned from asmMain.\n");
}