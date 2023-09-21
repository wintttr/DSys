import mpi.MPI;

public class Blocking {
    public static void main(String[] args) {
        MPI.Init(args);

        int TAG = 0;
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        int[] buf = { 0 };

        if(rank == 0) {
            int src = size - 1;
            int dst = rank + 1;
            MPI.COMM_WORLD.Sendrecv_replace(buf, 0, 1, MPI.INT, dst, TAG, src, TAG);
            System.out.println("Sum is " + buf[0]);
        }
        else {
            int src = rank - 1;
            int dst = (rank + 1) % size;
            MPI.COMM_WORLD.Recv(buf, 0, 1, MPI.INT, src, TAG);
            buf[0] += rank;
            MPI.COMM_WORLD.Send(buf, 0, 1, MPI.INT, dst, TAG);
        }

        MPI.Finalize();
    }
}
