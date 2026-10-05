import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    private final Database db = new Database();
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private Label lblTotalContas;
    private Label lblPendentes;
    private ComboBox<Database.Conta> cbSaldoConta;
    private Label lblSaldo;
    private ComboBox<Database.Cliente> cbExtratoCliente;
    private TableView<Database.Movimento> tabelaExtrato;
    private TableView<Database.Emprestimo> tabelaEmprestimos;
    private ComboBox<Database.Cliente> cbEmpCliente;
    private ComboBox<Database.Conta> cbTransOrigem;
    private ComboBox<Database.Conta> cbTransDestino;

    @Override
    public void start(Stage stage) {
        TabPane tabs = new TabPane(
                aba("Painel", painel()),
                aba("Saldo", saldo()),
                aba("Extrato", extrato()),
                aba("Empréstimos", emprestimos()),
                aba("Transação", transacao()));

        Label titulo = new Label("ETEC Bank");
        titulo.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");
        HBox topo = new HBox(titulo);
        topo.setPadding(new Insets(14, 20, 14, 20));
        topo.setStyle("-fx-background-color: #14532d;");

        VBox raiz = new VBox(topo, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        stage.setTitle("ETEC Bank");
        stage.setScene(new Scene(raiz, 900, 600));
        stage.show();

        if (!db.conectado()) {
            erro("Sem conexão com o banco de dados. Verifique o arquivo .env e execute o programa na pasta que o contém.");
            return;
        }
        recarregar();
    }

    private Tab aba(String nome, javafx.scene.Node conteudo) {
        Tab t = new Tab(nome, conteudo);
        t.setClosable(false);
        return t;
    }

    private VBox painel() {
        lblTotalContas = new Label("-");
        lblPendentes = new Label("-");
        HBox cards = new HBox(20, card("Total de contas", lblTotalContas), card("Empréstimos pendentes", lblPendentes));
        Button atualizar = new Button("Atualizar");
        atualizar.setOnAction(e -> recarregar());
        VBox box = new VBox(20, cards, atualizar);
        box.setPadding(new Insets(20));
        return box;
    }

    private VBox card(String titulo, Label valor) {
        Label t = new Label(titulo);
        t.setStyle("-fx-text-fill: #555;");
        valor.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #14532d;");
        VBox c = new VBox(6, t, valor);
        c.setPadding(new Insets(20));
        c.setPrefWidth(240);
        c.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-radius: 8; -fx-background-radius: 8;");
        return c;
    }

    private VBox saldo() {
        cbSaldoConta = new ComboBox<>();
        cbSaldoConta.setPrefWidth(380);
        lblSaldo = new Label("");
        lblSaldo.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #14532d;");
        Button consultar = new Button("Consultar");
        consultar.setOnAction(e -> {
            Database.Conta c = cbSaldoConta.getValue();
            if (c == null) {
                erro("Selecione uma conta.");
                return;
            }
            try {
                lblSaldo.setText(moeda.format(db.consultarSaldo(c.id())));
            } catch (Exception ex) {
                erro(ex.getMessage());
            }
        });
        VBox box = new VBox(14, new Label("Conta"), new HBox(10, cbSaldoConta, consultar), lblSaldo);
        box.setPadding(new Insets(20));
        return box;
    }

    private VBox extrato() {
        cbExtratoCliente = new ComboBox<>();
        cbExtratoCliente.setPrefWidth(300);
        Button buscar = new Button("Buscar");
        tabelaExtrato = new TableView<>();
        tabelaExtrato.getColumns().add(coluna("Conta", 120, (Database.Movimento m) -> m.conta()));
        tabelaExtrato.getColumns().add(coluna("Tipo", 120, (Database.Movimento m) -> m.tipo()));
        tabelaExtrato.getColumns().add(coluna("Valor", 130, (Database.Movimento m) -> moeda.format(m.valor())));
        tabelaExtrato.getColumns().add(coluna("Data/Hora", 190, (Database.Movimento m) -> m.dataHora()));
        tabelaExtrato.getColumns().add(coluna("Destino", 120, (Database.Movimento m) -> m.destino() == null ? "" : m.destino()));
        buscar.setOnAction(e -> {
            Database.Cliente c = cbExtratoCliente.getValue();
            if (c == null) {
                erro("Selecione um cliente.");
                return;
            }
            try {
                tabelaExtrato.setItems(FXCollections.observableArrayList(db.listarExtrato(c.id())));
            } catch (Exception ex) {
                erro(ex.getMessage());
            }
        });
        VBox box = new VBox(12, new Label("Cliente"), new HBox(10, cbExtratoCliente, buscar), tabelaExtrato);
        VBox.setVgrow(tabelaExtrato, Priority.ALWAYS);
        box.setPadding(new Insets(20));
        return box;
    }

    private VBox emprestimos() {
        tabelaEmprestimos = new TableView<>();
        tabelaEmprestimos.getColumns().add(coluna("ID", 60, (Database.Emprestimo e) -> e.id()));
        tabelaEmprestimos.getColumns().add(coluna("Cliente", 200, (Database.Emprestimo e) -> e.cliente()));
        tabelaEmprestimos.getColumns().add(coluna("Valor", 120, (Database.Emprestimo e) -> moeda.format(e.valor())));
        tabelaEmprestimos.getColumns().add(coluna("Taxa (%)", 90, (Database.Emprestimo e) -> e.taxa()));
        tabelaEmprestimos.getColumns().add(coluna("Parcelas", 90, (Database.Emprestimo e) -> e.parcelas()));
        tabelaEmprestimos.getColumns().add(coluna("Status", 110, (Database.Emprestimo e) -> e.status()));

        ComboBox<String> filtro = new ComboBox<>(FXCollections.observableArrayList("todos", "pendente", "aprovado", "quitado"));
        filtro.setValue("pendente");
        filtro.setOnAction(e -> carregarEmprestimos(filtro.getValue()));
        tabelaEmprestimos.setUserData(filtro);

        cbEmpCliente = new ComboBox<>();
        cbEmpCliente.setPrefWidth(250);
        TextField valor = new TextField();
        TextField taxa = new TextField();
        TextField parcelas = new TextField();
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("pendente", "aprovado", "quitado"));
        status.setValue("pendente");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Cliente"), cbEmpCliente, new Label("Valor (R$)"), valor);
        form.addRow(1, new Label("Taxa de juros (0 a 20)"), taxa, new Label("Parcelas"), parcelas);
        form.addRow(2, new Label("Status"), status);

        Button registrar = new Button("Registrar empréstimo");
        registrar.setOnAction(e -> {
            Database.Cliente c = cbEmpCliente.getValue();
            if (c == null) {
                erro("Selecione um cliente.");
                return;
            }
            try {
                BigDecimal v = decimal(valor.getText());
                BigDecimal t = decimal(taxa.getText());
                int p = Integer.parseInt(parcelas.getText().trim());
                db.registrarEmprestimo(c.id(), v, t, p, status.getValue());
                valor.clear();
                taxa.clear();
                parcelas.clear();
                info("Empréstimo registrado com sucesso!");
                recarregar();
            } catch (NumberFormatException ex) {
                erro("Preencha valor, taxa e parcelas com números válidos.");
            } catch (Exception ex) {
                erro(ex.getMessage());
            }
        });

        VBox box = new VBox(12, new HBox(10, new Label("Filtrar por status"), filtro), tabelaEmprestimos,
                new Label("Novo empréstimo"), form, registrar);
        VBox.setVgrow(tabelaEmprestimos, Priority.ALWAYS);
        box.setPadding(new Insets(20));
        return box;
    }

    private VBox transacao() {
        cbTransOrigem = new ComboBox<>();
        cbTransDestino = new ComboBox<>();
        cbTransOrigem.setPrefWidth(380);
        cbTransDestino.setPrefWidth(380);
        ComboBox<String> tipo = new ComboBox<>(FXCollections.observableArrayList("deposito", "saque", "pix", "transferencia"));
        tipo.setValue("deposito");
        TextField valor = new TextField();
        valor.setPromptText("0,00");

        Label lblDestino = new Label("Conta de destino");
        tipo.setOnAction(e -> {
            boolean usaDestino = tipo.getValue().equals("pix") || tipo.getValue().equals("transferencia");
            cbTransDestino.setDisable(!usaDestino);
            lblDestino.setDisable(!usaDestino);
            if (!usaDestino) {
                cbTransDestino.setValue(null);
            }
        });
        cbTransDestino.setDisable(true);
        lblDestino.setDisable(true);

        Button executar = new Button("Realizar transação");
        executar.setDefaultButton(true);
        executar.setOnAction(e -> {
            Database.Conta origem = cbTransOrigem.getValue();
            Database.Conta destino = cbTransDestino.getValue();
            if (origem == null) {
                erro("Selecione a conta de origem.");
                return;
            }
            try {
                BigDecimal v = decimal(valor.getText());
                if (v.signum() <= 0) {
                    erro("O valor deve ser maior que zero.");
                    return;
                }
                db.executarTransacao(origem.id(), tipo.getValue(), v, destino == null ? null : destino.id());
                valor.clear();
                info("Transação realizada com sucesso!");
                recarregar();
            } catch (NumberFormatException ex) {
                erro("Informe um valor numérico válido.");
            } catch (Exception ex) {
                erro(ex.getMessage());
            }
        });

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.addRow(0, new Label("Tipo"), tipo);
        form.addRow(1, new Label("Conta de origem"), cbTransOrigem);
        form.addRow(2, lblDestino, cbTransDestino);
        form.addRow(3, new Label("Valor (R$)"), valor);

        VBox box = new VBox(18, form, executar);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(20));
        return box;
    }

    private <S> TableColumn<S, Object> coluna(String titulo, double largura, Function<S, Object> extrator) {
        TableColumn<S, Object> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(extrator.apply(d.getValue())));
        c.setPrefWidth(largura);
        return c;
    }

    private void recarregar() {
        try {
            List<Database.Conta> contas = db.listarContas();
            List<Database.Cliente> clientes = db.listarClientes();
            cbSaldoConta.setItems(FXCollections.observableArrayList(contas));
            cbTransOrigem.setItems(FXCollections.observableArrayList(contas));
            cbTransDestino.setItems(FXCollections.observableArrayList(contas));
            cbExtratoCliente.setItems(FXCollections.observableArrayList(clientes));
            cbEmpCliente.setItems(FXCollections.observableArrayList(clientes));
            lblTotalContas.setText(String.valueOf(db.contarContas()));
            lblPendentes.setText(String.valueOf(db.listarEmprestimos("pendente").size()));
            @SuppressWarnings("unchecked")
            ComboBox<String> filtro = (ComboBox<String>) tabelaEmprestimos.getUserData();
            carregarEmprestimos(filtro.getValue());
            lblSaldo.setText("");
        } catch (Exception ex) {
            erro(ex.getMessage());
        }
    }

    private void carregarEmprestimos(String filtro) {
        try {
            tabelaEmprestimos.setItems(FXCollections.observableArrayList(
                    db.listarEmprestimos(filtro.equals("todos") ? null : filtro)));
        } catch (Exception ex) {
            erro(ex.getMessage());
        }
    }

    private BigDecimal decimal(String texto) {
        return new BigDecimal(texto.trim().replace(",", "."));
    }

    private void erro(String mensagem) {
        Alert a = new Alert(Alert.AlertType.ERROR, mensagem);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void info(String mensagem) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, mensagem);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
