
		.const
__str0	byte	'Hello, My Dumb Compiler!', 10, 0
__str1	byte	'arr[%2d]', 9,'=', 9,'%d', 10, 0
NULL	qword	0

		.data

		.code
		externdef	printf:proc
		public		asmMain

_TEXT	SEGMENT
n$ = 96
m$ = 88
a$ = 80
t7$ = 44
t4$ = 48
t2$ = 52
temp$ = 56
v$ = 60
j$ = 64
i$ = 68
quicksort PROC 
;      function prologue   
;      Line 4   :static void quicksort( int[]a , int m, int n)
       sub      rsp    , 72       
;      store r8d to Shadow Space
       mov      dword ptr n$[rsp]    , r8d      
;      store edx to Shadow Space
       mov      dword ptr m$[rsp]    , edx      
;      store rcx to Shadow Space
       mov      qword ptr a$[rsp]    , rcx      
;      function body       
;      Line 9   :if (n <=m) return;                      
       cmp      r8d    , edx      
       jle      L0                
;      Line 11  :i = m;                                  
L1:    mov      dword ptr i$[rsp]    , edx      
;      Line 12  :j = n -1;                               
       mov      eax    , r8d      
       dec      eax               
       mov      dword ptr j$[rsp]    , eax      
;      Line 13  :v = a[n];                               
       mov      r8d    , r8d      
       mov      r9d    , dword ptr[rcx+r8*4] 
       mov      dword ptr t2$[rsp]    , r9d      
       mov      dword ptr v$[rsp]    , r9d      

       mov      r8d    , eax      
       mov      rax    , rcx      
       mov      ecx    , edx      
       movsxd   rdx    , r8d      
       movsxd   rcx    , ecx      
       movsxd   r8    ,  r9d      
;      Line 15  :while (a[i] < v) i++;                   
L3:    mov      r11d    , dword ptr[rax+rcx*4] 
       movsxd   r9    ,  r11d     
       cmp      r11d    , r8d      
       jge      L7                
       inc      ecx               
       jmp      L3                
;      Line 16  :while (a[j] > v) j--;                   
L7:    mov      r11d    , dword ptr[rax+rdx*4] 
       movsxd   r10    , r11d     
       cmp      r11d    , r8d      
       jle      L10               
       dec      edx               
       jmp      L7                
;      Line 18  :if (i >= j)                             
L10:   cmp      ecx    , edx      
       jge      L4                
;      Line 22  :a[i] = a[j];                            
L11:   mov      dword ptr[rax+rcx*4]    , r10d     
;      Line 23  :a[j] = temp;                            
       mov      dword ptr[rax+rdx*4]    , r9d      
       jmp      L3                

;      Line 27  :a[i]=a[n];                              
L4:    mov      r8d    , dword ptr t2$[rsp] 
       mov      dword ptr[rax+rcx*4]    , r8d      
;      Line 28  :a[n]=temp;                              
       movsxd   r8    ,  dword ptr n$[rsp] 
       mov      dword ptr[rax+r8*4]    , r9d      
;      Line 30  :quicksort(a, m, j);                     
       mov      dword ptr i$[rsp]    , ecx      
;      passing the 3th argument
       mov      r8d    , edx      
;      passing the 2th argument
       mov      edx    , dword ptr m$[rsp] 
;      passing the 1th argument
       mov      rcx    , rax      
       call     quicksort          

;      passing the 3th argument
;      Line 31  :quicksort(a, i + 1, n);                 
       mov      r8d    , dword ptr n$[rsp] 
;      passing the 2th argument
       mov      eax    , dword ptr i$[rsp] 
       inc      eax               
       mov      edx    , eax      
;      passing the 1th argument
       mov      rcx    , qword ptr a$[rsp] 
       call     quicksort          

;      function epilogue   
L0:    add      rsp    , 72       
       ret                        
quicksort ENDP 
_TEXT	ENDS


_TEXT	SEGMENT
i$1 = 32
num$ = 36
arr$ = 40
asmMain PROC 
;      function prologue   
;      Line 34  :void asmMain(void){                     
       sub      rsp    , 1064     
;      function body       
;      Line 35  :int num=16;                             
       mov      eax    , 16       
       mov      dword ptr num$[rsp]    , eax      
;      Line 36  :int [256] arr ={0x80000000,4,8,-80,2,10,23,188,-2,45,5,9,102,27,63,1,3};
       mov      dword ptr arr$[rsp]    , 80000000H 
       mov      dword ptr arr$[rsp+4]    , 4        
       mov      dword ptr arr$[rsp+8]    , 8        
       mov      dword ptr arr$[rsp+12]    , -80      
       mov      dword ptr arr$[rsp+16]    , 2        
       mov      dword ptr arr$[rsp+20]    , 10       
       mov      dword ptr arr$[rsp+24]    , 23       
       mov      dword ptr arr$[rsp+28]    , 188      
       mov      dword ptr arr$[rsp+32]    , -2       
       mov      dword ptr arr$[rsp+36]    , 45       
       mov      dword ptr arr$[rsp+40]    , 5        
       mov      dword ptr arr$[rsp+44]    , 9        
       mov      dword ptr arr$[rsp+48]    , 102      
       mov      dword ptr arr$[rsp+52]    , 27       
       mov      dword ptr arr$[rsp+56]    , 63       
       mov      dword ptr arr$[rsp+60]    , 1        
       mov      dword ptr arr$[rsp+64]    , 3        
;      passing the 3th argument
;      Line 37  :quicksort(arr,1,num-1);                 
       mov      r8d    , 15       
;      passing the 2th argument
       mov      edx    , 1        
;      passing the 1th argument
       lea      rcx    , qword ptr arr$[rsp] 
       call     quicksort          

;      passing the 1th argument
;      Line 38  :printf("Hello, My Dumb Compiler!\n");   
       lea      rcx    , __str0   
       call     printf            

;      Line 39  :for(int i=1;i<num;i++){                 
       mov      eax    , 1        
       mov      dword ptr i$1[rsp]    , eax      

       movsxd   rcx    , eax      
       lea      rax    , qword ptr arr$[rsp] 
       mov      edx    , dword ptr num$[rsp] 
       movsxd   rdx    , edx      
       jmp      L16               
;      Line 40  :printf("arr[%2d]\t=\t%d\n",i,arr[i]);   
L15:   mov      dword ptr num$[rsp]    , edx      
;      passing the 3th argument
       mov      ecx    , ecx      
       mov      edx    , dword ptr[rax+rcx*4] 
       mov      r8d    , edx      
;      passing the 2th argument
       mov      edx    , ecx      
;      passing the 1th argument
       lea      rcx    , __str1   
       call     printf            

       mov      eax    , dword ptr i$1[rsp] 
       inc      eax               
       mov      dword ptr i$1[rsp]    , eax      
       movsxd   rcx    , eax      
       lea      rax    , qword ptr arr$[rsp] 
       mov      edx    , dword ptr num$[rsp] 
L16:   cmp      ecx    , edx      
       jl       L15               

;      function epilogue   
       add      rsp    , 1064     
       ret                        
asmMain ENDP 
_TEXT	ENDS

END
