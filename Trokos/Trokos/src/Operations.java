import java.io.IOException;

import com.google.zxing.WriterException;

public interface Operations {

	int makepayment(String user, String destino, float valor) throws IOException;

	float balance(String user) throws IOException;

	//newgroup
	int criaGrupo(String user, String nomeGrupo) throws IOException;
	//addu
	int adicionaAoGrupo(String owner, String user, String nomeGrupo) throws IOException;
	
	String groups (String user) throws IOException;

	int requestpayment(String user, String destino, float valor) throws IOException;

	String viewrequests(String user) throws IOException;

	int payrequest(String id, String user) throws IOException;

	String obtainQRcode(String user, float valor) throws WriterException, IOException;

	int dividepayment(String user, String nomeGrupo, float valor) throws IOException;

	String statuspayment(String user, String nomeGrupo) throws IOException;

	int confirmQRcode(String user, String id) throws IOException;
	
	String history(String user) throws IOException;
}
