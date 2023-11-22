
		.const
__str0	byte	'x=%d,arr[j]=%d,z=%d', 10, 0
__str1	byte	'p1[0]=%d,p1[1]=%d', 10, 0
__str2	byte	'p1[1]=%d', 10, 0
__str3	byte	'p1=%p,brr=%p,arr=%p', 10, 10, 10, 0
__str4	byte	'x=%d,brr[j]=%d,z=%d,y=%d', 10, 10, 0
__str5	byte	'Hello Compiler', 0
__str6	byte	'st.p=%p,st.p[0]=%d', 10, 0
__str7	byte	'hello', 0
__str8	byte	'world', 0
__f0	real4	2.7
NULL	qword	0

		.data

		.code
		externdef	printf:proc
		public		asmMain

_TEXT	SEGMENT
t15$ = 48
j$ = 72
i$ = 76
z$ = 80
y$ = 84
x$ = 88
e$ = 92
d$ = 96
c$ = 100
b$ = 104
a$ = 108
str1$ = 112
p2$ = 120
p1$ = 128
brr$ = 136
arr$ = 144
str2$ = 156
st$ = 176
asmMain PROC 
;      function prologue   
;      Line 3   :int asmMain(void){                      
       sub      rsp    , 232      
       mov      qword ptr[rsp+56]    , rsi      
       mov      qword ptr[rsp+64]    , rdi      
;      function body       
;      Line 13  :int x,y=27,z;                           
       mov      eax    , 27       
       mov      dword ptr y$[rsp]    , eax      
;      Line 14  :int i=1,j=2;                            
       mov      ecx    , 1        
       mov      dword ptr i$[rsp]    , ecx      
       mov      edx    , 2        
       mov      dword ptr j$[rsp]    , edx      
;      Line 15  :int[3] arr = { 10,20,30 };              
       mov      dword ptr[r8]    , 10       
       mov      dword ptr[r8+4]    , 20       
       mov      dword ptr[r8+8]    , 30       
;      Line 16  :a=arr[i];                               
       mov      r9d    , dword ptr[r8+4] 
       mov      dword ptr a$[rsp]    , r9d      
;      Line 18  :arr[j]=y+x;                             
       mov      r8d    , r9d      
       add      r8d    , 27       
       mov      dword ptr[r10+8]    , r8d      
;      passing the 4th argument
;      passing the 3th argument
;      passing the 2th argument
;      Line 20  :printf("x=%d,arr[j]=%d,z=%d\n",x,arr[j],z);
       mov      edx    , r9d      
;      passing the 1th argument
       lea      rcx    , __str0   
       call     printf            

;      Line 22  :int* brr=0+arr,p1,p2;                   
       lea      rax    , qword ptr arr$[rsp] 
       mov      qword ptr brr$[rsp]    , rax      
;      Line 23  :z=brr[i]+a;                             
       mov      ecx    , dword ptr i$[rsp] 
       mov      ecx    , ecx      
       mov      edx    , dword ptr[rax+rcx*4] 
       mov      r8d    , dword ptr a$[rsp] 
       add      r8d    , edx      
;      Line 26  :brr[i]=-y+z;                            
       mov      r9d    , dword ptr y$[rsp] 
       neg      r9d               
       add      r9d    , r8d      
       mov      dword ptr[rax+rcx*4]    , r9d      
;      Line 27  :arr[j]=-y+z;                            
       mov      r8d    , dword ptr j$[rsp] 
       mov      r8d    , r8d      
       mov      dword ptr[rax+r8*4]    , r9d      
;      Line 29  :z=-y+z+brr[i];                          
       add      r9d    , edx      
;      Line 30  :p1=brr+1;                               
       mov      r10    , rax      
       add      r10    , 4        
       mov      qword ptr p1$[rsp]    , r10      
;      Line 31  :y=p1[0]+z+arr[i];                       
       mov      r11d    , dword ptr[r10] 
       add      r9d    , r11d     
       add      r9d    , edx      
       mov      dword ptr y$[rsp]    , r9d      
;      Line 33  :p1[1]=3;                                
       mov      dword ptr[r10+4]    , 3        
;      Line 34  :printf("p1[0]=%d,p1[1]=%d\n",p1[0],p1[1]);
       mov      qword ptr t15$[rsp]    , rax      
;      passing the 3th argument
       mov      eax    , dword ptr[r10+4] 
       mov      r8d    , eax      
;      passing the 2th argument
       mov      edx    , r11d     
;      passing the 1th argument
       lea      rcx    , __str1   
       call     printf            

;      passing the 2th argument
;      Line 35  :printf("p1[1]=%d\n",p1[1]);             
       mov      rax    , qword ptr p1$[rsp] 
       mov      ecx    , dword ptr[rax+4] 
       mov      edx    , ecx      
;      passing the 1th argument
       lea      rcx    , __str2   
       call     printf            

;      passing the 4th argument
;      Line 36  :printf("p1=%p,brr=%p,arr=%p\n\n\n",p1,brr,arr);
       mov      r9    ,  qword ptr t15$[rsp] 
;      passing the 3th argument
       mov      r8    ,  qword ptr brr$[rsp] 
;      passing the 2th argument
       mov      rdx    , qword ptr p1$[rsp] 
;      passing the 1th argument
       lea      rcx    , __str3   
       call     printf            

;      Line 38  :z=y+p1[0];                              
       mov      rax    , qword ptr p1$[rsp] 
       mov      ecx    , dword ptr[rax] 
       mov      eax    , dword ptr y$[rsp] 
       add      eax    , ecx      
;      Line 39  :x=brr[i];                               
       mov      ecx    , dword ptr i$[rsp] 
       mov      rdx    , qword ptr brr$[rsp] 
       mov      r8d    , dword ptr[rdx+rcx*4] 
;      Line 40  :z=brr[i]+z-2;                           
       add      eax    , r8d      
       sub      eax    , 2        
;      passing the 5th argument
;      Line 45  :printf("x=%d,brr[j]=%d,z=%d,y=%d\n\n",x++,brr[j],++z,y=z+1);
       inc      eax               
       mov      dword ptr[rsp+32]    , eax      
;      passing the 4th argument
       mov      r9d    , eax      
;      passing the 3th argument
       mov      eax    , dword ptr j$[rsp] 
       mov      eax    , eax      
       mov      ecx    , dword ptr[rdx+rax*4] 
       mov      eax    , r8d      
       mov      r8d    , ecx      
;      passing the 2th argument
       mov      edx    , eax      
;      passing the 1th argument
       lea      rcx    , __str4   
       call     printf            

;      Line 62  :struct st st = { 3,2.7,"Hello Compiler"};
       mov      dword ptr st$[rsp]    , 3        
       movss    xmm0    , __f0     
       movss    real4 ptr st$[rsp+4]    , xmm0     
       lea      rsi    , __str5   
       lea      rdi    , qword ptr st$[rsp+8] 
       mov      rcx    , 15       
       rep movsb                   
;      Line 67  :st.p=arr;                               
       mov      rax    , qword ptr t15$[rsp] 
       mov      qword ptr st$[rsp+40]    , rax      
;      Line 68  :st.a=(st.p)[0];                         
       mov      ecx    , dword ptr[rax] 
       mov      dword ptr st$[rsp]    , ecx      
;      passing the 3th argument
;      Line 73  :printf("st.p=%p,st.p[0]=%d\n",st.p,(st.p)[0]);
       mov      r8d    , ecx      
;      passing the 2th argument
       mov      rdx    , rax      
;      passing the 1th argument
       lea      rcx    , __str6   
       call     printf            

;      Line 77  :return 0;                               
       xor      eax    , eax      
;      function epilogue   
       mov      rdi    , qword ptr[rsp+64] 
       mov      rsi    , qword ptr[rsp+56] 
       add      rsp    , 232      
       ret                        
asmMain ENDP 
_TEXT	ENDS

END
