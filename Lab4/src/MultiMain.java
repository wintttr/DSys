import mpi.MPI;
import mpi.Request;
import mpi.Status;
import java.util.Random;

public class MultiMain {
    private static int getNToSend(int rank, int n, int actualSize) {
        int nToSend;
        if(rank == actualSize) {
            nToSend = n - (actualSize - 1) * (n / (actualSize - 1));
        }
        else {
            nToSend = n / (actualSize - 1);
        }

        return nToSend;
    }

    public static void printMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.printf("%.0f ", matrix[i][j]);
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        int nTag = 0;
        int BTag = 1;
        int ATag = 2;
        int AnswerTag = 3;

        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if(rank == 0) {
            int n = 2000;

            int actualSize = size - 1;

            Random rand = new Random();

            if(n < actualSize) {
                actualSize = n;
            }

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

            // Отправляем n всем потокам
            for(int i = 1; i < size; i++) {
                int[] msg = { n };
                MPI.COMM_WORLD.Send(msg, 0, 1, MPI.INT, i, nTag);
            }

            // Отправляем B только тем потокам, которым надо
            for(int i = 1; i <= actualSize; i++) {
                MPI.COMM_WORLD.Send(B, 0, n, MPI.OBJECT, i, BTag);
            }

            // Отправляем A нужными порциями кому надо
            for(int i = 1; i <= actualSize; i++) {
                int nToSend = getNToSend(i, n, actualSize);

                int a = (i - 1) * nToSend;

                // КОСТЫЛЬ
                if(i == actualSize) {
                    a = n - nToSend;
                }

                int b = a + nToSend;

                double[][] msg = new double[nToSend][n];

                for(int j = a; j < b; j++) {
                    for(int k = 0; k < n; k++) {
                        msg[j - a][k] = A[j][k];
                    }
                }

                MPI.COMM_WORLD.Send(msg, 0, nToSend, MPI.OBJECT, i, ATag);
            }

            // Составляем итоговую матрицу
            Request[] requests = new Request[actualSize];

            for(int i = 1; i <= actualSize; i++) {
                int nToSend = getNToSend(i, n, actualSize);
                int a = (i - 1) * nToSend;

                // КОСТЫЛЬ
                if(i == actualSize) {
                    a = n - nToSend;
                }

                requests[i - 1] = MPI.COMM_WORLD.Irecv(C, a, nToSend, MPI.OBJECT, i, AnswerTag);
            }

            Request.Waitall(requests);

            long end = System.currentTimeMillis();

            System.out.printf("Estimated time is: %dms", end - start);
        }
        else {
            // Получаем размерность
            int[] msg = new int[1];
            MPI.COMM_WORLD.Recv(msg, 0, 1, MPI.INT, 0, nTag);
            int n = msg[0];

            // Если rank > n, то выйти
            if(rank <= n) {
                // Получаем B
                double[][] B = new double[n][n];
                MPI.COMM_WORLD.Recv(B, 0, n, MPI.OBJECT, 0, BTag);

                // Получаем A
                Status st = MPI.COMM_WORLD.Probe(0, ATag);
                int nToSend = st.count;

                double[][] A = new double[nToSend][n];
                MPI.COMM_WORLD.Recv(A, 0, nToSend, MPI.OBJECT, 0, ATag);

                double[][] sendMsg = new double[nToSend][n];
                for (int i = 0; i < nToSend; i++) {
                    for (int j = 0; j < n; j++) {
                        sendMsg[i][j] = 0;
                        for (int k = 0; k < n; k++) {
                            sendMsg[i][j] += A[i][k] * B[k][j];
                        }
                    }
                }

                MPI.COMM_WORLD.Send(sendMsg, 0, nToSend, MPI.OBJECT, 0, AnswerTag);
            }
        }

        MPI.Finalize();
    }
}
