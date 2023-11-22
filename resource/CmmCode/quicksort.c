import stdio;
// extern int prinrf(char* f,...);/*  */

static void quicksort( int[]a , int m, int n)
{
    int i, j;
    int v;
    int temp;
    if (n <=m) return;
    /* 片段由此开始 */
    i = m;
    j = n -1;
    v = a[n];
    while (1) {
        while (a[i] < v) i++;
        while (a[j] > v) j--;
    
        if (i >= j)
            break;
        /* 交换a[i]和a[j] */
        temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }
    /* 交换a[i]和a[n] */
    temp=a[i];
    a[i]=a[n];
    a[n]=temp;
    // /* 片段结束 */
    quicksort(a, m, j);
    quicksort(a, i + 1, n);
}

void asmMain(void){
    int num=16;
    int [256] arr ={0x80000000,4,8,-80,2,10,23,188,-2,45,5,9,102,27,63,1,3};
    quicksort(arr,1,num-1); 
    printf("Hello, My Dumb Compiler!\n");
    for(int i=1;i<num;i++){
        printf("arr[%2d]\t=\t%d\n",i,arr[i]);
    }
}