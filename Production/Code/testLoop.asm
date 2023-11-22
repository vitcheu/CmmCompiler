
		.const
__str0	byte	'hello', 10, 0
__str1	byte	'i=%d,x%d', 10,',a=%p', 0
__str2	byte	'hello,out of loops', 10, 0
__str3	byte	'a[0][i]=%d', 10, 0
NULL	qword	0

		.data

		.code
		externdef	printf:proc
		public		asmMain

_TEXT	SEGMENT
j$2 = 44
x$1 = 48
i$ = 52
a$ = 56
asmMain PROC 
;      function prologue   
;      Line 3   :void asmMain(void){                     
       sub      rsp    , 456      
;      function body       
;      Line 6   :for(i=0;i<10;i++){                      
       xor      eax    , eax      
       mov      dword ptr i$[rsp]    , eax      

       mov      ecx    , eax      
       lea      rax    , qword ptr a$[rsp] 
       mov      edx    , dword ptr j$2[rsp] 
       movsxd   rdx    , edx      
       movsxd   rcx    , ecx      
       mov      r8d    , dword ptr x$1[rsp] 
       movsxd   r8    ,  r8d      
       jmp      L4                
;      Line 7   :int x=1;                                
L3:    mov      r8d    , 1        
;      Line 8   :for(int j=0;j<i;j++){                   
       xor      edx    , edx      
       jmp      L9                
;      Line 9   :a[i][j]=0;                              
L8:    mov      r9d    , ecx      
       imul     r9d    , 40       
       mov      r10d    , edx      
       imul     r10d    , 4        
       add      r9d    , r10d     
       mov      r9d    , r9d      
       mov      dword ptr[rax+r9]    , 0        
;      Line 10  :x=a[i][j];                              
       xor      r8d    , r8d      
       inc      edx               
L9:    cmp      edx    , ecx      
       jl       L8                
;      Line 14  :if (a[0][i] > 10)                       
L7:    mov      r9d    , dword ptr[rax+rcx*4] 
       cmp      r9d    , 10       
       jg       L13               
;      Line 16  :x++;                                    
L14:   inc      r8d               
       mov      dword ptr x$1[rsp]    , r8d      
;      Line 17  :i++;                                    
       inc      ecx               
       mov      dword ptr i$[rsp]    , ecx      
;      Line 18  :printf("hello\n");                      
       mov      dword ptr j$2[rsp]    , edx      
;      passing the 1th argument
       lea      rcx    , __str0   
       call     printf            

;      passing the 4th argument
;      Line 19  :printf("i=%d,x%d\n,a=%p", i, x,a);      
       lea      r9    ,  qword ptr a$[rsp] 
;      passing the 3th argument
       mov      r8d    , dword ptr x$1[rsp] 
;      passing the 2th argument
       mov      edx    , dword ptr i$[rsp] 
;      passing the 1th argument
       lea      rcx    , __str1   
       call     printf            

       lea      rax    , qword ptr a$[rsp] 
       mov      edx    , dword ptr j$2[rsp] 
       mov      ecx    , dword ptr i$[rsp] 
       mov      r8d    , dword ptr x$1[rsp] 
       jmp      L7                
;      Line 21  :i++;                                    
L13:   inc      ecx               
       inc      ecx               
L4:    cmp      ecx    , 10       
       jl       L3                

;      Line 24  :printf("hello,out of loops\n");         
       mov      dword ptr i$[rsp]    , ecx      
;      passing the 1th argument
       lea      rcx    , __str2   
       call     printf            

;      passing the 2th argument
;      Line 25  :printf("a[0][i]=%d\n",a[0][i]);         
       mov      eax    , dword ptr i$[rsp] 
       mov      eax    , eax      
       mov      ecx    , dword ptr a$[rsp+rax*4] 
       mov      edx    , ecx      
;      passing the 1th argument
       lea      rcx    , __str3   
       call     printf            

;      function epilogue   
       add      rsp    , 456      
       ret                        
asmMain ENDP 
_TEXT	ENDS

END
