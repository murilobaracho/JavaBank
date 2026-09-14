import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    Database db = new Database();
    int opcao;
    boolean laco = true;

    do {
      System.out.println("\n=== ETEC Bank ===");
      System.out.println("1. Extrato");
      System.out.println("2. Saldo");
      System.out.println("3. Total Contas");
      System.out.println("4. Empréstimos Pendentes");
      System.out.println("5. Registrar Empréstimo");
      System.out.println("6. Transação");
      System.out.println("7. Sair");
      System.out.print("Escolha uma opção: ");

      opcao = scanner.nextInt();
      scanner.nextLine();

      switch (opcao) {
        case 1: {
          System.out.print("ID do Cliente: ");
          int cliente_id = scanner.nextInt();
          db.extrato(cliente_id);
          break;
        }
        case 2: {
          System.out.print("ID da Conta: ");
          int cliente_id = scanner.nextInt();
          db.saldo(cliente_id);
          break;
        }
        case 3: {
          db.total_contas();
          break;
        }
        case 4: {
          db.emprestimo_pendente();
          break;
        }
        case 5: {
          System.out.print("ID Cliente: ");
          int cliente_id = scanner.nextInt();
          System.out.print("Valor: ");
          Long valor = scanner.nextLong();
          System.out.print("Taxa de Juros (0 a 20): ");
          Long taxa_juros = scanner.nextLong();
          System.out.print("Parcelas: ");
          int parcelas = scanner.nextInt();
          scanner.nextLine(); 
          System.out.print("Status (pendente/aprovado/quitado): ");
          String status = scanner.nextLine();

          db.emprestimo(cliente_id, valor, taxa_juros, parcelas, status);
          break;
        }
        case 6: {
          System.out.print("ID Conta Origem: ");
          int conta_id = scanner.nextInt();
          scanner.nextLine(); 
          
          System.out.print("Tipo (deposito/saque/pix/transferencia): ");
          String tipo = scanner.nextLine();
          
          System.out.print("Valor: ");
          Long valor = scanner.nextLong();
          scanner.nextLine(); 


          LocalDateTime data = LocalDateTime.now();
          DateTimeFormatter formatar = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
          String data_hora = data.format(formatar);
          System.out.print("Data/Hora: " + data_hora + "\n");

          
          System.out.print("ID Conta Destino: ");
          int conta_destino_id = scanner.nextInt();

          db.transacao(conta_id, tipo, valor, data_hora, conta_destino_id);
          break;
        }
        case 7: {
          laco = false;
          System.out.println("Saindo do sistema ETEC Bank...");
          break;
        }
        default:
          System.out.println("Opção inválida!");
      }
    } while (laco);

    scanner.close();
  }
}