extern int prinrf(char* f, ...);

void quicksort( int a[], int m, int n)
{
    // printf("m=%d,n=%d\n\n",m,n);
    // printf("a[0]=%d\n",a[0]);
    int i, j;
    int v;
    if (n <= m + 1) return;
    /* 片段由此开始 */
    i = m;
    j = n - 1;
    v = a[n];
    // printf("v=%d\n",v);
    // printf("a[i]=%d,a[j]=%d,a[n]=%d\n",a[i],a[j],a[n]);
    // // // int turn=0;
    while (1) {
        // //     if(turn>10)
        //         return;
            // printf("i=%d,j=%d,v=%d\n");
        while (a[i] < v) i++;
        while (a[j] > v) j--;
        if (i >= j)
            break;
        /* 交换a[i]和a[j] */
        int temp = a[i];
        a[i] = a[j];
        a[j] = temp;
        // turn++;
    }
    // /* 片段结束 */
    // quicksort(a, m, j);
    // quicksort(a, i + 1, n);
}

void asmMain(void)
{
    int arr [256] = { 4,8,2,1,3 };
    quicksort(arr, 0, 4);
}