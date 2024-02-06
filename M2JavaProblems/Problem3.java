package M2JavaProblems;
import java.util.Arrays;

public class Problem3 {
    public static void main(String[] args) {
        Integer[] a1 = new Integer[]{-1, -2, -3, -4, -5, -6, -7, -8, -9, -10};
        Integer[] a2 = new Integer[]{-1, 1, -2, 2, 3, -3, -4, 5};
        Double[] a3 = new Double[]{-0.01, -0.0001, -0.15};
        String[] a4 = new String[]{"-1", "2", "-3", "4", "-5", "5", "-6", "6", "-7", "7"};
        
        bePositive(a1);
        bePositive(a2);
        bePositive(a3);
        bePositive(a4);
    }

    static <T> void bePositive(T[] arr) {
        System.out.println("Processing Array:" + Arrays.toString(arr));

        // Create an array to store the output
        Object[] output = new Object[arr.length];

        // Convert each value to positive and store in the output array
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] instanceof Number) {
                // If it's a number, use Math.abs to get the absolute value
                output[i] = Math.abs(((Number) arr[i]).doubleValue());
            } else if (arr[i] instanceof String) {
                // If it's a string, parse it to a number and then get the absolute value
                try {
                    output[i] = Math.abs(Double.parseDouble((String) arr[i]));
                } catch (NumberFormatException e) {
                    // Handle invalid string format
                    output[i] = arr[i];
                }
            } else {
                // If it's neither a number nor a string, keep the original value
                output[i] = arr[i];
            }
        }

        // Build the result string
        StringBuilder sb = new StringBuilder();
        for (Object i : output) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(String.format("%s (%s)", i, i.getClass().getSimpleName().substring(0, 1)));
        }

        System.out.println("Result: " + sb.toString());
    }
}
// owe 2/5/2024