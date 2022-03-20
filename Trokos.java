import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

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

			out.close();
			in.close();

			clientSocket.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}
}
