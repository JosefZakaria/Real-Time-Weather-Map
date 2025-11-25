package MainProgram;

public class Main {
    public static void main(String[] args) {
        try {
            new Controller();
        } catch (Exception e) {
            System.err.println("Error when starting application" + e.getMessage());
            e.printStackTrace();
        }
    }
}
