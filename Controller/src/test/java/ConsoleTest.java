import java.io.Console;

public class ConsoleTest {
	public static void main(String[] args) {

		Console console = System.console();
		String theFuck = console.readLine("The fuck? ");
		System.out.println("This is the fuck: " + theFuck);

	}
}
