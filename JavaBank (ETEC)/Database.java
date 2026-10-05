import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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

    public record Cliente(int id, String nome) {
        @Override public String toString() { return id + " - " + nome; }
    }

    public record Conta(int id, String numero, String tipo, String titular, BigDecimal saldo) {
        @Override public String toString() { return numero + " (" + tipo + ") - " + titular; }
    }

    public record Movimento(String conta, String tipo, BigDecimal valor, String dataHora, String destino) {}

    public record Emprestimo(int id, String cliente, BigDecimal valor, BigDecimal taxa, int parcelas, String status) {}

    public boolean conectado() {
        return conexao != null;
    }

    private void exigirConexao() throws SQLException {
        if (conexao == null) {
            throw new SQLException("Sem conexão com o banco de dados. Verifique o arquivo .env.");
        }
    }

    public List<Cliente> listarClientes() throws SQLException {
        exigirConexao();
        List<Cliente> lista = new ArrayList<>();
        try (Statement st = conexao.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nome FROM cliente ORDER BY nome")) {
            while (rs.next()) {
                lista.add(new Cliente(rs.getInt("id"), rs.getString("nome")));
            }
        }
        return lista;
    }

    public List<Conta> listarContas() throws SQLException {
        exigirConexao();
        List<Conta> lista = new ArrayList<>();
        String sql = "SELECT c.id, c.numero, c.tipo, c.saldo, cl.nome FROM conta c " +
                     "JOIN cliente cl ON cl.id = c.cliente_id ORDER BY c.numero";
        try (Statement st = conexao.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Conta(rs.getInt("id"), rs.getString("numero"), rs.getString("tipo"),
                                    rs.getString("nome"), rs.getBigDecimal("saldo")));
            }
        }
        return lista;
    }

    public BigDecimal consultarSaldo(int contaId) throws SQLException {
        exigirConexao();
        try (PreparedStatement ps = conexao.prepareStatement("SELECT saldo FROM conta WHERE id = ?")) {
            ps.setInt(1, contaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("saldo");
            }
            throw new SQLException("Conta não encontrada.");
        }
    }

    public int contarContas() throws SQLException {
        exigirConexao();
        try (Statement st = conexao.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM conta")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<Movimento> listarExtrato(int clienteId) throws SQLException {
        exigirConexao();
        List<Movimento> lista = new ArrayList<>();
        String sql = "SELECT c.numero, t.tipo, t.valor, t.data_hora, cd.numero AS destino " +
                     "FROM transacao t JOIN conta c ON c.id = t.conta_id " +
                     "LEFT JOIN conta cd ON cd.id = t.conta_destino_id " +
                     "WHERE c.cliente_id = ? ORDER BY t.data_hora DESC";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new Movimento(rs.getString("numero"), rs.getString("tipo"), rs.getBigDecimal("valor"),
                                        String.valueOf(rs.getTimestamp("data_hora")), rs.getString("destino")));
            }
        }
        return lista;
    }

    public List<Emprestimo> listarEmprestimos(String status) throws SQLException {
        exigirConexao();
        List<Emprestimo> lista = new ArrayList<>();
        String sql = "SELECT e.id, c.nome, e.valor, e.taxa_juros, e.parcelas, e.status " +
                     "FROM emprestimo e JOIN cliente c ON c.id = e.cliente_id " +
                     (status == null ? "" : "WHERE e.status = ? ") + "ORDER BY e.id";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            if (status != null) {
                ps.setString(1, status);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new Emprestimo(rs.getInt("id"), rs.getString("nome"), rs.getBigDecimal("valor"),
                                         rs.getBigDecimal("taxa_juros"), rs.getInt("parcelas"), rs.getString("status")));
            }
        }
        return lista;
    }

    public void registrarEmprestimo(int clienteId, BigDecimal valor, BigDecimal taxa, int parcelas, String status) throws SQLException {
        exigirConexao();
        String sql = "INSERT INTO emprestimo (cliente_id, agencia_id, valor, taxa_juros, parcelas, status) " +
                     "VALUES (?, (SELECT agencia_id FROM conta WHERE cliente_id = ? LIMIT 1), ?, ?, ?, ?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            ps.setInt(2, clienteId);
            ps.setBigDecimal(3, valor);
            ps.setBigDecimal(4, taxa);
            ps.setInt(5, parcelas);
            ps.setString(6, status);
            ps.executeUpdate();
        }
    }

    public void executarTransacao(int contaId, String tipo, BigDecimal valor, Integer destinoId) throws SQLException {
        exigirConexao();
        boolean precisaDestino = tipo.equals("pix") || tipo.equals("transferencia");
        if (precisaDestino && (destinoId == null || destinoId == contaId)) {
            throw new SQLException("Selecione uma conta de destino diferente da origem.");
        }
        try {
            conexao.setAutoCommit(false);
            if (!tipo.equals("deposito")) {
                try (PreparedStatement ps = conexao.prepareStatement(
                        "UPDATE conta SET saldo = saldo - ? WHERE id = ? AND saldo >= ?")) {
                    ps.setBigDecimal(1, valor);
                    ps.setInt(2, contaId);
                    ps.setBigDecimal(3, valor);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("Saldo insuficiente ou conta de origem não encontrada.");
                    }
                }
            }
            Integer creditada = tipo.equals("deposito") ? Integer.valueOf(contaId) : (precisaDestino ? destinoId : null);
            if (creditada != null) {
                try (PreparedStatement ps = conexao.prepareStatement("UPDATE conta SET saldo = saldo + ? WHERE id = ?")) {
                    ps.setBigDecimal(1, valor);
                    ps.setInt(2, creditada);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("Conta de destino não encontrada.");
                    }
                }
            }
            try (PreparedStatement ps = conexao.prepareStatement(
                    "INSERT INTO transacao (conta_id, tipo, valor, conta_destino_id) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, contaId);
                ps.setString(2, tipo);
                ps.setBigDecimal(3, valor);
                if (precisaDestino) {
                    ps.setInt(4, destinoId);
                } else {
                    ps.setNull(4, java.sql.Types.INTEGER);
                }
                ps.executeUpdate();
            }
            conexao.commit();
        } catch (SQLException e) {
            try {
                conexao.rollback();
            } catch (SQLException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        } finally {
            conexao.setAutoCommit(true);
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