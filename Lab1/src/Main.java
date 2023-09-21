import mpi.MPI;
import mpi.Status;

public class Main {
    static public void main(String[] args) {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        int TAG = 0;

        int[] message = new int[1];
        message[0] = rank;

        if(rank % 2 == 0) {
            int dst = rank + 1;
            if(dst < size)
                MPI.COMM_WORLD.Send(message, 0, 1, MPI.INT, dst, TAG);
        }
        else {
            int src = rank - 1;
            MPI.COMM_WORLD.Recv(message, 0, 1, MPI.INT, src, TAG);
            System.out.printf("%d: Receive %d from thread %d\n", rank, message[0], src);
        }

        MPI.Finalize();
    }
}
