# 🏦 ETEC Bank

> **Sistema Bancário com Interface Gráfica (JavaFX) e Gestão de Transações Seguras**

[![Status do Projeto](https://img.shields.io/badge/Status-Concluído-brightgreen.svg)](#)
[![Linguagem](https://img.shields.io/badge/Linguagem-Java-orange.svg)](#)
[![Interface](https://img.shields.io/badge/UI-JavaFX-blueviolet.svg)](#)
[![Database](https://img.shields.io/badge/Database-PostgreSQL-blue.svg)](#)

---

## 📌 Sobre o Projeto

O **ETEC Bank** é um sistema bancário desenvolvido em Java com interface gráfica em **JavaFX**. A aplicação simula operações de uma agência e se conecta a um banco de dados PostgreSQL via JDBC.

O foco principal do projeto é a manipulação segura de dados, gerenciando clientes, contas, empréstimos e transferências com controle rigoroso de transações (*Commit* e *Rollback*).

A versão em terminal (`Main.java`) continua disponível junto com a nova interface.

---

## ✨ Principais Funcionalidades

- 📊 **Painel:** total de contas e quantidade de empréstimos pendentes.
- 💰 **Saldo:** consulta do saldo de qualquer conta, escolhida em uma lista.
- 🧾 **Extrato:** histórico de movimentações de um cliente em tabela, com conta de destino quando houver.
- 📝 **Empréstimos:** listagem com filtro por status (pendente, aprovado, quitado) e cadastro de novos empréstimos.
- 🔄 **Transações Seguras:** depósito, saque, pix e transferência com verificação de saldo. Usa `AutoCommit(false)` para garantir que o dinheiro só saia de uma conta se chegar na outra.
- 🔒 **Segurança de Credenciais:** a URL do banco é lida de um arquivo `.env`, que não é versionado.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Java
- **Interface:** JavaFX (`javafx.controls`)
- **Banco de Dados:** PostgreSQL (driver `postgresql-42.7.13.jar` incluso)
- **Classes Nativas:**
  - `java.sql.*` (Connection, PreparedStatement, ResultSet)
  - `java.math.BigDecimal` (valores monetários)
  - `java.util.Properties` (leitura do arquivo `.env`)

---

## 📁 Estrutura

```
JavaBank (ETEC)/
├── App.java        # Interface JavaFX
├── Database.java   # Acesso ao banco (JDBC)
├── Main.java       # Versão em terminal
├── run.sh          # Compila e executa a interface
├── javafx-lib/     # Jars do JavaFX (não versionado)
└── .env            # Credenciais do banco (não versionado)
```

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- [JDK](https://www.oracle.com/java/technologies/downloads/) 21 ou superior.
- Um banco PostgreSQL com as tabelas `agencia`, `cliente`, `conta`, `transacao` e `emprestimo` (veja o diagrama em `diagrama-db.png`).
- JavaFX: os jars `javafx-base`, `javafx-graphics` e `javafx-controls` (versão Linux) dentro de `JavaBank (ETEC)/javafx-lib/`, baixados em [Maven Central](https://repo1.maven.org/maven2/org/openjfx/). Como alternativa, defina `JAVAFX_HOME` com o caminho da pasta `lib` de um SDK do JavaFX.

### Passo a Passo

1. **Clone este repositório:**
   ```bash
   git clone https://github.com/seu-usuario/etec-bank.git
   cd etec-bank/"JavaBank (ETEC)"
   ```

2. **Crie o arquivo `.env`** na pasta `JavaBank (ETEC)`:
   ```
   DATABASE_URL=jdbc:postgresql://HOST/BANCO?user=USUARIO&password=SENHA&sslmode=require
   ```

3. **Execute a interface gráfica:**
   ```bash
   ./run.sh
   ```

### Versão em terminal

```bash
javac -cp postgresql-42.7.13.jar -d out Database.java Main.java
java -cp "out:postgresql-42.7.13.jar" Main
```

> ⚠️ Nunca envie o `.env` para o repositório. Ele já está no `.gitignore`.
