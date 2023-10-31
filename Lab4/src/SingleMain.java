import java.util.Random;

public class SingleMain {
    public static void main(String[] args) {
        int n = 2000;

        Random rand = new Random();

        double[][] A = new double[n][n];
        double[][] B = new double[n][n];
        double[][] C = new double[n][n];

        for(int i = 0; i < n; i++) {
            for(int j = 0; j < n; j++) {
                A[i][j] = rand.nextInt(20);
                B[i][j] = rand.nextInt(20);
            }
        }

        long start = System.currentTimeMillis();

        for(int i = 0; i < n; i++) {
            for(int j = 0; j < n; j++) {
                C[i][j] = 0;
                for(int k = 0; k < n; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }

        long end = System.currentTimeMillis();

        System.out.printf("Estimated time is: %dms", end - start);
    }
}
