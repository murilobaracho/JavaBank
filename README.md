# 🏦 ETEC Bank

> **Sistema Bancário via Terminal com Gestão de Transações Seguras**

[![Status do Projeto](https://img.shields.io/badge/Status-Concluído-brightgreen.svg)](#)
[![Linguagem](https://img.shields.io/badge/Linguagem-Java-orange.svg)](#)
[![Database](https://img.shields.io/badge/Database-SQL-blue.svg)](#)

---

## 📌 Sobre o Projeto

O **ETEC Bank** é um sistema bancário executado via terminal (CLI) desenvolvido em Java. A aplicação simula operações reais de uma agência através de um menu interativo, conectando-se diretamente a um banco de dados relacional. 

O foco principal do projeto é a manipulação segura de dados, gerenciando clientes, contas, empréstimos e transferências utilizando a API JDBC com controle rigoroso de transações (*Commit* e *Rollback*).

---

## ✨ Principais Funcionalidades

- 💰 **Consulta de Saldo e Extrato:** Exibe o histórico completo de movimentações e o saldo atualizado em tempo real.
- 🔄 **Transações Seguras:** Sistema de transferências, depósitos e saques com verificação de saldo. Utiliza `AutoCommit(false)` para garantir que o dinheiro só saia de uma conta se chegar na outra com sucesso.
- 📝 **Gestão de Empréstimos:** Cadastro de novas solicitações e listagem de empréstimos com status pendente.
- 📊 **Métricas Rápidas:** Contabilização instantânea do total de contas ativas no banco de dados.
- 🔒 **Segurança de Credenciais:** Leitura de variáveis de ambiente via arquivo `.env` para proteger a URL e as credenciais do banco de dados.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** [Java](https://www.oracle.com/java/)
- **Banco de Dados:** MySQL / PostgreSQL *(Conexão nativa via JDBC)*
- **Bibliotecas/Classes Nativas:**
  - `java.sql.*` (Connection, PreparedStatement, ResultSet)
  - `java.util.Properties` (Leitura do arquivo `.env`)
  - `java.time.LocalDateTime` (Registro de data e hora das transações)
  - `java.util.Scanner` (Interface interativa no terminal)

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- [JDK (Java Development Kit)](https://www.oracle.com/java/technologies/downloads/) instalado.
- Servidor de banco de dados rodando (MySQL, PostgreSQL, etc).
- Driver JDBC correspondente ao seu banco de dados.

### Passo a Passo

1. **Clone este repositório:**
   ```bash
   git clone [https://github.com/seu-usuario/etec-bank.git](https://github.com/seu-usuario/etec-bank.git)
   cd etec-bank
