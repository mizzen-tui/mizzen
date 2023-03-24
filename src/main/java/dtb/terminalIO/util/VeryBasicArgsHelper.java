package dtb.terminalIO.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class VeryBasicArgsHelper {

    // returns null if no named arg found
    static String getArg(String[] args, String argName) {
        
        String valueAsStr = null;
        if (args == null || args.length<1 || argName == null) {
            return valueAsStr;
        }
        
        int lengthCheck = args.length - 1;
        
        for (int i = 0; i < lengthCheck; i++) {
            
            if (("-"+argName).equalsIgnoreCase(args[i])) {
                valueAsStr = args[i+1];
                break;
            }
            if (("--"+argName).equalsIgnoreCase(args[i])) {
                valueAsStr = args[i+1];
                break;
            }
            if ((argName).equalsIgnoreCase(args[i])) {
                valueAsStr = args[i+1];
                break;
            }
            
        }
        
        return valueAsStr;
        
    }
    
    static <T> void __onNamedArg(String[] args, String argName, Function<String,T> transform, Consumer<T> argHandler) {
        
        if (args == null || args.length<1 || argName == null) {
            return;
        }
        
        int lengthCheck = args.length - 1;
        
        String valueAsStr = null;
        
        
        for (int i = 0; i < lengthCheck; i++) {
            
            if (("-"+argName).equalsIgnoreCase(args[i])) {
                valueAsStr = args[i+1];
                break;
            }
            if (("--"+argName).equalsIgnoreCase(args[i])) {
                valueAsStr = args[i+1];
                break;
            }
            if ((argName).equalsIgnoreCase(args[i])) {
                valueAsStr = args[i+1];
                break;
            }
            
        }
        
        if (valueAsStr == null) {
            return;
        }
        
        T value = transform.apply(valueAsStr);
        
        if (value == null) {
            return;
        }
        
        argHandler.accept(value);

    }
    
    public static void onNamedIntArg(String[] args, String argNameCaseInsensitive, Consumer<Integer> argHandler) {
        
        Function<String, Integer> transform = (str) -> {
            try {
                return Integer.parseInt(str);  
            } catch (NumberFormatException e) {
                return null;
            }
        };
        
        __onNamedArg(args, argNameCaseInsensitive, transform, argHandler);
    }

    public static void onNamedBooleanArg(String[] args, String argNameCaseInsensitive, Consumer<Boolean> argHandler) {
        
        Function<String, Boolean> transform = (str) -> {
             if (Boolean.parseBoolean(str)) {
                 return true;
             }
             if ("YES".equalsIgnoreCase(str)) {
                 return true;
             }
             if ("Y".equalsIgnoreCase(str)) {
                 return true;
             }
             return false;
        };
        
        __onNamedArg(args, argNameCaseInsensitive, transform, argHandler);
    }

    public static void onNamedArg(String[] args, String argNameCaseInsensitive, Consumer<String> argHandler) {
        Function<String, String> transform = (str) -> {
             return str;
        };
        __onNamedArg(args, argNameCaseInsensitive, transform, argHandler);
    }
    
    public static <T> T getFromArgs(String[] args, String argNameCaseInsensitive, Function<String, T> fromArgValueBuilder) {
        
        String value = getArg(args, argNameCaseInsensitive);
        if (value == null) {
            return null;
        }
        
        T mappedArgToT = fromArgValueBuilder.apply(value);
        
        return mappedArgToT;
    } 
}
