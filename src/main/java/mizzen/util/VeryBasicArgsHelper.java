package mizzen.util;

import java.util.ArrayList;
import java.util.List;
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
    
    /**
     * All values of a named arg that may be repeated, e.g. "IMAGE a.gif IMAGE b.gif" -> [a.gif, b.gif].
     * Same name matching as the other methods (name, -name, --name, case insensitive).
     * Returns an empty list if not found.
     */
    public static List<String> getAllArgs(String[] args, String argNameCaseInsensitive) {
        List<String> values = new ArrayList<>();
        if (args == null || argNameCaseInsensitive == null) {
            return values;
        }
        for (int i = 0; i < args.length - 1; i++) {
            if (isArgName(args[i], argNameCaseInsensitive)) {
                values.add(args[++i]);
            }
        }
        return values;
    }

    /** true if arg is argName, -argName or --argName (case insensitive) */
    public static boolean isArgName(String arg, String argNameCaseInsensitive) {
        return argNameCaseInsensitive.equalsIgnoreCase(arg)
            || ("-" + argNameCaseInsensitive).equalsIgnoreCase(arg)
            || ("--" + argNameCaseInsensitive).equalsIgnoreCase(arg);
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
