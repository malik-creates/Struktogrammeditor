import java.util.Scanner;

public class Ostern {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Bitte Jahreszahl eingeben: ");
        int x = scanner.nextInt();

        int r = x - 1900;
        int s = r % 19;
        int a = ((7 * s) + 1) / 19;
        int t = ((11 * s) + 4 - a) % 29;
        int b = r / 4;
        int u = ((r + b + 31 - t)) % 7;
        int o = 25 - t - u;

        if (o <= 0) {
            int tag = 31 + o;
            System.out.println("Ostern ist am " + tag + ". März");
        } else {
            System.out.println("Ostern ist am " + o + ". April");
        }
    }
}
