import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

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
	 * @return
	 * @throws IOException
	 */
	public int adicionaAoGrupo(String owner, String user, String nomeGrupo) throws IOException {

		if (!new File("groups/" + nomeGrupo + ".txt").exists()) {
			System.out.println("teste");
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
						return 1;
					}
				}
				ops.close();
			}

		}

		ips.close();
		return 1;
	}
}
