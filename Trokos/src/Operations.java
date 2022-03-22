import java.io.IOException;

public interface Operations {
	
	int makepayment(String user, String destino, float valor) throws IOException;
	
	float balance(String user) throws IOException;
	
	int criaGrupo(String user, String nomeGrupo) throws IOException;
	
	int adicionaAoGrupo(String owner, String user, String nomeGrupo) throws IOException;
}
