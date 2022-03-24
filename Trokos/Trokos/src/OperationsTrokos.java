import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class OperationsTrokos implements Operations {

	/**
	 * metodo que devolve o balanço de um dado user
	 * 
	 * @param user utilizador que fez o pedido balance
	 * @return o balanço da conta de um dado user
	 * @throws IOException
	 */
	public float balance(String user) throws IOException {
		File file = new File("dataUsers/" + user + ".txt");
		float balance = 0;

		FileInputStream fisUser = new FileInputStream(file);
		InputStream ipsUser = new BufferedInputStream(fisUser);

		int i = 0;
		String dataUser = "";
		while ((i = ipsUser.read()) != -1) {
			dataUser += (char) i;
		}

		String[] lines = dataUser.split("\n");
		for (String l : lines) {
			if (l.contains("Balance")) {
				String[] data = l.split(":");
				balance = Float.parseFloat(data[1]);

			}
		}
		ipsUser.close();
		return balance;

	}

	/**
	 * metodo que envia dinheiro de um user para outro
	 * 
	 * @param user    user que esta a enviar dinheiro
	 * @param destino user que vai receber o dinheiro
	 * @param valor   quantidade de dinheiro
	 * @return -2 se o user e o destino forem iguais, -1 se o user nao tiver saldo,
	 *         0 se o destino nao existir na db, 1 se o pagamento foi feito com
	 *         sucesso
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public int makepayment(String user, String destino, float valor) throws IOException {
		Scanner sc = new Scanner(new File("db.txt"));

		// procura o destino na db
		int check = 0;
		while ((sc.hasNextLine())) {
			String input = sc.nextLine();

			if (input.contains(destino.trim())) {
				check = 1;
			}
		} // Nao existe o destino
		if (check == 0) {
			return check;
		} // User e destino sao iguais
		if (user.equals(destino)) {
			check = -2;
			return check;

		}

		// File do user que envia dinheiro
		File file = new File("dataUsers/" + user + ".txt");

		FileInputStream fisUser = new FileInputStream(file);
		InputStream ipsUser = new BufferedInputStream(fisUser);

		// Le o ficheiro e transforma em string
		int i;
		String dataUser = "";
		while ((i = ipsUser.read()) != -1) {
			dataUser += (char) i;
		}
		ipsUser.close();
		// Divide as linhas
		String[] lines = dataUser.split("\n");

		// procura a linha que contem o balance
		for (String l : lines) {
			if (l.contains("Balance")) {
				String[] data = l.split(":");
				// Se o valor a enviar foir maior que o saldo da erro
				if (Float.parseFloat(data[1]) - valor < 0) {
					return -1;
				} else {
					// Abre o outStream para atualizar os dados dos ficheiros
					FileOutputStream fin = new FileOutputStream(file, false);
					OutputStream ops = new BufferedOutputStream(fin);
					float valorFinal = Float.parseFloat(data[1]) - valor;
					data[1] = String.valueOf(valorFinal);
					byte[] testeB = (data[0] + ":" + data[1]).getBytes();

					ops.write(testeB);
					ops.write("\n".getBytes());

					for (int x = 1; x < lines.length; x++) {
						ops.write(lines[x].getBytes());
					}

					ops.flush();
					ops.close();

				}
			}
		}

		// Ficheiro destino
		File fileDestino = new File("dataUsers/" + destino + ".txt");

		FileInputStream fisDestino = new FileInputStream(fileDestino);
		InputStream ipsDestino = new BufferedInputStream(fisDestino);

		// Le o ficheiro destino e transforma em string
		int j;
		String dataDestino = "";
		while ((j = ipsDestino.read()) != -1) {
			dataDestino += (char) i;
		}
		ipsDestino.close();

		// Divide as linhas
		String[] linesDestino = dataUser.split("\n");

		// Procura a line com o balance
		for (String l : linesDestino) {
			if (l.contains("Balance")) {
				String[] data = l.split(":");

				// Abre um output stream para atualizar o ficheiro
				FileOutputStream fin = new FileOutputStream(fileDestino, false);
				OutputStream ops = new BufferedOutputStream(fin);
				float valorFinal = Float.parseFloat(data[1]) + valor;
				data[1] = String.valueOf(valorFinal);
				byte[] testeB = (data[0] + ":" + data[1]).getBytes();

				ops.write(testeB);
				ops.write("\n".getBytes());
				for (int x = 1; x < lines.length; x++) {
					ops.write(lines[x].getBytes());
				}
				ops.flush();
				ops.close();

			}
		}

		sc.close();
		return 1;
	}

	/**
	 * Faz um pedido de pagamento a um certo utilizador
	 * 
	 * @param user    utilizador que faz o pedido
	 * @param destino utilizador a quem vai ser feito o pedido
	 * @param valor   valor a ser pedido
	 * @return 0 se o utilizador na existir, 1 se o pedido foi feito com sucesso
	 * @throws IOException
	 */
	public int requestpayment(String user, String destino, float valor) throws IOException {

		Scanner sc = new Scanner(new File("db.txt"));

		int check = 0;
		while ((sc.hasNextLine())) {
			String input = sc.nextLine();

			if (input.contains(destino.trim())) {
				check = 1;
			}
		} // Nao existe o destino
		if (check == 0) {
			return check;
		}

		File file = new File("requests/" + destino + ".txt");

		UUID uuid = UUID.randomUUID();
		String[] split = uuid.toString().split("-");
		String id = split[0];
		String line = "";

		FileOutputStream fin = new FileOutputStream(file, true);
		OutputStream ops = new BufferedOutputStream(fin);

		line += (id.toString() + "/" + user + "/" + String.valueOf(valor) + "\n");
		ops.write(line.getBytes());
		ops.flush();
		ops.close();
		return check;

	}

	public String viewrequests(String user) throws IOException {

		if (!new File("requests/" + user + ".txt").exists()) {

			// nao existe este grupo
			return "Este utilizador nao tem pedidos";
		}

		File file = new File("requests/" + user + ".txt");

		FileInputStream fis = new FileInputStream(file);
		InputStream ips = new BufferedInputStream(fis);
		String data = "";
		int i;
		while ((i = ips.read()) != -1) {
			data += (char) i;
		}
		if (data.length() == 0 || data == null) {
			ips.close();
			return null;
		}

		String resposta = "id/user/value:\n\n".toUpperCase();
		resposta += data;
		ips.close();
		return resposta;
	}

	/**
	 * Metodo que cria um novo grupo
	 * 
	 * @param user      dono do grupo
	 * @param nomeGrupo id do grupo
	 * @return 0 se o grupo ja existir, 1 se o grupo nao existir
	 * @throws IOException
	 */
	public int criaGrupo(String user, String nomeGrupo) throws IOException {
		File file = new File("groups/" + nomeGrupo + ".txt");

		if ((file.isFile() && file.exists())) {

			return 0;
		}

		FileOutputStream fin = new FileOutputStream(file, false);
		OutputStream ops = new BufferedOutputStream(fin);

		ops.write(("Owner:" + user + "\n").getBytes());
		ops.write("Members:".getBytes());
		ops.flush();
		ops.close();

		addToDataUser(user, nomeGrupo, true);

		return 1;

	}

	/**
	 * FALTA ADICIONAR UM METODO PARA VERIFICAR SE O NOVO MEMBRO ESTA NA DB
	 * 
	 * Metodo que adiciona um novo membro ao grupo
	 * 
	 * @param owner     dono do grupo
	 * @param user      membro que ira ser adicionado
	 * @param nomeGrupo id do grupo ao qual vai ser adicionado um novo membro
	 * @return 0 se o grupo nao existir, -1 se nao for a owner a adicionar pessoas,
	 *         -2 se a pessoa a adicionar ja estiver no grupo, 1 se tudo correr bem
	 * @throws IOException
	 */
	public int adicionaAoGrupo(String owner, String user, String nomeGrupo) throws IOException {

		if (!new File("groups/" + nomeGrupo + ".txt").exists()) {

			// nao existe este grupo
			return 0;
		}

		File file = new File("groups/" + nomeGrupo + ".txt");
		FileInputStream fis = new FileInputStream(file);
		InputStream ips = new BufferedInputStream(fis);

		String data = "";
		int i;
		while ((i = ips.read()) != -1) {
			data += (char) i;
		}

		String[] lines = data.split("\n");

		// verificar se eh o owner a fazer o pedido
		for (String l : lines) {

			if (l.contains("Owner")) {
				String[] ownerData = l.split(":");
				if (!ownerData[1].equals(owner)) {
					ips.close();
					// so o owner pode adicionar membros
					return -1;
				}
			}

		}

		for (String l : lines) {

			if (l.contains("Members")) {
				String[] listMembers = l.split(":");
				FileOutputStream fin = new FileOutputStream(file, true);
				OutputStream ops = new BufferedOutputStream(fin);
				if (listMembers.length == 1) {

					ops.write((user + ",").getBytes());
					ops.flush();
					ops.close();
					addToDataUser(user, nomeGrupo, false);
					// membro adicionado com sucesso
					return 1;
				}

				String[] members = listMembers[1].split(",");
				for (String m : members) {
					if (m.equals(user)) {
						ops.close();
						// este membro ja faz parte do grupo
						return -2;
					} else {
						ops.write((user + ",").getBytes());
						ops.flush();
						ops.close();
						// membro adicionado com sucesso

						addToDataUser(user, nomeGrupo, false);

					}
				}
				ops.flush();
				ops.close();
			}

		}

		ips.close();

		return 1;
	}

	/**
	 * Metodo que faz update da informaçao acerca dos grupos dos ficheiros no
	 * dataUsers
	 * 
	 * @param user      user que vai sofrer update
	 * @param nomeGrupo nome do grupo que vai ser inserido no ficheiro do user
	 * @param owner     true se for owner, false se for apenas membro
	 * @throws IOException
	 */
	private void addToDataUser(String user, String nomeGrupo, boolean owner) throws IOException {
		// Adiciona ao seu user file o novo grupo a que aderiu
		File fileUser = new File("dataUsers/" + user + ".txt");
		FileInputStream fis = new FileInputStream(fileUser);
		InputStream ips = new BufferedInputStream(fis);

		String data = "";
		int j;
		while ((j = ips.read()) != -1) {
			data += (char) j;
		}

		String[] lines = data.split("\n");
		ips.close();

		FileOutputStream fin = new FileOutputStream(fileUser, false);
		OutputStream ops = new BufferedOutputStream(fin);

		if (owner) {

			String newLine = lines[1].trim() + nomeGrupo + ",";

			ops.write((lines[0] + "\n").getBytes());
			ops.write((newLine + "\n").getBytes());
			ops.write((lines[2] + "\n").getBytes());
			ops.flush();
			ops.close();
		} else {
			String newLine = lines[2].trim() + nomeGrupo + ",";

			ops.write((lines[0] + "\n").getBytes());
			ops.write((lines[1] + "\n").getBytes());
			ops.write((newLine + "\n").getBytes());
			ops.flush();
			ops.close();
		}
	}

	/**
	 * Metodo que faz o pagamento de um pedido
	 * 
	 * @param id   id do pedido
	 * @param user membro que vai enviar o dinheiro
	 * @return 1 se o pagamento for feito com sucesso, 0 user nao tem saldo
	 *         suficiente, -1 se nao existir este id
	 * 
	 * @throws IOException
	 */
	public int payrequest(String id, String user) throws IOException {
		File file = new File("requests/" + user + ".txt");

		FileInputStream fis = new FileInputStream(file);
		InputStream ips = new BufferedInputStream(fis);

		String data = "";
		int j;
		while ((j = ips.read()) != -1) {
			data += (char) j;
		}

		String[] lines = data.split("\n");
		String[] payment = new String[3];
		ips.close();

		FileOutputStream fin = new FileOutputStream(file, false);
		OutputStream ops = new BufferedOutputStream(fin);

		int check = 0;

		for (String l : lines) {
			if (l.contains(id)) {

				payment = l.split("/");
				String destino = payment[1];
				float valor = Float.parseFloat(payment[2]);

				check = makepayment(user, destino, valor);

				if (balance(user) - valor < 0) {
					check = 0;
				} else {
					l = "";
				}

			} else {
				check = -1;
			}

			ops.write((l + "\n").getBytes());

		}

		// SE NAO TIVER SALDO APAGA A LINHA CORRIGIR
		ops.flush();
		ops.close();

		return check;
	}

	public int dividepayment(String owner, String nomeGrupo, float valor) throws IOException {
		File fileGrupo = new File("groups/" + nomeGrupo + ".txt");

		FileInputStream fis = new FileInputStream(fileGrupo);
		InputStream ips = new BufferedInputStream(fis);

		String data = "";
		int j;
		while ((j = ips.read()) != -1) {
			data += (char) j;
		}
		String[] lines = data.split(data);

		// Colocar aqui os users que vao receber o pedido para pagars
		List<String> listUsers = new ArrayList<>();

		for (String l : lines) {

			if (l.contains("Owner")) {
				String[] lineOwner = l.split(":");
				if (!lineOwner[1].equals(owner)) {
					// O owner deste grupo nao coincide com o user que fez o pedido
					return 0;
				} else {
					listUsers.add(lineOwner[1]);

				}
			} else {
				String[] lineMembers = l.split(":");
				String membersString = lineMembers[1];
				String[] members = membersString.split(",");

				for (String m : members) {
					listUsers.add(m);
				}

			}

			for (String u : listUsers) {
				System.out.println(u);
			}
		}
		return 0;
	}

	/**
	 * Metodo que devolve uma lista com os membros do grupo que ainda nao pagaram o
	 * pedido
	 * 
	 * @param user      utilizador que pediu para ver o pedido
	 * @param nomeGrupo id do grupo
	 * @return erro ou uma lista com os membros do grupo que ainda nao pagaram
	 * @throws IOException
	 */
	public String statuspayment(String user, String nomeGrupo) throws IOException {
		return null;

	}

	// Criar um id para o qrcode que vai ser usado na operacao confirm qrcode

	public String obtainQRcode(String user, float valor) throws WriterException, IOException {

		UUID uuid = UUID.randomUUID();
		String[] split = uuid.toString().split("-");
		String id = split[0];
		String valorS = String.valueOf(valor);

		String cabecalho = "USER/VALOR/ID\n";
		String data = user + "/" + valorS + "/" + id;
		File fileQR = new File("QRrequests/data.txt");

		FileOutputStream fin = new FileOutputStream(fileQR, true);
		OutputStream ops = new BufferedOutputStream(fin);

		ops.write((data + "\n").getBytes());
		ops.flush();
		ops.close();
		// data that we want to store in the QR code

		// path where we want to get QR Code
		String path = "QRcodes/" + id + ".png";
		// Encoding charset to be used
		String charset = "UTF-8";
		Map<EncodeHintType, ErrorCorrectionLevel> hashMap = new HashMap<EncodeHintType, ErrorCorrectionLevel>();
		// generates QR code with Low level(L) error correction capability
		hashMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
		// invoking the user-defined method that creates the QR code
		generateQRcode(cabecalho + data, path, charset, hashMap, 200, 200);// increase or decrease height and width
																			// accodingly

		return "QRCode criado com sucesso";

	}

	public void generateQRcode(String data, String path, String charset, Map map, int w, int h)
			throws WriterException, IOException {
		// the BitMatrix class represents the 2D matrix of bits
		// MultiFormatWriter is a factory class that finds the appropriate Writer
		// subclass for the BarcodeFormat requested and encodes the barcode with the
		// supplied contents.
		BitMatrix matrix = new MultiFormatWriter().encode(new String(data.getBytes(charset), charset),
				BarcodeFormat.QR_CODE, w, h);
		MatrixToImageWriter.writeToFile(matrix, path.substring(path.lastIndexOf('.') + 1), new File(path));
	}

	/** APAGAR O QRCODE E FAZER VERIFICACOES
	 * 
	 */
	public int confirmQRcode(String user, String id) throws IOException {
		File file = new File("qrRequests/data.txt");

		FileInputStream fis = new FileInputStream(file);
		InputStream ips = new BufferedInputStream(fis);

		String data = "";
		int j;
		while ((j = ips.read()) != -1) {
			data += (char) j;
		}
		

		String destino = "";
		float valor = 0;

		ips.close();

		FileOutputStream fin = new FileOutputStream(file, false);
		OutputStream ops = new BufferedOutputStream(fin);

		String[] lines = data.split("\n");
		for (String l : lines) {
			String[] lineData = l.split("/");

			if (lineData[2].equals(id.trim())) {

				destino = lineData[0];

				valor = Float.parseFloat(lineData[1]);
				l = "".trim();
			}
			ops.write((l+"\n").getBytes());
		}
		ops.flush();
		ops.close();

		if (makepayment(user, destino, valor) != 1) {
			return 0;
		}

		return 1;

	}
}
