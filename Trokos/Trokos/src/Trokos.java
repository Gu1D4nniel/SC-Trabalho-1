import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/*Como usar
 * java Trokos <serverAddress> <userID> [password]
 * por omissao o porto eh 45678 
 */
public class Trokos {

	public static void main(String[] args) throws ClassNotFoundException {
		Socket clientSocket = null;
		String hostname = "";
		int port = 0;

		if (args.length != 3) {
			System.out.println("Insira os dados corretamente");
			return;
		}

		// fazer aqui arranjo dos dados
		String dadosServer = args[0];
		if (dadosServer.contains((":"))) {
			String[] dados = dadosServer.split((":"));
			hostname = dados[0];
			port = Integer.parseInt(dados[1]);
		} else {
			port = 45678;
		}

		String user = args[1];
		String passwd = args[2];

		// conexao com o server
		try {
			clientSocket = new Socket(hostname, port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		try {
			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

			Scanner sc = new Scanner(System.in);

			String dados = user + ":" + passwd;

			byte[] bDados = new byte[dados.length()];

			// converte os tamanhos dos ararys em bytes
			byte sizeU = (byte) args[1].length();
			byte sizeP = (byte) args[2].length();

			// envia o tamanho do username e da password ao servidor
			out.writeObject(sizeU);
			out.writeObject(sizeP);

			// converte os dados em bytes e poe no buffer
			char[] dadosArray = dados.toCharArray();
			int i = 0;
			for (char c : dadosArray) {
				bDados[i] = (byte) c;
				i++;
			}

			out.write(bDados, 0, bDados.length);
			out.flush();
			String respostaLogin = (String) in.readObject();
			System.out.println(respostaLogin);

			// Mudar por um enum(?)
			if (respostaLogin.equals("Utilizador autenticado")) {
				String ops = (String) in.readObject();
				System.out.println(ops);

				System.out.println("Escreva o que deseja fazer: ");
				String respostaOp = sc.nextLine();
				System.out.println();

				out.writeObject(respostaOp);
				out.flush();

				String respostaPedido = (String) in.readObject();

				if (respostaOp.contains("balance") || respostaOp.charAt(0) == 'b') {

					System.out.println("O seu balance e: " + respostaPedido);
				} else if (respostaOp.contains("makepayment") || respostaOp.charAt(0) == 'm') {

					System.out.println(respostaPedido);
				} else if (respostaOp.contains("requestpayment") || respostaOp.charAt(0) == 'r') {
					System.out.println(respostaPedido);
				} else if (respostaOp.contains("viewrequests") || respostaOp.charAt(0) == 'v') {
					System.out.println(respostaPedido);
				} else if (respostaOp.contains("payrequest") || respostaOp.charAt(0) == 'p') {
					System.out.println(respostaPedido);
				} else if (respostaOp.contains("newgroup") || respostaOp.charAt(0) == 'n') {

					System.out.println(respostaPedido);
				} else if (respostaOp.contains("addu") || respostaOp.charAt(0) == 'a') {

					System.out.println(respostaPedido);
				}
			}

			out.close();
			in.close();

			clientSocket.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}
}
