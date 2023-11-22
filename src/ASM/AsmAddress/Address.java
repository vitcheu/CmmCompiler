package ASM.AsmAddress;

import IR.Constants.Type;

public interface Address {
     default String getOutputStr(){
          return toString();
     }
     int getWidth();
     default boolean isRegister(){
          return false;
     }
     Type getIRType();
     boolean isFloat();

     default boolean isSinglePrecision(){
          return false;
     }
     default boolean isDoublePrecision(){
          return false;
     }
     default boolean isLeftValue(){
          return false;
     }

     default boolean isIndex(){
          return false;
     }
}
