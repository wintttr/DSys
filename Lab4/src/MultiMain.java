import mpi.MPI;
import mpi.Request;
import mpi.Status;
import java.util.Random;

import static java.lang.Math.abs;

public class MultiMain {
    private static int getNToSend(int rank, int n, int actualSize) {
        int nToSend;
        if(rank == actualSize) {
            nToSend = n - (actualSize - 1) * (n / actualSize);
        }
        else {
            nToSend = n / actualSize;
        }

        return nToSend;
    }

    private static int getLowerBound(int rank, int n, int nToSend, int actualSize) {
        int a = (rank - 1) * nToSend;

        if(rank == actualSize) {
            a = n - nToSend;
        }

        return a;
    }

    private static int getNextRank(int rank, int actualSize) {
        return 1 + (rank % actualSize);
    }

    private static int getPreviousRank(int rank, int actualSize){
        // Придумать что-нибудь лучше
        rank--;
        if(rank == 0)
            return actualSize;
        else
            return rank;
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
        int xchgTag = 3;
        int AnswerTag = 4;

        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if(rank == 0) {
            Random rand = new Random();
            int n = 2000;

            int actualSize = size - 1;

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
            for (int i = 1; i < size; i++) {
                int[] msg = { n };
                MPI.COMM_WORLD.Send(msg, 0, 1, MPI.INT, i, nTag);
            }

            // Отправляем A и B нужными порциями кому надо
            for (int i = 1; i <= actualSize; i++) {
                int nToSend = getNToSend(i, n, actualSize);

                int a = getLowerBound(i, n, nToSend, actualSize);
                int b = a + nToSend;

                // Отправляем A
                double[][] aMsg = new double[nToSend][n];

                for (int j = a; j < b; j++) {
                    for (int k = 0; k < n; k++) {
                        aMsg[j - a][k] = A[j][k];
                    }
                }

                MPI.COMM_WORLD.Send(aMsg, 0, nToSend, MPI.OBJECT, i, ATag);

                // Отправляем B
                double[][] bMsg = new double[n][nToSend];

                for (int j = 0; j < n; j++) {
                    for (int k = a; k < b; k++) {
                        bMsg[j][k - a] = B[j][k];
                    }
                }

                MPI.COMM_WORLD.Send(bMsg, 0, n, MPI.OBJECT, i, BTag);
            }

            // Составляем итоговую матрицу
            Request[] requests = new Request[actualSize];

            for (int i = 1; i <= actualSize; i++) {
                int nToSend = getNToSend(i, n, actualSize);
                int a = getLowerBound(i, n, nToSend, actualSize);
                requests[i - 1] = MPI.COMM_WORLD.Irecv(C, a, nToSend, MPI.OBJECT, i, AnswerTag);
            }

            Request.Waitall(requests);

            long end = System.currentTimeMillis();

            System.out.println("Estimated time is: " + (end - start) + "ms");
        }
        else {
            // Получаем размерность
            int[] msg = new int[1];
            MPI.COMM_WORLD.Recv(msg, 0, 1, MPI.INT, 0, nTag);
            int n = msg[0];

            int actualSize = size - 1;

            if(n < actualSize) {
                actualSize = n;
            }

            // Если rank > n, то выйти
            if(rank <= n) {
                // Получаем A
                Status st = MPI.COMM_WORLD.Probe(0, ATag);
                int nToSend = st.count;

                double[][] A = new double[nToSend][];
                MPI.COMM_WORLD.Recv(A, 0, nToSend, MPI.OBJECT, 0, ATag);

                // Получаем B
                double[][] B = new double[n][];
                MPI.COMM_WORLD.Recv(B, 0, n, MPI.OBJECT, 0, BTag);

                double[][] sendMsg = new double[nToSend][n];

                int aSize = nToSend;
                int a = getLowerBound(rank, n, nToSend, actualSize);
                for(int u = 1; u <= actualSize; u++) {
                    int bSize = B[0].length;

                    for (int i = 0; i < aSize; i++) {
                        for (int j = 0; j < bSize; j++) {
                            sendMsg[i][a + j] = 0;
                            for (int k = 0; k < n; k++) {
                                sendMsg[i][a + j] += A[i][k] * B[k][j];
                            }
                        }
                    }

                    double[][] newB = new double[n][];
                    int nextRank = getNextRank(rank, actualSize);
                    int prevRank = getPreviousRank(rank, actualSize);
                    MPI.COMM_WORLD.Sendrecv_replace(B, 0, n, MPI.OBJECT, nextRank, xchgTag, prevRank, xchgTag);

                    int r = rank - u;
                    if(r <= 0) {
                        r = actualSize - abs(r);
                    }

                    nToSend = getNToSend(r, n, actualSize);
                    a = getLowerBound(r, n, nToSend, actualSize);
                }

                MPI.COMM_WORLD.Send(sendMsg, 0, aSize, MPI.OBJECT, 0, AnswerTag);
            }
        }

        MPI.Finalize();
    }
}
