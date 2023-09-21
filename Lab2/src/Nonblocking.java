import mpi.MPI;
import mpi.Status;

public class Nonblocking {
    public static void main(String[] args) {
        MPI.Init(args);

        int TAG = 0;
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        int[] buf = { 0 };

        if(rank == 0) {
            int src = size - 1;
            int dst = rank + 1;
            Status sendStatus = MPI.COMM_WORLD.Isend(buf, 0, 1, MPI.INT, dst, TAG).Wait();
            Status recvStatus = MPI.COMM_WORLD.Irecv(buf, 0, 1, MPI.INT, src, TAG).Wait();
            System.out.println("Sum is " + buf[0]);
        }
        else {
            int src = rank - 1;
            int dst = (rank + 1) % size;
            Status recvStatus = MPI.COMM_WORLD.Irecv(buf, 0, 1, MPI.INT, src, TAG).Wait();
            buf[0] += rank;
            Status sendStatus = MPI.COMM_WORLD.Isend(buf, 0, 1, MPI.INT, dst, TAG).Wait();
        }

        MPI.Finalize();
    }
}
