import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Database {
    private final Properties prop = new Properties();
    private Connection conexao;
    private String url;

    Database() {
        try {
            InputStream input = Files.newInputStream(Paths.get(".env"));
            prop.load(input);
            url = prop.getProperty("DATABASE_URL");
            conexao = DriverManager.getConnection(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void extrato(int cliente_id) {
        String sql = "SELECT c.numero, cl.nome AS cliente, t.tipo, t.valor, t.data_hora " +
                     "FROM transacao t JOIN conta c ON c.id = t.conta_id " +
                     "JOIN cliente cl ON cl.id = c.cliente_id " +
                     "WHERE cl.id = ? ORDER BY t.data_hora DESC";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, cliente_id);
            ResultSet resultado = ps.executeQuery();
            System.out.println("\n--- Extrato ---");
            while (resultado.next()) {
                System.out.println("Conta: " + resultado.getString("numero") +
                                   " | Tipo: " + resultado.getString("tipo") +
                                   " | Valor: R$ " + resultado.getLong("valor") +
                                   " | Data: " + resultado.getTimestamp("data_hora"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saldo(int conta_id) {
        String sql = "SELECT numero, tipo, saldo FROM conta WHERE id = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, conta_id);
            ResultSet resultado = ps.executeQuery();
            System.out.println("\n--- Saldo ---");
            while (resultado.next()) {
                System.out.println("Conta: " + resultado.getString("numero") +
                                   " | Tipo: " + resultado.getString("tipo") +
                                   " | Saldo: R$ " + resultado.getLong("saldo"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void total_contas() {
        String sql = "SELECT COUNT(*) AS total FROM conta";
        try (Statement consulta = conexao.createStatement();
             ResultSet resultado = consulta.executeQuery(sql)) {
            if (resultado.next()) {
                System.out.println("\nTotal de contas no banco: " + resultado.getInt("total"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void emprestimo_pendente() {
        String sql = "SELECT e.id, c.nome, e.valor, e.taxa_juros, e.parcelas " +
                     "FROM emprestimo e JOIN cliente c ON c.id = e.cliente_id WHERE e.status = 'pendente'";
        try (Statement consulta = conexao.createStatement();
             ResultSet resultado = consulta.executeQuery(sql)) {
            System.out.println("\n--- Empréstimos Pendentes ---");
            while (resultado.next()) {
                System.out.println("ID: " + resultado.getInt("id") +
                                   " | Cliente: " + resultado.getString("nome") +
                                   " | Valor: R$ " + resultado.getLong("valor") +
                                   " | Taxa: " + resultado.getLong("taxa_juros") + "%" +
                                   " | Parcelas: " + resultado.getInt("parcelas"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void emprestimo(int cliente_id, Long valor, Long taxa_juros, int parcelas, String status) {
   
        String sql = "INSERT INTO emprestimo (cliente_id, agencia_id, valor, taxa_juros, parcelas, status) VALUES (?, 1, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, cliente_id);
            ps.setLong(2, valor);
            ps.setLong(3, taxa_juros);
            ps.setInt(4, parcelas);
            ps.setString(5, status);
            ps.executeUpdate();
            System.out.println("\nEmpréstimo registrado com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void transacao(int conta_id, String tipo, Long valor, String data_hora, int conta_destino_id) {
        try {
           
            conexao.setAutoCommit(false);

           
            String sqlDebito = "UPDATE conta SET saldo = saldo - ? WHERE id = ? AND saldo >= ?";
            try (PreparedStatement psDebito = conexao.prepareStatement(sqlDebito)) {
                psDebito.setLong(1, valor);
                psDebito.setInt(2, conta_id);
                psDebito.setLong(3, valor);
                if (psDebito.executeUpdate() == 0) {
                    throw new Exception(" Saldo insuficiente ou conta origem não encontrada.");
                }
            }

          
            if (conta_destino_id > 0) {
                String sqlCredito = "UPDATE conta SET saldo = saldo + ? WHERE id = ?";
                try (PreparedStatement psCredito = conexao.prepareStatement(sqlCredito)) {
                    psCredito.setLong(1, valor);
                    psCredito.setInt(2, conta_destino_id);
                    if (psCredito.executeUpdate() == 0) {
                        throw new Exception("Conta destino não encontrada.");
                    }
                }
            }

         
            String sqlTransacao = "INSERT INTO transacao (conta_id, tipo, valor, conta_destino_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psRegistroTransacao = conexao.prepareStatement(sqlTransacao)) {
                psRegistroTransacao.setInt(1, conta_id);
                psRegistroTransacao.setString(2, tipo);
                psRegistroTransacao.setLong(3, valor);
                if (conta_destino_id > 0) {
                    psRegistroTransacao.setInt(4, conta_destino_id);
                } else {
                    psRegistroTransacao.setNull(4, java.sql.Types.INTEGER);
                }
                psRegistroTransacao.executeUpdate();
            }

           
            conexao.commit();
            System.out.println("\nTransação realizada com sucesso!");

        } catch (Exception e) {
            System.out.println("\nErro durante a transação. Efetuando Rollback...");
            try {
                conexao.rollback(); 
            } catch (SQLException ex) { 
                ex.printStackTrace(); 
            }
            e.printStackTrace();
        } finally {
            try { 
             
                conexao.setAutoCommit(true); 
            } catch (SQLException e) { 
                e.printStackTrace(); 
            }
        }
    }
}