
#include "syscall.h"

#define PageSize 128
#define BIG PageSize*32

char s1[BIG],s2[BIG];

int
main()
{
    char prompt[8];
    int i;

    prompt[0] = 's';
    prompt[1] = 't';
    prompt[2] = 'e';
    prompt[3] = 'p';
    prompt[4] = ' ';
    prompt[5] = '1';
    prompt[6] = '!';
    prompt[7] = '\n';

    TestCase(0);

    for (i = 0; i < BIG; i ++) {
      s1[i]=i/PageSize;
      s2[i]=i/PageSize+64;
    } 
    prompt[0] = 'S';
    Write(prompt,8,ConsoleOutput);

    TestCase(1);

    for (i=0; i<BIG; i++)
	s2[BIG-1-i]=s1[i];
    prompt[5]='2';
    Write(prompt,8,ConsoleOutput);

    TestCase(2);

    for (i=0; i<BIG; i++)
	s1[BIG-i]=s2[BIG-i];
    prompt[5]='3';
    Write(prompt,8,ConsoleOutput);

    TestCase(2);

    for (i=0; i<BIG; i++)
        s1[BIG-i]=s1[i]; 
    prompt[5]='4';
    Write(prompt,8,ConsoleOutput);

    TestCase(2);

    Halt(); 
}

